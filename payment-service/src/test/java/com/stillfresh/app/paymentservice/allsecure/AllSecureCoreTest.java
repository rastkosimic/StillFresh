package com.stillfresh.app.paymentservice.allsecure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Offline known-answer tests for the hand-rolled AllSecure crypto and XML core. These do not hit the
 * network; they validate the parts that must be byte-exact for the gateway to accept our requests and
 * for us to accept its callbacks.
 */
class AllSecureCoreTest {

    private final AllSecureSignatureService signing = new AllSecureSignatureService();

    @Test
    void sha1PasswordMatchesKnownAnswer() {
        // Well-known SHA-1 of "password"; the API docs use exactly this hash in their examples.
        assertEquals("5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8", signing.sha1Password("password"));
    }

    @Test
    void signatureIsDeterministicAndVerifiable() {
        String body = "<transaction><debit/></transaction>";
        String contentType = "text/xml; charset=utf-8";
        String date = "Mon, 27 Nov 2018 10:33:41 UTC";
        String uri = "/transaction";
        String secret = "THE_SHARED_SECRET";

        String sig1 = signing.sign("POST", body, contentType, date, uri, secret);
        String sig2 = signing.sign("POST", body, contentType, date, uri, secret);
        assertEquals(sig1, sig2, "Signing must be deterministic for identical inputs");

        assertTrue(signing.verify("POST", body, contentType, date, uri, secret, sig1),
                "A freshly-generated signature must verify");
        assertFalse(signing.verify("POST", body, contentType, date, uri, secret, sig1 + "x"),
                "A tampered signature must be rejected");
        assertFalse(signing.verify("POST", body + " ", contentType, date, uri, secret, sig1),
                "A modified body must invalidate the signature");
        assertFalse(signing.verify("POST", body, contentType, date, uri, secret, null),
                "A missing signature must be rejected");
    }

    private static final String SANDBOX_NS = "http://asxgw.paymentsandbox.cloud/Schema/V2/Transaction";

    @Test
    void buildRegisterProducesNamespacedEnvelope() {
        String xml = AllSecureXml.buildRegister(SANDBOX_NS, "api-user", "hashedpw", "reg-1", "john",
                "desc", "https://x/s", "https://x/e", "https://x/c", "https://x/cb");
        assertTrue(xml.contains("xmlns=\"" + SANDBOX_NS + "\""));
        assertTrue(xml.contains("<register>"));
        assertTrue(xml.contains("<username>api-user</username>"));
        assertTrue(xml.contains("<password>hashedpw</password>"));
        assertTrue(xml.contains("<transactionId>reg-1</transactionId>"));
        assertTrue(xml.contains("<identification>john</identification>"));
        assertTrue(xml.contains("<callbackUrl>https://x/cb</callbackUrl>"));
    }

    @Test
    void buildPreauthorizeIncludesCardOnFileIndicator() {
        String xml = AllSecureXml.buildPreauthorize(SANDBOX_NS, "api-user", "hashedpw", "req-1", "john",
                "12.34", "RSD", "Order payment", "ref-uuid", "CARDONFILE", "https://x/cb");
        assertTrue(xml.contains("<preauthorize>"));
        assertTrue(xml.contains("<amount>12.34</amount>"));
        assertTrue(xml.contains("<currency>RSD</currency>"));
        assertTrue(xml.contains("<referenceTransactionId>ref-uuid</referenceTransactionId>"));
        assertTrue(xml.contains("<transactionIndicator>CARDONFILE</transactionIndicator>"));
    }

    @Test
    void namespaceRootDerivesFromSandboxHost() {
        AllSecureProperties props = new AllSecureProperties();
        props.setBaseUrl("https://asxgw.paymentsandbox.cloud");
        assertEquals("http://asxgw.paymentsandbox.cloud", props.namespaceRoot());
        assertEquals(SANDBOX_NS, props.transactionNamespace());
    }

