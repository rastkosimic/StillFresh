package com.stillfresh.app.sharedentities.security;

/**
 * Header and role names used to authenticate service-to-service calls that do not carry an
 * end-user JWT.
 *
 * <p>Some internal endpoints run during registration flows, before any user is authenticated,
 * so there is no Authorization header to forward. Rather than leaving them open, callers present
 * a shared secret and the receiving service grants only {@link #ROLE_INTERNAL_SERVICE}.
 */
public final class InternalServiceHeaders {

    /** Shared secret proving the caller is another StillFresh service. */
    public static final String INTERNAL_SECRET = "X-Internal-Service-Secret";

    /** Authority granted to a verified internal caller. Never granted to end users. */
    public static final String ROLE_INTERNAL_SERVICE = "ROLE_INTERNAL_SERVICE";

    /** Role name without the Spring Security {@code ROLE_} prefix, for {@code hasRole(...)}. */
    public static final String INTERNAL_SERVICE = "INTERNAL_SERVICE";

    private InternalServiceHeaders() {
    }
}
