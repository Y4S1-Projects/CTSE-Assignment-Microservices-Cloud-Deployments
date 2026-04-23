package com.example.catalogservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class CatalogDataSeeder implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(CatalogDataSeeder.class);

    private final JdbcTemplate jdbcTemplate;

    public CatalogDataSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        List<SeedItem> seedItems = List.of(
                new SeedItem(
                        "ITEM-0001",
                        "Green Garden Salad",
                        "Fresh lettuce, cucumber, avocado, and light herb dressing",
                        BigDecimal.valueOf(8.5),
                        "Salads",
                        20,
                        "https://images.unsplash.com/photo-1546793665-c74683f339c1?w=800"),
                new SeedItem(
                        "ITEM-0002",
                        "Chicken Rice Bowl",
                        "Grilled chicken, jasmine rice, and sautéed vegetables",
                        BigDecimal.valueOf(12.0),
                        "Main",
                        15,
                        "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=800"),
                new SeedItem(
                        "ITEM-0003",
                        "Pasta Primavera",
                        "Penne pasta with seasonal veggies and basil sauce",
                        BigDecimal.valueOf(11.25),
                        "Pasta",
                        10,
                        "https://images.unsplash.com/photo-1473093295043-cdd812d0e601?w=800"),
                new SeedItem(
                        "ITEM-0004",
                        "Matcha Smoothie",
                        "Green tea smoothie with banana and almond milk",
                        BigDecimal.valueOf(6.75),
                        "Drinks",
                        5,
                        "https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=800")
        );

        int createdCount = 0;
        for (SeedItem item : seedItems) {
            createdCount += seedItemIfMissing(item);
        }

        if (createdCount > 0) {
            logger.info("Seeded {} catalog items for local/demo checkout flows.", createdCount);
        }
    }

    private int seedItemIfMissing(SeedItem item) {
        Integer existingCount = jdbcTemplate.queryForObject(
                "select count(*) from items where item_id = ?",
                Integer.class,
                item.itemId());

        if (existingCount != null && existingCount > 0) {
            return 0;
        }

        jdbcTemplate.update(
                "insert into items (id, item_id, name, description, price, category, stock_count, available, image_url, created_at, updated_at) " +
                        "values (?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)",
                UUID.randomUUID().toString(),
                item.itemId(),
                item.name(),
                item.description(),
                item.price(),
                item.category(),
                item.stockCount(),
                item.stockCount() > 0,
                item.imageUrl());

        return 1;
    }

    private record SeedItem(
            String itemId,
            String name,
            String description,
            BigDecimal price,
            String category,
            int stockCount,
            String imageUrl) {
    }
}