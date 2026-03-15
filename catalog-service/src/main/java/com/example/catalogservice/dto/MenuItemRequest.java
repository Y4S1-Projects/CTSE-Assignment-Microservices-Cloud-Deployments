package com.example.catalogservice.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemRequest {
    private String itemId;       // optional; auto-generated if blank
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private Integer stockCount;
    private String imageUrl;
}
