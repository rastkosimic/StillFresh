package com.stillfresh.app.paymentservice.allsecure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Builds AllSecure transaction request XML (Schema V2) and parses result/callback responses.
 *
 * <p>Requests are built as exact strings so the signature can be computed over the precise bytes
 * that are transmitted. Responses are parsed with a namespace-agnostic Jackson XML tree.</p>
 */
public final class AllSecureXml {

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    private AllSecureXml() {}

    // ── Request builders ──────────────────────────────────────────────────────
    // The transaction namespace must match the gateway host (e.g. http://asxgw.paymentsandbox.cloud/Schema/V2/Transaction
    // for the sandbox), not the production asxgw.com host, or the gateway rejects the body with error 1005.

    /** Register: stores a customer's payment instrument for future card-on-file charges (hosted redirect). */
    public static String buildRegister(String namespace, String username, String sha1Password, String transactionId,
                                       String customerIdentification, String description,
                                       String successUrl, String errorUrl, String cancelUrl, String callbackUrl) {
        StringBuilder inner = new StringBuilder();
        inner.append(el("transactionId", transactionId));
        inner.append(customerBlock(customerIdentification));
        inner.append(el("description", description));
        inner.append(el("successUrl", successUrl));
        inner.append(el("cancelUrl", cancelUrl));
        inner.append(el("errorUrl", errorUrl));
        inner.append(el("callbackUrl", callbackUrl));
        return envelope(namespace, username, sha1Password, "register", inner.toString());
    }

    /** Preauthorize against a stored card (card-on-file). */
    public static String buildPreauthorize(String namespace, String username, String sha1Password, String transactionId,
                                           String customerIdentification, String amount, String currency,
                                           String description, String referenceTransactionId,
                                           String transactionIndicator, String callbackUrl) {
        StringBuilder inner = new StringBuilder();
        inner.append(el("transactionId", transactionId));
        inner.append(customerBlock(customerIdentification));
        if (notBlank(referenceTransactionId)) {
            inner.append(el("referenceTransactionId", referenceTransactionId));
        }
        inner.append(el("amount", amount));
        inner.append(el("currency", currency));
        inner.append(el("description", description));
        inner.append(el("callbackUrl", callbackUrl));
        if (notBlank(transactionIndicator)) {
            inner.append(el("transactionIndicator", transactionIndicator));
        }
        return envelope(namespace, username, sha1Password, "preauthorize", inner.toString());
    }

    /** Capture a previously preauthorized transaction. */
    public static String buildCapture(String namespace, String username, String sha1Password, String transactionId,
                                      String referenceTransactionId, String amount, String currency) {
        StringBuilder inner = new StringBuilder();
        inner.append(el("transactionId", transactionId));
        inner.append(el("referenceTransactionId", referenceTransactionId));
        if (notBlank(amount)) inner.append(el("amount", amount));
        if (notBlank(currency)) inner.append(el("currency", currency));
        return envelope(namespace, username, sha1Password, "capture", inner.toString());
    }

    /** Void a previously preauthorized transaction (release the hold). */
    public static String buildVoid(String namespace, String username, String sha1Password, String transactionId,
                                   String referenceTransactionId) {
        StringBuilder inner = new StringBuilder();
        inner.append(el("transactionId", transactionId));
        inner.append(el("referenceTransactionId", referenceTransactionId));
        return envelope(namespace, username, sha1Password, "void", inner.toString());
    }

    /** Deregister a stored payment instrument. */
    public static String buildDeregister(String namespace, String username, String sha1Password, String transactionId,
                                         String referenceTransactionId) {
        StringBuilder inner = new StringBuilder();
        inner.append(el("transactionId", transactionId));
        inner.append(el("referenceTransactionId", referenceTransactionId));
        return envelope(namespace, username, sha1Password, "deregister", inner.toString());
    }

    private static String envelope(String namespace, String username, String sha1Password, String type, String innerXml) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<transaction xmlns=\"" + namespace + "\">"
                + el("username", username)
                + el("password", sha1Password)
                + "<" + type + ">" + innerXml + "</" + type + ">"
                + "</transaction>";
    }

    private static String customerBlock(String identification) {
        if (!notBlank(identification)) return "";
        return "<customer>" + el("identification", identification) + "</customer>";
    }

    private static String el(String name, String value) {
        if (value == null) return "";
        return "<" + name + ">" + escape(value) + "</" + name + ">";
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&apos;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    // ── Response / callback parsers ──────────────────────────────────────────

    public static AllSecureResult parseResult(String xml) throws Exception {
        JsonNode root = XML_MAPPER.readTree(xml.getBytes());
        AllSecureResult result = new AllSecureResult();
        result.setRawXml(xml);
        result.setSuccess(asBool(text(root, "success")));
        result.setReturnType(text(root, "returnType"));
        result.setReferenceId(text(root, "referenceId"));
        result.setRegistrationId(text(root, "registrationId"));
        result.setPurchaseId(text(root, "purchaseId"));
        result.setRedirectUrl(text(root, "redirectUrl"));

        JsonNode error = firstError(root);
        if (error != null) {
            result.setErrorMessage(text(error, "message"));
            result.setErrorCode(text(error, "code"));
        }
        return result;
    }

    public static AllSecureCallback parseCallback(String xml) throws Exception {
        JsonNode root = XML_MAPPER.readTree(xml.getBytes());
        AllSecureCallback cb = new AllSecureCallback();
        cb.setResult(text(root, "result"));
        cb.setReferenceId(text(root, "referenceId"));
        cb.setTransactionId(text(root, "transactionId"));
        cb.setPurchaseId(text(root, "purchaseId"));
        cb.setTransactionType(text(root, "transactionType"));
        cb.setMerchantMetaData(text(root, "merchantMetaData"));
        cb.setAmount(text(root, "amount"));
        cb.setCurrency(text(root, "currency"));

        JsonNode error = firstError(root);
        if (error != null) {
            cb.setErrorMessage(text(error, "message"));
            cb.setErrorCode(text(error, "code"));
        }

        JsonNode customerData = root.get("customerData");
        if (customerData != null) {
            cb.setCustomerIdentification(text(customerData, "identification"));
        }

        JsonNode returnData = root.get("returnData");
        if (returnData != null) {
            JsonNode card = returnData.get("creditcardData");
            if (card != null) {
                cb.setCardType(text(card, "type"));
                cb.setCardHolder(text(card, "cardHolder"));
                cb.setCardFirstSixDigits(text(card, "firstSixDigits"));
                cb.setCardLastFourDigits(text(card, "lastFourDigits"));
                cb.setCardExpiryMonth(text(card, "expiryMonth"));
                cb.setCardExpiryYear(text(card, "expiryYear"));
            }
        }
        return cb;
    }

    private static JsonNode firstError(JsonNode root) {
        JsonNode errors = root.get("errors");
        if (errors == null) return null;
        JsonNode error = errors.get("error");
        if (error == null) return null;
        return error.isArray() ? (error.size() > 0 ? error.get(0) : null) : error;
    }

    private static String text(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        String s = v.asText();
        return (s != null && !s.isEmpty()) ? s : null;
    }

    private static boolean asBool(String s) {
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }
}
