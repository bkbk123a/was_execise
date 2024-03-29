package com.example.web.jpa.repository.product;

import com.example.web.jpa.entity.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

  Optional<Product> findByProductName(String productName);
}
