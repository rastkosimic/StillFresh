package com.stillfresh.app.paymentservice.service.executor;

import com.stillfresh.app.paymentservice.service.rail.pain001.SepaPain001Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Staging / fallback executor that generates an ISO 20022 pain.001.001.03
 * Credit Transfer Initiation XML file.  The file can be uploaded manually to
 * the bank's corporate banking portal (supported by virtually all European banks)
 * or submitted via the bank's file-upload API when that integration is ready.
 * <p>
 * One XML file is generated per {@link #execute} call, written to
 * {@code payout.executor.sepa-xml.output-dir} (default: {@code /tmp/stillfresh-payouts}).
 * The filename is returned as the externalReference so the admin can locate it.
 * <p>
 * Set {@code payout.rail=sepa-xml} to activate.
 */
@Component
@ConditionalOnProperty(name = "payout.rail", havingValue = "sepa-xml")
public class SepaXmlExportExecutor implements BankTransferExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SepaXmlExportExecutor.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Value("${payout.executor.sepa-xml.output-dir:/tmp/stillfresh-payouts}")
    private String outputDir;

    @Autowired
    private SepaPain001Builder pain001Builder;

    @Override
    public PayoutTransferResult execute(PayoutTransferRequest request) {
        try {
            Path dir = Paths.get(outputDir);
            Files.createDirectories(dir);

            String filename = String.format("payout-%s-%s.xml",
                    request.getIdempotencyKey(), LocalDate.now().format(DATE_FMT));
            Path outFile = dir.resolve(filename);

            // Idempotency: if the file already exists this key was already submitted
            if (Files.exists(outFile)) {
                logger.info("[SEPA-XML] File already exists for idempotencyKey={}, skipping duplicate", request.getIdempotencyKey());
                return PayoutTransferResult.success(filename);
            }

            String xml = pain001Builder.build(request);
            Files.writeString(outFile, xml, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);

            logger.info("[SEPA-XML] Generated pain.001 file: {}", outFile.toAbsolutePath());
            return PayoutTransferResult.success(filename);

        } catch (IOException e) {
            logger.error("[SEPA-XML] Failed to write pain.001 for idempotencyKey={}: {}", request.getIdempotencyKey(), e.getMessage(), e);
            return PayoutTransferResult.failure("IO error: " + e.getMessage());
        }
    }
}
