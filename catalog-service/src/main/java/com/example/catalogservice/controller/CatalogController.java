package com.example.catalogservice.controller;

import com.example.catalogservice.dto.*;
import com.example.catalogservice.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * NOTE: context-path is /catalog (application.properties)
 * So controller paths below are RELATIVE to /catalog.
 * External URL:  /catalog/items  →  controller sees  /items
 */
@RestController
@Tag(name = "Catalog", description = "Catalog & inventory management endpoints")
public class CatalogController {

    @Autowired
    private CatalogService catalogService;

    // ── item listing ─────────────────────────────────────────────────────────

    @GetMapping("/items")
    @Operation(summary = "Get all catalog items")
    @ApiResponse(responseCode = "200", description = "List returned")
    public ResponseEntity<List<MenuItemResponse>> getAllItems() {
        return ResponseEntity.ok(catalogService.getAllItems());
    }

    @GetMapping("/items/{id}")
    @Operation(summary = "Get item by DB id")
    @ApiResponse(responseCode = "200", description = "Item found")
    @ApiResponse(responseCode = "404", description = "Not found")
    public ResponseEntity<MenuItemResponse> getItemById(@PathVariable String id) {
        return ResponseEntity.ok(catalogService.getItemById(id));
    }

    @GetMapping("/items/by-item-id/{itemId}")
    @Operation(summary = "Get item by business itemId")
    public ResponseEntity<MenuItemResponse> getItemByItemId(@PathVariable String itemId) {
        return ResponseEntity.ok(catalogService.getItemByItemId(itemId));
    }

    @GetMapping("/items/category/{category}")
    @Operation(summary = "Get items by category")
    public ResponseEntity<List<MenuItemResponse>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(catalogService.getItemsByCategory(category));
    }

    @GetMapping("/categories")
    @Operation(summary = "List all distinct categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(catalogService.getCategories());
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @PostMapping("/items")
    @Operation(summary = "Create a new catalog item (admin)")
    @ApiResponse(responseCode = "200", description = "Item created")
    public ResponseEntity<MenuItemResponse> createItem(@RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(catalogService.createItem(request));
    }

    @PutMapping("/items/{id}")
    @Operation(summary = "Update catalog item (admin)")
    public ResponseEntity<MenuItemResponse> updateItem(
            @PathVariable String id,
            @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(catalogService.updateItem(id, request));
    }

    @DeleteMapping("/items/{id}")
    @Operation(summary = "Delete catalog item (admin)")
    @ApiResponse(responseCode = "204", description = "Deleted")
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        catalogService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    // ── stock management ─────────────────────────────────────────────────────

    @PatchMapping("/items/{id}/stock")
    @Operation(summary = "Set or adjust stock count (admin)")
    public ResponseEntity<MenuItemResponse> updateStock(
            @PathVariable String id,
            @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(catalogService.updateStock(id, request));
    }

    /**
     * Called by payment-service after successful checkout.
     * Uses business itemId (e.g. "ITEM-1234") not DB UUID.
     */
    @PostMapping("/items/{itemId}/decrement-stock")
    @Operation(summary = "Decrement stock on checkout (internal)")
    public ResponseEntity<MenuItemResponse> decrementStock(
            @PathVariable String itemId,
            @RequestParam(defaultValue = "1") int quantity) {
        return ResponseEntity.ok(catalogService.decrementStock(itemId, quantity));
    }

    // ── dashboard ────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @Operation(summary = "Get catalog dashboard stats (admin)")
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(catalogService.getDashboardStats());
    }
}
