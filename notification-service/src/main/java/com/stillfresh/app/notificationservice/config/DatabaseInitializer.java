package com.stillfresh.app.notificationservice.config;

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
            // Check if is_read column exists
            String checkColumnQuery = """
                SELECT COUNT(*) 
                FROM information_schema.columns 
                WHERE table_name = 'notifications' 
                AND column_name = 'is_read'
                """;
            
            Integer columnExists = jdbcTemplate.queryForObject(checkColumnQuery, Integer.class);
            
            if (columnExists == null || columnExists == 0) {
                logger.info("is_read column does not exist, adding it...");
                
                // Add the is_read column
                String addColumnQuery = """
                    ALTER TABLE notifications 
                    ADD COLUMN is_read BOOLEAN NOT NULL DEFAULT FALSE
                    """;
                
                jdbcTemplate.execute(addColumnQuery);
                
                // Create index for better performance
                String createIndexQuery = """
                    CREATE INDEX IF NOT EXISTS idx_notifications_user_read 
                    ON notifications(user_id, is_read)
                    """;
                
                jdbcTemplate.execute(createIndexQuery);
                
                logger.info("Successfully added is_read column and index to notifications table");
            } else {
                logger.info("is_read column already exists in notifications table");
            }

            // Add deleted column for soft delete (hide from user listing)
            String checkDeletedQuery = """
                SELECT COUNT(*) 
                FROM information_schema.columns 
                WHERE table_name = 'notifications' 
                AND column_name = 'deleted'
                """;
            Integer deletedColumnExists = jdbcTemplate.queryForObject(checkDeletedQuery, Integer.class);
            if (deletedColumnExists == null || deletedColumnExists == 0) {
                logger.info("deleted column does not exist, adding it...");
                jdbcTemplate.execute("ALTER TABLE notifications ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE");
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_notifications_user_deleted ON notifications(user_id, deleted)");
                logger.info("Successfully added deleted column and index to notifications table");
            } else {
                logger.info("deleted column already exists in notifications table");
            }
            
        } catch (Exception e) {
            logger.error("Error initializing database schema: {}", e.getMessage(), e);
            throw e;
        }
    }
}
