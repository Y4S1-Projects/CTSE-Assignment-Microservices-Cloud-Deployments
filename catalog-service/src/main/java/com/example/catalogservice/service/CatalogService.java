package com.example.catalogservice.service;

import com.example.catalogservice.dto.*;
import java.util.List;

public interface CatalogService {
    List<MenuItemResponse> getAllItems();
    MenuItemResponse getItemById(String id);
    MenuItemResponse getItemByItemId(String itemId);
    List<MenuItemResponse> getItemsByCategory(String category);
    MenuItemResponse createItem(MenuItemRequest request);
    MenuItemResponse updateItem(String id, MenuItemRequest request);
    void deleteItem(String id);
    MenuItemResponse updateStock(String id, StockUpdateRequest request);
    MenuItemResponse decrementStock(String itemId, int quantity);
    List<String> getCategories();
    DashboardResponse getDashboardStats();
}
