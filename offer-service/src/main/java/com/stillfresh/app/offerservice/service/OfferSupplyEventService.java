package com.stillfresh.app.offerservice.service;

import com.stillfresh.app.offerservice.model.Offer;
import com.stillfresh.app.offerservice.model.OfferSupplyEvent;
import com.stillfresh.app.offerservice.model.OfferSupplyEventType;
import com.stillfresh.app.offerservice.repository.OfferSupplyEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfferSupplyEventService {

    private static final Logger logger = LoggerFactory.getLogger(OfferSupplyEventService.class);

    @Autowired
    private OfferSupplyEventRepository repository;

    @Transactional
    public void recordCreate(Offer offer, int quantity) {
        if (quantity <= 0 || offer.getId() == null) {
            return;
        }
        saveEvent(offer, quantity, OfferSupplyEventType.CREATE);
    }

    @Transactional
    public void recordReplenishDelta(Offer offer, int oldQuantity, int newQuantity) {
        int delta = Math.max(0, newQuantity - oldQuantity);
        if (delta <= 0 || offer.getId() == null) {
            return;
        }
        saveEvent(offer, delta, OfferSupplyEventType.REPLENISH);
    }

    private void saveEvent(Offer offer, int quantity, OfferSupplyEventType type) {
        OfferSupplyEvent event = new OfferSupplyEvent();
        event.setOfferId(offer.getId());
        event.setVendorId(offer.getVendorId());
        event.setQuantityUnits(quantity);
        event.setEventType(type);
        repository.save(event);
        logger.info("Recorded {} supply event: offerId={}, vendorId={}, units={}",
                type, offer.getId(), offer.getVendorId(), quantity);
    }
}
