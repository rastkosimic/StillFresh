package com.stillfresh.app.paymentservice.cmiplus;

import com.stillfresh.app.paymentservice.service.executor.PayoutTransferRequest;
import com.stillfresh.app.paymentservice.service.rail.pain001.DomesticLcyPain001Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Submits ISO 20022 pain.001.001.03 payment initiation messages to CMIplus.
 */
@Component
public class CmiplusPaymentInitiationClient {

    private static final Logger logger = LoggerFactory.getLogger(CmiplusPaymentInitiationClient.class);
    private static final Pattern MSG_ID_PATTERN = Pattern.compile("<MsgId>([^<]+)</MsgId>");

    private final CmiplusProperties properties;
    private final CmiplusHttpClient httpClient;
    private final DomesticLcyPain001Builder pain001Builder;

    public CmiplusPaymentInitiationClient(CmiplusProperties properties,
                                          CmiplusHttpClient httpClient,
                                          DomesticLcyPain001Builder pain001Builder) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.pain001Builder = pain001Builder;
    }

    public InitiationResponse initiate(PayoutTransferRequest request) {
        String xml = pain001Builder.build(request);
        String msgId = extractMsgId(xml);

        if (properties.isStubMode()) {
            logger.info("[CMIplus-STUB] pain.001 accepted msgId={} endToEndId={}",
                    msgId, request.getIdempotencyKey());
            return new InitiationResponse(msgId, "STUB-ACCEPTED");
        }

        String responseBody = httpClient.postXml(properties.getPaymentInitiationPath(), xml);
        logger.info("[CMIplus] Payment initiated msgId={} responseLength={}",
                msgId, responseBody != null ? responseBody.length() : 0);
        return new InitiationResponse(msgId, msgId);
    }

    private String extractMsgId(String xml) {
        Matcher m = MSG_ID_PATTERN.matcher(xml);
        return m.find() ? m.group(1) : "SF-UNKNOWN";
    }

    public record InitiationResponse(String bankMessageId, String externalReference) {}
}
