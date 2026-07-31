package com.stillfresh.app.paymentservice.service.rail.pain001;

import com.stillfresh.app.paymentservice.service.executor.PayoutTransferRequest;
import org.springframework.stereotype.Component;

/**
 * SEPA credit transfer pain.001 (service level SEPA, IBAN required).
 * Used by the sepa-xml file export rail.
 */
@Component
public class SepaPain001Builder extends AbstractPain001Builder {

    @Override
    protected String paymentTypeInfo() {
        return "      <PmtTpInf><SvcLvl><Cd>SEPA</Cd></SvcLvl></PmtTpInf>\n";
    }

    @Override
    protected String creditorAccount(PayoutTransferRequest req) {
        return "        <CdtrAcct><Id><IBAN>" + escapeXml(req.getTargetIban()) + "</IBAN></Id></CdtrAcct>\n";
    }
}