    @Test
    void parseResultReadsFinishedReference() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<result xmlns=\"https://asxgw.com/Schema/V2/Result\">"
                + "<success>true</success>"
                + "<referenceId>12345678901234567890</referenceId>"
                + "<purchaseId>20170101-12345678901234567890</purchaseId>"
                + "<returnType>FINISHED</returnType>"
                + "</result>";
        AllSecureResult result = AllSecureXml.parseResult(xml);
        assertTrue(result.isSuccess());
        assertTrue(result.isFinished());
        assertEquals("12345678901234567890", result.getReferenceId());
        assertNull(result.getErrorMessage());
    }

    @Test
    void parseResultReadsRedirectUrl() throws Exception {
        String xml = "<result xmlns=\"https://asxgw.com/Schema/V2/Result\">"
                + "<success>true</success>"
                + "<referenceId>ref-1</referenceId>"
                + "<returnType>REDIRECT</returnType>"
                + "<redirectUrl>https://asxgw.paymentsandbox.cloud/redirect/abc</redirectUrl>"
                + "</result>";
        AllSecureResult result = AllSecureXml.parseResult(xml);
        assertTrue(result.isRedirect());
        assertEquals("https://asxgw.paymentsandbox.cloud/redirect/abc", result.getRedirectUrl());
    }

    @Test
    void parseResultReadsError() throws Exception {
        String xml = "<result xmlns=\"https://asxgw.com/Schema/V2/Result\">"
                + "<success>false</success>"
                + "<returnType>ERROR</returnType>"
                + "<errors><error><message>Card number invalid</message><code>2008</code></error></errors>"
                + "</result>";
        AllSecureResult result = AllSecureXml.parseResult(xml);
        assertTrue(result.isError());
        assertFalse(result.isSuccess());
        assertEquals("Card number invalid", result.getErrorMessage());
        assertEquals("2008", result.getErrorCode());
    }

    @Test
    void parseCallbackReadsCardAndCustomerData() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<callback xmlns=\"https://asxgw.com/Schema/V2/Callback\">"
                + "<result>OK</result>"
                + "<referenceId>edcba123456789012345</referenceId>"
                + "<transactionId>reg-1</transactionId>"
                + "<transactionType>REGISTER</transactionType>"
                + "<returnData type=\"creditcardData\"><creditcardData>"
                + "<type>visa</type><cardHolder>Max</cardHolder>"
                + "<expiryMonth>10</expiryMonth><expiryYear>2030</expiryYear>"
                + "<firstSixDigits>411111</firstSixDigits><lastFourDigits>1111</lastFourDigits>"
                + "</creditcardData></returnData>"
                + "<customerData><identification>john</identification></customerData>"
                + "</callback>";
        AllSecureCallback cb = AllSecureXml.parseCallback(xml);
        assertTrue(cb.isOk());
        assertEquals("REGISTER", cb.getTransactionType());
        assertEquals("edcba123456789012345", cb.getReferenceId());
        assertEquals("john", cb.getCustomerIdentification());
        assertEquals("visa", cb.getCardType());
        assertEquals("1111", cb.getCardLastFourDigits());
        assertEquals("10", cb.getCardExpiryMonth());
        assertEquals("2030", cb.getCardExpiryYear());
    }

    @Test
    void parseCallbackReadsError() throws Exception {
        String xml = "<callback xmlns=\"https://asxgw.com/Schema/V2/Callback\">"
                + "<result>ERROR</result>"
                + "<referenceId>ref-1</referenceId>"
                + "<transactionType>PREAUTHORIZE</transactionType>"
                + "<errors><error><message>Card declined</message><code>2003</code></error></errors>"
                + "</callback>";
        AllSecureCallback cb = AllSecureXml.parseCallback(xml);
        assertTrue(cb.isError());
        assertEquals("Card declined", cb.getErrorMessage());
        assertEquals("2003", cb.getErrorCode());
    }
}
