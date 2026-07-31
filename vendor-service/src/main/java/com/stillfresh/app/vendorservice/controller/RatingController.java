package com.stillfresh.app.vendorservice.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.stillfresh.app.sharedentities.responses.ApiResponse;
import com.stillfresh.app.sharedentities.responses.ErrorResponse;
import com.stillfresh.app.vendorservice.dto.RatingRequest;
import com.stillfresh.app.vendorservice.dto.RatingResponse;
import com.stillfresh.app.vendorservice.dto.VendorRatingSummary;
import com.stillfresh.app.vendorservice.exception.RatingValidationException;
import com.stillfresh.app.vendorservice.service.RatingService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/vendors/ratings")
@Tag(name = "Rating Management", description = "APIs for submitting and retrieving vendor ratings")
public class RatingController {

    private static final Logger logger = LoggerFactory.getLogger(RatingController.class);

    @Autowired
    private RatingService ratingService;

    @Operation(
        summary = "Submit a rating for a vendor",
        description = "Allows a user to rate a vendor after a completed order pickup. " +
                      "Each category is rated from 1 to 5 stars. One rating per order; " +
                      "resubmitting for the same order updates that order's rating. " +
                      "Vendor averages aggregate all order ratings."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rating submitted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or order not eligible"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Order does not belong to user"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Vendor or order not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/submit")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> submitRating(
            @Valid @RequestBody RatingRequest ratingRequest,
            BindingResult result,
            HttpServletRequest request) {
        try {
            if (result.hasErrors()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Validation failed: " + result.getAllErrors().get(0).getDefaultMessage()));
            }

            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("User not authenticated"));
            }

            RatingResponse response = ratingService.submitRating(userId, ratingRequest);
            return ResponseEntity.ok(response);
        } catch (RatingValidationException e) {
            logger.error("Error submitting rating: {}", e.getMessage());
            return ResponseEntity.status(e.getStatus())
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error submitting rating: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to submit rating: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "Get all ratings for a vendor",
        description = "Retrieves all ratings submitted for a specific vendor"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ratings retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/vendor/{vendorId}")
    @PreAuthorize("hasAnyRole('USER', 'VENDOR', 'ADMIN')")
    public ResponseEntity<List<RatingResponse>> getRatingsByVendorId(
            @Parameter(description = "Vendor ID", required = true)
            @PathVariable Long vendorId) {
        List<RatingResponse> ratings = ratingService.getRatingsByVendorId(vendorId);
        return ResponseEntity.ok(ratings);
    }

    @Operation(
        summary = "Get all ratings by the current user",
        description = "Retrieves all ratings submitted by the currently authenticated user (one per completed order)"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ratings retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my-ratings")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<RatingResponse>> getMyRatings(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<RatingResponse> ratings = ratingService.getRatingsByUserId(userId);
        return ResponseEntity.ok(ratings);
    }

    @Operation(
        summary = "Get rating summary for a vendor",
        description = "Retrieves a summary of ratings for a vendor including average ratings per category"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rating summary retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/vendor/{vendorId}/summary")
    @PreAuthorize("hasAnyRole('USER', 'VENDOR', 'ADMIN')")
    public ResponseEntity<VendorRatingSummary> getVendorRatingSummary(
            @Parameter(description = "Vendor ID", required = true)
            @PathVariable Long vendorId) {
        VendorRatingSummary summary = ratingService.getVendorRatingSummary(vendorId);
        return ResponseEntity.ok(summary);
    }

    @Operation(
        summary = "Check if an order has been rated",
        description = "Checks if a rating has already been submitted for a specific completed order"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Check completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/order/{orderId}/has-rated")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse> hasOrderBeenRated(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long orderId) {
        boolean hasRated = ratingService.hasOrderBeenRated(orderId);
        return ResponseEntity.ok(new ApiResponse(true, "Order has been rated: " + hasRated));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        try {
            Object userIdAttr = request.getAttribute("userId");
            if (userIdAttr != null) {
                if (userIdAttr instanceof Long) {
                    return (Long) userIdAttr;
                } else if (userIdAttr instanceof Number) {
                    return ((Number) userIdAttr).longValue();
                }
            }

            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null && !userIdHeader.isEmpty()) {
                return Long.parseLong(userIdHeader);
            }

            logger.warn("Could not extract user ID from request");
            return null;
        } catch (Exception e) {
            logger.error("Error extracting user ID from request: {}", e.getMessage());
            return null;
        }
    }
}
