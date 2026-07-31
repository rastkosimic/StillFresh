package com.stillfresh.app.paymentservice.service.rail.pain001;

import com.stillfresh.app.paymentservice.service.executor.PayoutTransferRequest;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Shared assembly of a single-transaction pain.001.001.03 document.
 * Subclasses supply the payment type information block and the creditor
 * account block, which is where SEPA and domestic formats diverge.
 */
public abstract class AbstractPain001Builder implements Pain001MessageBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Value("${bank-transfer.platform.iban}")
    protected String debtorIban;

    @Value("${bank-transfer.platform.account-holder}")
    protected String debtorName;

    /** Returns the {@code <PmtTpInf>} block, or an empty string to omit it. */
    protected abstract String paymentTypeInfo();

    /** Returns the {@code <CdtrAcct>} block for the creditor's account. */
    protected abstract String creditorAccount(PayoutTransferRequest req);

    /** Returns the {@code <CdtrAgt>} block for the creditor's bank. */
    protected String creditorAgent(PayoutTransferRequest req) {
        String bankCode = req.getTargetBankCode();
        if (bankCode != null && !bankCode.isBlank()) {
            return "        <CdtrAgt><FinInstnId><BIC>" + escapeXml(bankCode) + "</BIC></FinInstnId></CdtrAgt>\n";
        }
        return "        <CdtrAgt><FinInstnId><Othr><Id>NOTPROVIDED</Id></Othr></FinInstnId></CdtrAgt>\n";
    }

    @Override
    public String build(PayoutTransferRequest req) {
        BigDecimal amount = BigDecimal.valueOf(req.getAmountCents())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        String msgId  = "SF-" + req.getIdempotencyKey().substring(0, 8).toUpperCase();
        String now    = LocalDateTime.now().format(DT_FMT);
        String today  = LocalDate.now().format(DATE_FMT);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.001.001.03\">\n" +
            "  <CstmrCdtTrfInitn>\n" +
            "    <GrpHdr>\n" +
            "      <MsgId>" + msgId + "</MsgId>\n" +
            "      <CreDtTm>" + now + "</CreDtTm>\n" +
            "      <NbOfTxs>1</NbOfTxs>\n" +
            "      <CtrlSum>" + amount.toPlainString() + "</CtrlSum>\n" +
            "      <InitgPty><Nm>" + escapeXml(debtorName) + "</Nm></InitgPty>\n" +
            "    </GrpHdr>\n" +
            "    <PmtInf>\n" +
            "      <PmtInfId>" + req.getIdempotencyKey() + "</PmtInfId>\n" +
            "      <PmtMtd>TRF</PmtMtd>\n" +
            "      <NbOfTxs>1</NbOfTxs>\n" +
            "      <CtrlSum>" + amount.toPlainString() + "</CtrlSum>\n" +
            paymentTypeInfo() +
            "      <ReqdExctnDt>" + today + "</ReqdExctnDt>\n" +
            "      <Dbtr><Nm>" + escapeXml(debtorName) + "</Nm></Dbtr>\n" +
            "      <DbtrAcct><Id><IBAN>" + escapeXml(debtorIban) + "</IBAN></Id></DbtrAcct>\n" +
            "      <DbtrAgt><FinInstnId><Othr><Id>NOTPROVIDED</Id></Othr></FinInstnId></DbtrAgt>\n" +
            "      <CdtTrfTxInf>\n" +
            "        <PmtId>\n" +
            "          <EndToEndId>" + req.getIdempotencyKey() + "</EndToEndId>\n" +
            "        </PmtId>\n" +
            "        <Amt><InstdAmt Ccy=\"" + req.getCurrency() + "\">" + amount.toPlainString() + "</InstdAmt></Amt>\n" +
            creditorAgent(req) +
            "        <Cdtr><Nm>" + escapeXml(req.getTargetAccountHolder()) + "</Nm></Cdtr>\n" +
            creditorAccount(req) +
            "        <RmtInf><Ustrd>" + escapeXml(req.getDescription()) + "</Ustrd></RmtInf>\n" +
            "      </CdtTrfTxInf>\n" +
            "    </PmtInf>\n" +
            "  </CstmrCdtTrfInitn>\n" +
            "</Document>\n";
    }

    protected static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
