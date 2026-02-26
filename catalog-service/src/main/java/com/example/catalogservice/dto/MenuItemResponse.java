package com.example.catalogservice.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemResponse {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String availability;
    private String category;
    private String imageUrl;
}
