package com.stillfresh.app.vendorservice.service;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeoLocationService {

    private final GeoApiContext geoApiContext;

    public GeoLocationService(@Value("${google.maps.api-key}") String apiKey) {
        this.geoApiContext = new GeoApiContext.Builder()
            .apiKey(apiKey)
            .build();
    }

    public double[] getCoordinates(String address) {
        try {
            GeocodingResult[] results = GeocodingApi.geocode(geoApiContext, address).await();
            if (results.length > 0) {
                double latitude = results[0].geometry.location.lat;
                double longitude = results[0].geometry.location.lng;
                return new double[]{latitude, longitude};
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch coordinates for address: " + address, e);
        }
        return null;
    }
}
