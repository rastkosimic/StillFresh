package com.stillfresh.app.orderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stillfresh.app.orderservice.model.Order;

public interface OrderRepository extends JpaRepository<Order, Integer> {
}
