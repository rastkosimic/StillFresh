package com.stillfresh.app.paymentservice.service.rail;

import com.stillfresh.app.paymentservice.model.VendorPayoutItem;
import com.stillfresh.app.paymentservice.service.executor.BankTransferExecutor;
import com.stillfresh.app.paymentservice.service.executor.PayoutTransferRequest;
import com.stillfresh.app.paymentservice.service.executor.PayoutTransferResult;
import com.stillfresh.app.paymentservice.service.executor.SepaXmlExportExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing the legacy synchronous {@link BankTransferExecutor}
 * implementations (stub, sepa-xml) through the {@link PayoutRail} interface.
 * Active whenever one of those executors is on the context, i.e. when
 * {@code payout.rail} is {@code stub} (default) or {@code sepa-xml}.
 */
@Component
@ConditionalOnBean(BankTransferExecutor.class)
public class SyncExecutorPayoutRail implements PayoutRail {

    private final BankTransferExecutor executor;
    private final String railType;

    public SyncExecutorPayoutRail(BankTransferExecutor executor) {
        this.executor = executor;
        this.railType = executor instanceof SepaXmlExportExecutor ? "SEPA-XML" : "STUB";
    }

    @Override
    public PayoutSubmissionResult submit(PayoutTransferRequest request) {
        PayoutTransferResult result = executor.execute(request);
        return result.isSuccess()
                ? PayoutSubmissionResult.completed(result.getExternalReference())
                : PayoutSubmissionResult.failed(result.getErrorMessage());
    }

    @Override
    public PayoutStatusUpdate pollStatus(VendorPayoutItem item) {
        throw new UnsupportedOperationException(
            "Synchronous rail " + railType + " never leaves items in SUBMITTED");
    }

    @Override
    public String railType() {
        return railType;
    }
}
