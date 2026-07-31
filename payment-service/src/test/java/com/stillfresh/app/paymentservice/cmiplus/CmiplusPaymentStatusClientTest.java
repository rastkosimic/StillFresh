package com.stillfresh.app.paymentservice.cmiplus;

import com.stillfresh.app.paymentservice.service.rail.PayoutStatusUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CmiplusPaymentStatusClientTest {

    private CmiplusPaymentStatusClient client;

    @BeforeEach
    void setUp() {
        CmiplusProperties props = new CmiplusProperties();
        props.setStubMode(true);
        client = new CmiplusPaymentStatusClient(props, null);
    }

    @Test
    void parsePain002_acceptsSettledStatus() {
        String pain002 = """
            <Document>
              <CstmrPmtStsRpt>
                <OrgnlPmtInfAndSts>
                  <TxInfAndSts><TxSts>ACSC</TxSts></TxInfAndSts>
                </OrgnlPmtInfAndSts>
              </CstmrPmtStsRpt>
            </Document>
            """;
        PayoutStatusUpdate update = client.parsePain002(pain002);
        assertEquals(PayoutStatusUpdate.Outcome.COMPLETED, update.getOutcome());
    }

    @Test
    void parsePain002_rejectsTransfer() {
        String pain002 = """
            <TxSts>RJCT</TxSts><AddtlInf>Insufficient funds</AddtlInf>
            """;
        PayoutStatusUpdate update = client.parsePain002(pain002);
        assertEquals(PayoutStatusUpdate.Outcome.FAILED, update.getOutcome());
        assertEquals("Insufficient funds", update.getErrorMessage());
    }

    @Test
    void parsePain002_pendingStatus() {
        String pain002 = "<TxSts>PDNG</TxSts>";
        PayoutStatusUpdate update = client.parsePain002(pain002);
        assertEquals(PayoutStatusUpdate.Outcome.PENDING, update.getOutcome());
    }
}
