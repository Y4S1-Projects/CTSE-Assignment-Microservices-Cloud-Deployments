package com.example.catalogservice.service;

import com.example.catalogservice.dto.*;
import com.example.catalogservice.entity.MenuItem;
import com.example.catalogservice.exception.DuplicateItemException;
import com.example.catalogservice.exception.ItemNotFoundException;
import com.example.catalogservice.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CatalogServiceImpl implements CatalogService {
    private static final Logger logger = LoggerFactory.getLogger(CatalogServiceImpl.class);

    @Autowired
    private MenuItemRepository menuItemRepository;

    // ── helpers ──────────────────────────────────────────────────────────────

    private MenuItemResponse toResponse(MenuItem item) {
        return MenuItemResponse.builder()
                .id(item.getId())
                .itemId(item.getItemId())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .category(item.getCategory())
                .stockCount(item.getStockCount())
                .available(item.getAvailable())
                .imageUrl(item.getImageUrl())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private String generateItemId() {
        String id;
        int attempts = 0;
        do {
            id = "ITEM-" + String.format("%04d", (int)(Math.random() * 9000) + 1000);
            attempts++;
        } while (menuItemRepository.existsByItemId(id) && attempts < 20);
        return id;
    }

    // ── read operations ───────────────────────────────────────────────────────

    @Override
    public List<MenuItemResponse> getAllItems() {
        logger.info("Fetching all catalog items");
        return menuItemRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public MenuItemResponse getItemById(String id) {
        logger.info("Fetching item by DB id: {}", id);
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with id: " + id));
        return toResponse(item);
    }

    @Override
    public MenuItemResponse getItemByItemId(String itemId) {
        logger.info("Fetching item by itemId: {}", itemId);
        MenuItem item = menuItemRepository.findByItemId(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with itemId: " + itemId));
        return toResponse(item);
    }

    @Override
    public List<MenuItemResponse> getItemsByCategory(String category) {
        logger.info("Fetching items for category: {}", category);
        return menuItemRepository.findByCategory(category).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── write operations ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public MenuItemResponse createItem(MenuItemRequest request) {
        String itemId = (request.getItemId() != null && !request.getItemId().isBlank())
                ? request.getItemId()
                : generateItemId();

        if (menuItemRepository.existsByItemId(itemId)) {
            throw new DuplicateItemException("Item ID already exists: " + itemId);
        }

        MenuItem item = MenuItem.builder()
                .itemId(itemId)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .stockCount(request.getStockCount() != null ? request.getStockCount() : 0)
                .available(request.getStockCount() != null && request.getStockCount() > 0)
                .imageUrl(request.getImageUrl())
                .build();

        MenuItem saved = menuItemRepository.save(item);
        logger.info("Created item: {} ({})", saved.getName(), saved.getItemId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public MenuItemResponse updateItem(String id, MenuItemRequest request) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with id: " + id));

        if (request.getName() != null)        item.setName(request.getName());
        if (request.getDescription() != null)  item.setDescription(request.getDescription());
        if (request.getPrice() != null)        item.setPrice(request.getPrice());
        if (request.getCategory() != null)     item.setCategory(request.getCategory());
        if (request.getImageUrl() != null)     item.setImageUrl(request.getImageUrl());
        if (request.getStockCount() != null) {
            item.setStockCount(request.getStockCount());
            item.setAvailable(request.getStockCount() > 0);
        }

        return toResponse(menuItemRepository.save(item));
    }

    @Override
    @Transactional
    public void deleteItem(String id) {
        if (!menuItemRepository.existsById(id)) {
            throw new ItemNotFoundException("Item not found with id: " + id);
        }
        menuItemRepository.deleteById(id);
        logger.info("Deleted item: {}", id);
    }

    // ── stock operations ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public MenuItemResponse updateStock(String id, StockUpdateRequest request) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with id: " + id));

        if (request.getStockCount() != null) {
            item.setStockCount(request.getStockCount());
        } else if (request.getDelta() != null) {
            int newStock = item.getStockCount() + request.getDelta();
            item.setStockCount(Math.max(0, newStock));
        }
        item.setAvailable(item.getStockCount() > 0);
        return toResponse(menuItemRepository.save(item));
    }

    @Override
    @Transactional
    public MenuItemResponse decrementStock(String itemId, int quantity) {
        MenuItem item = menuItemRepository.findByItemId(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with itemId: " + itemId));

        int newStock = item.getStockCount() - quantity;
        if (newStock < 0) {
            logger.warn("Stock would go negative for item {}; clamping to 0", itemId);
            newStock = 0;
        }
        item.setStockCount(newStock);
        item.setAvailable(newStock > 0);
        MenuItem saved = menuItemRepository.save(item);
        logger.info("Decremented stock for {} by {} → new stock: {}", itemId, quantity, newStock);
        return toResponse(saved);
    }

    // ── category / dashboard ──────────────────────────────────────────────────

    @Override
    public List<String> getCategories() {
        return menuItemRepository.findDistinctCategories();
    }

    @Override
    public DashboardResponse getDashboardStats() {
        long total      = menuItemRepository.count();
        long available  = menuItemRepository.countAvailable();
        long outOfStock = menuItemRepository.countOutOfStock();
        long lowStock   = menuItemRepository.countLowStock();

        List<String> categories = menuItemRepository.findDistinctCategories();

        List<DashboardResponse.CategoryStat> catStats = categories.stream().map(cat -> {
            List<MenuItem> items = menuItemRepository.findByCategory(cat);
            long totalStock = items.stream().mapToLong(MenuItem::getStockCount).sum();
            return DashboardResponse.CategoryStat.builder()
                    .category(cat)
                    .count(items.size())
                    .totalStock(totalStock)
                    .build();
        }).collect(Collectors.toList());

        return DashboardResponse.builder()
                .totalItems(total)
                .availableItems(available)
                .outOfStockItems(outOfStock)
                .lowStockItems(lowStock)
                .categories(categories)
                .categoryStats(catStats)
                .build();
    }
}
