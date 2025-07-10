package com.stillfresh.app.vendorservice.service;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.errors.ApiException;
import com.google.maps.errors.OverQueryLimitException;
import com.google.maps.errors.RequestDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeoLocationService {

    private static final Logger logger = LoggerFactory.getLogger(GeoLocationService.class);
    private final GeoApiContext geoApiContext;

    public GeoLocationService(@Value("${google.maps.api-key}") String apiKey) {
        this.geoApiContext = new GeoApiContext.Builder()
            .apiKey(apiKey)
            .build();
        logger.info("GeoLocationService initialized with API key: {}", apiKey.substring(0, Math.min(10, apiKey.length())) + "...");
    }

    public double[] getCoordinates(String address) {
        try {
            logger.debug("Attempting to geocode address: {}", address);
            GeocodingResult[] results = GeocodingApi.geocode(geoApiContext, address).await();
            
            if (results != null && results.length > 0) {
                double latitude = results[0].geometry.location.lat;
                double longitude = results[0].geometry.location.lng;
                logger.debug("Successfully geocoded address: {} -> lat: {}, lng: {}", address, latitude, longitude);
                return new double[]{latitude, longitude};
            } else {
                logger.warn("No geocoding results found for address: {}", address);
                return null;
            }
        } catch (OverQueryLimitException e) {
            logger.error("Google Maps API quota exceeded for address: {}", address, e);
            throw new RuntimeException("Google Maps API quota exceeded. Please try again later.", e);
        } catch (RequestDeniedException e) {
            logger.error("Google Maps API request denied for address: {}. Check API key permissions.", address, e);
            throw new RuntimeException("Google Maps API access denied. Please check API key configuration.", e);
        } catch (ApiException e) {
            logger.error("Google Maps API error for address: {}. Error: {}", address, e.getMessage(), e);
            throw new RuntimeException("Google Maps API error: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during geocoding for address: {}", address, e);
            throw new RuntimeException("Failed to fetch coordinates for address: " + address, e);
        }
    }
}
