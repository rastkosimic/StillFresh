package com.stillfresh.app.orderservice.listener;

import com.stillfresh.app.orderservice.controller.OrderStatsController;
import com.stillfresh.app.orderservice.repository.OrderRepository;
import com.stillfresh.app.orderservice.repository.projections.OfferBreakdownProjection;
import com.stillfresh.app.orderservice.repository.projections.VendorTotalsProjection;
import com.stillfresh.app.orderservice.publisher.OrderEventPublisher;
import com.stillfresh.app.sharedentities.dto.VendorStatsResponse;
import com.stillfresh.app.sharedentities.order.events.VendorStatsRequestEvent;
import com.stillfresh.app.sharedentities.order.events.VendorStatsResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class VendorStatsRequestListener {

    private static final Logger logger = LoggerFactory.getLogger(VendorStatsRequestListener.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderEventPublisher eventPublisher;

    @KafkaListener(topics = "${kafka.topic.vendor-stats-request:vendor-stats-request}", groupId = "order-service-group")
    public void handleVendorStatsRequest(VendorStatsRequestEvent event) {
        logger.info("Received VendorStatsRequestEvent for vendorId: {}", event.getVendorId());

        String correlationId = event.getCorrelationId();
        try {
            OffsetDateTime from = (event.getFrom() == null || event.getFrom().isBlank()) ? null : OffsetDateTime.parse(event.getFrom());
            OffsetDateTime to = (event.getTo() == null || event.getTo().isBlank()) ? null : OffsetDateTime.parse(event.getTo());

            VendorTotalsProjection totals = orderRepository.aggregateTotalsByVendor(event.getVendorId(), from, to);
            List<OfferBreakdownProjection> breakdown = orderRepository.aggregateByOffer(event.getVendorId(), from, to);

            VendorStatsResponse stats = OrderStatsController.buildStatsResponse(totals, breakdown, from, to);
            eventPublisher.publishVendorStatsResponse(new VendorStatsResponseEvent(correlationId, true, null, stats));
        } catch (Exception ex) {
            logger.error("Failed to compute stats for vendorId: {}", event.getVendorId(), ex);
            eventPublisher.publishVendorStatsResponse(new VendorStatsResponseEvent(correlationId, false, ex.getMessage(), null));
        }
    }
}
