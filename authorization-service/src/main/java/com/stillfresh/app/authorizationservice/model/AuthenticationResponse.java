package com.stillfresh.app.authorizationservice.model;

/**
 * Response DTO for authentication/login
 * Includes JWT token, role, and optional vendor information
 */
public class AuthenticationResponse {
    /** New field for modern clients (mirrors {@link #jwt} for backward compatibility). */
    private String accessJwt;
    /** Long-lived refresh token used to mint new access tokens. */
    private String refreshToken;
    private String jwt;
    private String role;
    private VendorInfo vendor;  // Only populated for VENDOR and VENDOR_ADMIN roles
    /** When true, the account was previously deleted and has been reactivated; app can show "Welcome back" message. */
    private Boolean accountWasDeleted;

    // Default constructor
    public AuthenticationResponse() {
    }

    // Constructor with parameters
    public AuthenticationResponse(String jwt, String role) {
        this.jwt = jwt;
        this.accessJwt = jwt;
        this.role = role;
    }

    // Constructor with access + refresh tokens
    public AuthenticationResponse(String accessJwt, String refreshToken, String role, Boolean accountWasDeleted) {
        this.accessJwt = accessJwt;
        this.refreshToken = refreshToken;
        this.jwt = accessJwt; // legacy
        this.role = role;
        this.accountWasDeleted = accountWasDeleted;
    }

    // Constructor with vendor info
    public AuthenticationResponse(String jwt, String role, VendorInfo vendor) {
        this.jwt = jwt;
        this.accessJwt = jwt;
        this.role = role;
        this.vendor = vendor;
    }

    // Constructor with accountWasDeleted (e.g. reactivated after delete)
    public AuthenticationResponse(String jwt, String role, Boolean accountWasDeleted) {
        this.jwt = jwt;
        this.accessJwt = jwt;
        this.role = role;
        this.accountWasDeleted = accountWasDeleted;
    }

    // Getters and setters
    public String getAccessJwt() {
        return accessJwt;
    }

    public void setAccessJwt(String accessJwt) {
        this.accessJwt = accessJwt;
        this.jwt = accessJwt; // keep legacy field in sync
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
        if (this.accessJwt == null) {
            this.accessJwt = jwt;
        }
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public VendorInfo getVendor() {
        return vendor;
    }

    public void setVendor(VendorInfo vendor) {
        this.vendor = vendor;
    }

    public Boolean getAccountWasDeleted() {
        return accountWasDeleted;
    }

    public void setAccountWasDeleted(Boolean accountWasDeleted) {
        this.accountWasDeleted = accountWasDeleted;
    }

    /**
     * Vendor information included in login response
     */
    public static class VendorInfo {
        private Long id;
        private String email;
        private Boolean isHeadquarters;
        private Boolean isChainLocation;
        private Boolean isUniqueVendor;
        private String chainName;
        private String locationName;
        private Boolean usesSharedPaymentAccount;

        public VendorInfo() {
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Boolean getIsHeadquarters() {
            return isHeadquarters;
        }

        public void setIsHeadquarters(Boolean isHeadquarters) {
            this.isHeadquarters = isHeadquarters;
        }

        public Boolean getIsChainLocation() {
            return isChainLocation;
        }

        public void setIsChainLocation(Boolean isChainLocation) {
            this.isChainLocation = isChainLocation;
        }

        public Boolean getIsUniqueVendor() {
            return isUniqueVendor;
        }

        public void setIsUniqueVendor(Boolean isUniqueVendor) {
            this.isUniqueVendor = isUniqueVendor;
        }

        public String getChainName() {
            return chainName;
        }

        public void setChainName(String chainName) {
            this.chainName = chainName;
        }

        public String getLocationName() {
            return locationName;
        }

        public void setLocationName(String locationName) {
            this.locationName = locationName;
        }

        public Boolean getUsesSharedPaymentAccount() {
            return usesSharedPaymentAccount;
        }

        public void setUsesSharedPaymentAccount(Boolean usesSharedPaymentAccount) {
            this.usesSharedPaymentAccount = usesSharedPaymentAccount;
        }
    }
} 