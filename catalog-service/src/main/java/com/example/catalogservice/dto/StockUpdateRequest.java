package com.example.catalogservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockUpdateRequest {
    private Integer stockCount;   // set absolute stock
    private Integer delta;        // +/- adjustment (used for decrement)
}
