package com.stillfresh.app.sharedentities.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Refuses to start a service under the {@code prod} profile when a security-critical setting
 * still holds a development value.
 *
 * <p>Spring has no {@code ${VAR:?fail}} syntax, so committed YAML defaults silently mask a
 * missing environment variable. The {@code prod} profiles declare secrets without defaults so
 * placeholder resolution fails outright; this guard covers the cases a missing-value check
 * cannot catch — a secret that is present but weak, left at a published default, or a payment
 * setting that would route real money to a stub.
 *
 * <p>Only properties the service actually declares are inspected, so one guard works for all
 * services regardless of which subset of configuration they use.
 */
@Component
@Profile("prod")
public class ProdConfigGuard implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(ProdConfigGuard.class);

    /**
     * Minimum secret length. HS256 needs a 256-bit key, and this is also a reasonable floor
     * for the gateway trust secret.
     */
    private static final int MIN_SECRET_LENGTH = 32;

    /** Values published in committed YAML and {@code .env.example}; treat as public knowledge. */
    private static final List<String> KNOWN_DEV_SECRETS = List.of(
            "stillfresh-gw-dev-secret-change-in-prod",
            "change-me",
            "change-me-use-a-long-random-string");

    /** AllSecure simulator credentials from the public sandbox documentation. */
    private static final List<String> KNOWN_SANDBOX_CREDENTIALS = List.of(
            "genericmerchant-simulator-1",
            "genericmerchant-api-1",
            "sJBHPYLTFPNXjDYrQocG-1");

    private final Environment environment;

    public ProdConfigGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        List<String> problems = new ArrayList<>();

        checkSecret("jwt.secret", "JWT_SECRET", problems);
        checkSecret("gateway.internal.secret", "GATEWAY_INTERNAL_SECRET", problems);

        checkSchemaManagement(problems);
        checkSqlLogging(problems);
        checkPaymentProvider(problems);
        checkPayoutRail(problems);

        if (!problems.isEmpty()) {
            StringBuilder message = new StringBuilder(
                    "Refusing to start with profile 'prod': insecure configuration detected.");
            for (String problem : problems) {
                message.append(System.lineSeparator()).append("  - ").append(problem);
            }
            throw new IllegalStateException(message.toString());
        }

        logger.info("Production configuration guard passed for {}",
                environment.getProperty("spring.application.name", "this service"));
    }

    /** A secret must be present, long enough, and not a value that has been published. */
    private void checkSecret(String property, String envVar, List<String> problems) {
        String value = environment.getProperty(property);
        if (value == null) {
            // The service does not use this secret at all.
            return;
        }
        if (value.isBlank()) {
            problems.add(property + " is empty; set " + envVar + " in the environment");
            return;
        }
        if (value.length() < MIN_SECRET_LENGTH) {
            problems.add(property + " is only " + value.length() + " characters; " + envVar
                    + " must be at least " + MIN_SECRET_LENGTH);
        }
        if (KNOWN_DEV_SECRETS.contains(value)) {
            problems.add(property + " is still the committed development default; rotate " + envVar);
        }
    }

    /** Hibernate must not be allowed to mutate a production schema. */
    private void checkSchemaManagement(List<String> problems) {
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
        if (ddlAuto == null) {
            return;
        }
        if (!"validate".equalsIgnoreCase(ddlAuto) && !"none".equalsIgnoreCase(ddlAuto)) {
            problems.add("spring.jpa.hibernate.ddl-auto is '" + ddlAuto
                    + "'; production must use 'validate' or 'none' and apply schema changes via migrations");
        }
    }

    /** SQL logging writes bind parameters, which for these services means PII and bank fields. */
    private void checkSqlLogging(List<String> problems) {
        if (Boolean.TRUE.equals(environment.getProperty("spring.jpa.show-sql", Boolean.class))) {
            problems.add("spring.jpa.show-sql is true; SQL logging exposes personal data in production logs");
        }
        if (Boolean.TRUE.equals(
                environment.getProperty("spring.jpa.properties.hibernate.format_sql", Boolean.class))) {
            problems.add("hibernate.format_sql is true; disable SQL logging in production");
        }
    }

    /** Card processing must not point at the sandbox host or use simulator credentials. */
    private void checkPaymentProvider(List<String> problems) {
        String baseUrl = environment.getProperty("allsecure.base-url");
        if (baseUrl != null && baseUrl.contains("paymentsandbox")) {
            problems.add("allsecure.base-url points at the sandbox host (" + baseUrl
                    + "); set ALLSECURE_BASE_URL to the production gateway");
        }
        if (baseUrl != null && baseUrl.startsWith("http://")) {
            problems.add("allsecure.base-url must use https, got " + baseUrl);
        }

        for (String property : List.of("allsecure.api-key", "allsecure.username",
                "allsecure.integration-key")) {
            String value = environment.getProperty(property);
            if (value != null && KNOWN_SANDBOX_CREDENTIALS.contains(value)) {
                problems.add(property + " is still an AllSecure simulator credential");
            }
        }

        String publicBaseUrl = environment.getProperty("allsecure.public-base-url");
        if (publicBaseUrl != null && publicBaseUrl.startsWith("http://")) {
            problems.add("allsecure.public-base-url must be a public https URL, got " + publicBaseUrl
                    + "; the provider posts callbacks to it");
        }
    }

    /**
     * The stub rail logs a transfer and marks it complete without sending anything, so combining
     * it with automatic execution would silently mark real vendor payouts as paid. Building and
     * approving batches on a stub rail is harmless, so only execution is blocked.
     */
    private void checkPayoutRail(List<String> problems) {
        String rail = environment.getProperty("payout.rail");
        if (rail == null) {
            return;
        }
        boolean autoExecute = Boolean.TRUE.equals(
                environment.getProperty("payout.auto.execute", Boolean.class));
        if (!autoExecute) {
            return;
        }
        if ("stub".equalsIgnoreCase(rail)) {
            problems.add("payout.rail is 'stub' while payout.auto.execute is true; the stub rail marks "
                    + "payouts complete without transferring funds. Set PAYOUT_RAIL to sepa-xml or cmiplus, "
                    + "or set PAYOUT_AUTO_EXECUTE=false");
        }
        if ("cmiplus".equalsIgnoreCase(rail) && Boolean.TRUE.equals(
                environment.getProperty("payout.cmiplus.stub-mode", Boolean.class))) {
            problems.add("payout.cmiplus.stub-mode is true while payout.auto.execute is true; "
                    + "set CMIPLUS_STUB_MODE=false or PAYOUT_AUTO_EXECUTE=false");
        }
    }
}
