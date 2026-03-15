package com.example.catalogservice.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private long totalItems;
    private long availableItems;
    private long outOfStockItems;
    private long lowStockItems;      // stock <= 5
    private List<String> categories;
    private List<CategoryStat> categoryStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStat {
        private String category;
        private long count;
        private long totalStock;
    }
}
