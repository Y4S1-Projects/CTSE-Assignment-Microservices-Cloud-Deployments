package com.example.catalogservice.repository;

import com.example.catalogservice.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, String> {
    List<MenuItem> findByCategory(String category);
    List<MenuItem> findByAvailable(Boolean available);
    Optional<MenuItem> findByItemId(String itemId);
    boolean existsByItemId(String itemId);

    @Query("SELECT DISTINCT m.category FROM MenuItem m")
    List<String> findDistinctCategories();

    @Query("SELECT COUNT(m) FROM MenuItem m WHERE m.available = true")
    long countAvailable();

    @Query("SELECT COUNT(m) FROM MenuItem m WHERE m.stockCount = 0")
    long countOutOfStock();

    @Query("SELECT COUNT(m) FROM MenuItem m WHERE m.stockCount > 0 AND m.stockCount <= 5")
    long countLowStock();
}
