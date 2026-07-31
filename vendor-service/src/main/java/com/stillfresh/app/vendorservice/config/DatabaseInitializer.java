package com.stillfresh.app.vendorservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            // Check if stripe_account_id column exists
            String checkColumnQuery = """
                SELECT COUNT(*) 
                FROM information_schema.columns 
                WHERE table_name = 'vendor' 
                AND column_name = 'stripe_account_id'
                """;
            
            Integer columnExists = jdbcTemplate.queryForObject(checkColumnQuery, Integer.class);
            
            if (columnExists == null || columnExists == 0) {
                logger.info("stripe_account_id column does not exist, adding it...");
                
                // Add the stripe_account_id column (nullable, as existing vendors won't have this)
                String addColumnQuery = """
                    ALTER TABLE vendor 
                    ADD COLUMN stripe_account_id VARCHAR(255)
                    """;
                
                jdbcTemplate.execute(addColumnQuery);
                
                // Create index for better performance when looking up vendors by Stripe account ID
                String createIndexQuery = """
                    CREATE INDEX IF NOT EXISTS idx_vendor_stripe_account_id 
                    ON vendor(stripe_account_id)
                    """;
                
                jdbcTemplate.execute(createIndexQuery);
                
                logger.info("Successfully added stripe_account_id column and index to vendor table");
            } else {
                logger.info("stripe_account_id column already exists in vendor table");
            }
            
        } catch (Exception e) {
            logger.error("Error initializing database schema: {}", e.getMessage(), e);
            // Don't throw exception - allow application to continue even if column already exists
            // This is safe because Hibernate's ddl-auto: update will also handle it
        }
    }
}


