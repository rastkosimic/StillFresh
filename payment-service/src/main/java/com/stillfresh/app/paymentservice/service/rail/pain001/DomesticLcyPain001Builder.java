package com.stillfresh.app.paymentservice.service.rail.pain001;

import com.stillfresh.app.paymentservice.service.executor.PayoutTransferRequest;
import org.springframework.stereotype.Component;

/**
 * Domestic local-currency pain.001 for CMIplus (Serbian RSD transfers).
 * No SEPA service level; the creditor account may be identified by IBAN or,
 * when unavailable, by a domestic account number ({@code Othr/Id}).
 */
@Component
public class DomesticLcyPain001Builder extends AbstractPain001Builder {

    @Override
    protected String paymentTypeInfo() {
        // Domestic LCY transfers carry no SEPA service level. Instruction
        // priority NORM is broadly accepted; adjust per RBI sandbox spec if needed.
        return "      <PmtTpInf><InstrPrty>NORM</InstrPrty></PmtTpInf>\n";
    }

    @Override
    protected String creditorAccount(PayoutTransferRequest req) {
        if (req.getTargetIban() != null && !req.getTargetIban().isBlank()) {
            return "        <CdtrAcct><Id><IBAN>" + escapeXml(req.getTargetIban()) + "</IBAN></Id></CdtrAcct>\n";
        }
        return "        <CdtrAcct><Id><Othr><Id>" + escapeXml(req.getTargetAccountNumber())
                + "</Id></Othr></Id></CdtrAcct>\n";
    }
}
