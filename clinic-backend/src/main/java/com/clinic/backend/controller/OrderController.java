package com.clinic.backend.controller;

import com.clinic.backend.dto.order.CreateOrderRequest;
import com.clinic.backend.dto.order.OrderDTO;
import com.clinic.backend.dto.order.UpdateOrderRequest;
import com.clinic.backend.mapper.OrderMapper;
import com.clinic.backend.service.OrderService;
import com.clinic.common.entity.operational.MedicalOrder;
import com.clinic.common.enums.OrderStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/orders")
@RequiredArgsConstructor
@PreAuthorize("@tenantValidator.isValidTenant(#tenantId)")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<OrderDTO> createOrder(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CreateOrderRequest request) {
        log.info("Creating medical order for tenant: {}", tenantId);
        MedicalOrder order = orderService.createOrder(
            orderMapper.toEntity(request), tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(orderMapper.toDTO(order));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDTO> getOrderById(
            @PathVariable UUID tenantId,
            @PathVariable UUID orderId) {
        log.debug("Getting order: {} for tenant: {}", orderId, tenantId);
        MedicalOrder order = orderService.getOrderById(orderId, tenantId);
        return ResponseEntity.ok(orderMapper.toDTO(order));
    }

    @GetMapping
    public ResponseEntity<Page<OrderDTO>> getAllOrders(
            @PathVariable UUID tenantId,
            Pageable pageable) {
        log.debug("Getting all orders for tenant: {}", tenantId);
        Page<MedicalOrder> orders = orderService.getAllOrders(tenantId, pageable);
        return ResponseEntity.ok(
            orders.map(orderMapper::toDTO));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<Page<OrderDTO>> getOrdersByPatient(
            @PathVariable UUID tenantId,
            @PathVariable UUID patientId,
            Pageable pageable) {
        log.debug("Getting orders for patient: {} in tenant: {}", patientId, tenantId);
        Page<MedicalOrder> orders = orderService.getOrdersByPatient(patientId, tenantId, pageable);
        return ResponseEntity.ok(
            orders.map(orderMapper::toDTO));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<OrderDTO>> getOrdersByStatus(
            @PathVariable UUID tenantId,
            @PathVariable OrderStatus status,
            Pageable pageable) {
        log.debug("Getting orders with status: {} for tenant: {}", status, tenantId);
        Page<MedicalOrder> orders = orderService.getOrdersByStatusPaged(tenantId, status, pageable);
        return ResponseEntity.ok(
            orders.map(orderMapper::toDTO));
    }

    @GetMapping("/search/product")
    public ResponseEntity<Page<OrderDTO>> searchByProductName(
            @PathVariable UUID tenantId,
            @RequestParam String productName,
            Pageable pageable) {
        log.debug("Searching orders by product: {} for tenant: {}", productName, tenantId);
        Page<MedicalOrder> orders = orderService.searchByProductName(tenantId, productName, pageable);
        return ResponseEntity.ok(
            orders.map(orderMapper::toDTO));
    }

    @GetMapping("/search/patient")
    public ResponseEntity<Page<OrderDTO>> searchByPatientNameOrPhone(
            @PathVariable UUID tenantId,
            @RequestParam String search,
            Pageable pageable) {
        log.debug("Searching orders by patient: {} for tenant: {}", search, tenantId);
        Page<MedicalOrder> orders = orderService.searchByPatientNameOrPhone(tenantId, search, pageable);
        return ResponseEntity.ok(
            orders.map(orderMapper::toDTO));
    }

    @GetMapping("/ready-for-pickup")
    public ResponseEntity<Page<OrderDTO>> getOrdersReadyForPickup(
            @PathVariable UUID tenantId,
            Pageable pageable) {
        log.debug("Getting orders ready for pickup for tenant: {}", tenantId);
        var orders = orderService.getOrdersReadyForPickupNotNotified(tenantId);
        var dtos = orderMapper.toDTOList(orders);
        return ResponseEntity.ok(
            new org.springframework.data.domain.PageImpl<>(dtos, pageable, dtos.size()));
    }

    @GetMapping("/overdue")
    public ResponseEntity<Page<OrderDTO>> getOverdueOrders(
            @PathVariable UUID tenantId,
            Pageable pageable) {
        log.debug("Getting overdue orders for tenant: {}", tenantId);
        var orders = orderService.getOverdueOrders(tenantId);
        var dtos = orderMapper.toDTOList(orders);
        return ResponseEntity.ok(
            new org.springframework.data.domain.PageImpl<>(dtos, pageable, dtos.size()));
    }

    @PutMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<OrderDTO> updateOrder(
            @PathVariable UUID tenantId,
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderRequest request) {
        log.info("Updating order: {} for tenant: {}", orderId, tenantId);
        MedicalOrder order = orderService.getOrderById(orderId, tenantId);
        orderMapper.updateEntityFromRequest(request, order);
        MedicalOrder updated = orderService.updateOrder(orderId, tenantId, order);
        return ResponseEntity.ok(orderMapper.toDTO(updated));
    }

    @PatchMapping("/{orderId}/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<OrderDTO> sendOrder(
            @PathVariable UUID tenantId,
            @PathVariable UUID orderId) {
        log.info("Sending order: {} for tenant: {}", orderId, tenantId);
        MedicalOrder order = orderService.sendOrder(orderId, tenantId);
        return ResponseEntity.ok(orderMapper.toDTO(order));
    }

    @PatchMapping("/{orderId}/in-production")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<OrderDTO> markAsInProduction(
            @PathVariable UUID tenantId,
            @PathVariable UUID orderId) {
        log.info("Marking order as in production: {} for tenant: {}", orderId, tenantId);
        MedicalOrder order = orderService.markAsInProduction(orderId, tenantId);
        return ResponseEntity.ok(orderMapper.toDTO(order));
    }

    @PatchMapping("/{orderId}/shipped")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<OrderDTO> markAsShipped(
            @PathVariable UUID tenantId,
            @PathVariable UUID orderId,
            @RequestParam String trackingNumber) {
        log.info("Marking order as shipped with tracking: {} for tenant: {}", orderId, tenantId);
        MedicalOrder order = orderService.markAsShipped(orderId, tenantId, trackingNumber);
        return ResponseEntity.ok(orderMapper.toDTO(order));
    }

    @PatchMapping("/{orderId}/received")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<OrderDTO> markAsReceived(
            @PathVariable UUID tenantId,
            @PathVariable UUID orderId) {
        log.info("Marking order as received: {} for tenant: {}", orderId, tenantId);
        MedicalOrder order = orderService.markAsReceived(orderId, tenantId);
        return ResponseEntity.ok(orderMapper.toDTO(order));
    }

    @PatchMapping("/{orderId}/ready-for-pickup")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<OrderDTO> markAsReadyForPickup(
            @PathVariable UUID tenantId,
            @PathVariable UUID orderId) {
        log.info("Marking order as ready for pickup: {} for tenant: {}", orderId, tenantId);
        MedicalOrder order = orderService.markAsReadyForPickup(orderId, tenantId);
        return ResponseEntity.ok(orderMapper.toDTO(order));
    }

    @PatchMapping("/{orderId}/delivered")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<OrderDTO> markAsDelivered(
            @PathVariable UUID tenantId,
            @PathVariable UUID orderId) {
        log.info("Marking order as delivered: {} for tenant: {}", orderId, tenantId);
        MedicalOrder order = orderService.markAsDelivered(orderId, tenantId);
        return ResponseEntity.ok(orderMapper.toDTO(order));
    }

    @PatchMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<OrderDTO> cancelOrder(
            @PathVariable UUID tenantId,
            @PathVariable UUID orderId) {
        log.info("Cancelling order: {} for tenant: {}", orderId, tenantId);
        MedicalOrder order = orderService.cancelOrder(orderId, tenantId);
        return ResponseEntity.ok(orderMapper.toDTO(order));
    }

    @PatchMapping("/{orderId}/notify-patient")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<Void> markPatientNotified(
            @PathVariable UUID tenantId,
            @PathVariable UUID orderId) {
        log.info("Marking patient notified for order: {} in tenant: {}", orderId, tenantId);
        orderService.markPatientNotified(orderId, tenantId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable UUID tenantId,
            @PathVariable UUID orderId) {
        log.info("Deleting order: {} for tenant: {}", orderId, tenantId);
        orderService.softDeleteOrder(orderId, tenantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countOrders(
            @PathVariable UUID tenantId) {
        log.debug("Counting orders for tenant: {}", tenantId);
        long count = orderService.countOrders(tenantId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/status/{status}")
    public ResponseEntity<Long> countOrdersByStatus(
            @PathVariable UUID tenantId,
            @PathVariable OrderStatus status) {
        log.debug("Counting orders with status: {} for tenant: {}", status, tenantId);
        long count = orderService.countOrdersByStatus(tenantId, status);
        return ResponseEntity.ok(count);
    }
}
