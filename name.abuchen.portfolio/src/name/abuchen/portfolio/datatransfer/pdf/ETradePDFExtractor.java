package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class ETradePDFExtractor extends AbstractPDFExtractor
{
    public ETradePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("E*TRADE Securities LLC");

        addDeliveryInOutBoundTransaction();
    }

    @Override
    public String getLabel()
    {
        return "E*TRADE Securities LLC";
    }

    private void addDeliveryInOutBoundTransaction()
    {
        var type = new DocumentType("Purchase Summary");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<PortfolioTransaction>();

        var firstRelevantLine = new Block("^EMPLOYEE STOCK PLAN (EXERCISE|PURCHASE) CONFIRMATION$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            return portfolioTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Company Name (Symbol) NETAPP,  INC.(NTAP) Beginning Balance 60.0000
                                        // Grant Date Market Value $71.960000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "tickerSymbol", "currency") //
                                                        .match("^Company Name \\(Symbol\\) (?<name>.*)\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\) Beginning Balance .*$") //
                                                        .match("^Grant Date Market Value (?<currency>\\p{Sc})[\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Company Name (Symbol) NXP SEMICONDUCTORS, Beginning Balance 0.0000
                                        // N.V.(NXPI) Shares Purchased 5.2350
                                        // Grant Date Market Value $215.590000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "nameContinued", "tickerSymbol", "currency") //
                                                        .match("^Company Name \\(Symbol\\) (?<name>.*) Beginning Balance .*$") //
                                                        .match("^(?<nameContinued>.*)\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\) Shares Purchased [\\.,\\d]+$") //
                                                        .match("^Grant Date Market Value (?<currency>\\p{Sc})[\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Company Name (Symbol) NETAPP,  INC.(NTAP) Broker Assist Fee ($0.00)
                                        // Exercise Market Value $83.28
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "tickerSymbol", "currency") //
                                                        .match("^Company Name \\(Symbol\\) (?<name>.*)\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\) Broker Assist Fee .*$") //
                                                        .match("^Exercise Market Value (?<currency>\\p{Sc})[\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Company Name (Symbol) NXP SEMICONDUCTORS, Broker Assist Fee ($0.00)
                                        // N.V.(NXPI) Disbursement Fee ($0.00)
                                        // Exercise Market Value $123.45
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "nameContinued", "tickerSymbol", "currency") //
                                                        .match("^Company Name \\(Symbol\\) (?<name>.*)\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\) Broker Assist Fee .*$") //
                                                        .match("^(?<nameContinued>.*)\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\) Disbursement Fee.*$") //
                                                        .match("^Exercise Market Value (?<currency>\\p{Sc})[\\.,\\d]+$") //
                                                        .match("^Company Name \\(Symbol\\) (?<name>.*) Beginning Balance.*$") //
                                                        .match("^(?<nameContinued>.*)\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\) Shares Purchased [\\.,\\d]+$") //
                                                        .match("^Grant Date Market Value (?<currency>\\p{Sc})[\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // N.V.(NXPI) Shares Purchased 5.2350
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* Shares Purchased (?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Shares Issued 16
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Shares Issued (?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Purchase Date 02-28-2025
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Purchase Date (?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4})( .*)?$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Exercise Date: 04/14/2025 Exercise Type: Cash Exercise Registration:
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Exercise Date: (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4})( .*)?$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        // @formatter:off
                        // Total Price ($959.31)
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Total Price \\((?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)\\)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Taxable Gain $169.30
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Taxable Gain .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Taxes Withheld $356.66 (Tax Rate / Taxable Gain)
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Taxes Withheld \\((?<currency>\\p{Sc})(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Comission/Fee $0.00
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Comission/Fee \\((?<currency>\\p{Sc})(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "US");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "en", "US");
    }
}
