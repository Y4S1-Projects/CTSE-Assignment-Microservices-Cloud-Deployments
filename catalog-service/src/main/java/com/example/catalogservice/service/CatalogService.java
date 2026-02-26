package com.example.catalogservice.service;

import com.example.catalogservice.dto.MenuItemResponse;
import java.util.List;

/**
 * CatalogService interface
 * Implementation will be added during feature development
 */
public interface CatalogService {
    List<MenuItemResponse> getAllItems();
    MenuItemResponse getItemById(String itemId);
    List<MenuItemResponse> getItemsByCategory(String category);
    MenuItemResponse updateItemAvailability(String itemId, String availability);
}
