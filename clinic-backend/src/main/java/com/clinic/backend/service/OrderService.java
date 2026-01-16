package com.clinic.backend.service;

import com.clinic.common.entity.operational.MedicalOrder;
import com.clinic.backend.repository.OrderRepository;
import com.clinic.common.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    @CacheEvict(value = "orders", allEntries = true, condition = "#tenantId != null")
    public MedicalOrder createOrder(MedicalOrder order, UUID tenantId) {
        log.debug("Creating medical order for patient: {}", order.getPatient().getId());

        // Set tenant ID
        order.setTenantId(tenantId);

        // Set default order date if not provided
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDate.now());
        }

        // Set default status
        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.PENDING);
        }

        MedicalOrder saved = orderRepository.save(order);
        log.info("Created medical order: {} for patient: {}",
            saved.getId(), saved.getPatient().getId());
        return saved;
    }

    @Cacheable(value = "orders", key = "#id + '-' + #tenantId", unless = "#result == null")
    public MedicalOrder getOrderById(UUID id, UUID tenantId) {
        return orderRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    public Page<MedicalOrder> getAllOrders(UUID tenantId, Pageable pageable) {
        return orderRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
    }

    public Page<MedicalOrder> getOrdersByPatient(UUID patientId, UUID tenantId, Pageable pageable) {
        return orderRepository.findByPatientId(patientId, tenantId, pageable);
    }

    public List<MedicalOrder> getOrdersByStatus(UUID tenantId, OrderStatus status) {
        return orderRepository.findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, status);
    }

    public Page<MedicalOrder> getOrdersByStatusPaged(UUID tenantId, OrderStatus status, Pageable pageable) {
        return orderRepository.findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, status, pageable);
    }

    public List<MedicalOrder> getOrdersReadyForPickupNotNotified(UUID tenantId) {
        return orderRepository.findOrdersReadyForPickupNotNotified(tenantId);
    }

    public List<MedicalOrder> getOverdueOrders(UUID tenantId) {
        return orderRepository.findOverdueOrders(tenantId, LocalDate.now());
    }

    public Page<MedicalOrder> searchByProductName(UUID tenantId, String productName, Pageable pageable) {
        return orderRepository.searchByProductName(tenantId, productName, pageable);
    }

    public Page<MedicalOrder> searchByPatientNameOrPhone(UUID tenantId, String search, Pageable pageable) {
        return orderRepository.searchByPatientNameOrPhone(tenantId, search, pageable);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "orders", allEntries = true, condition = "#tenantId != null"),
        @CacheEvict(value = "orders", key = "#id + '-' + #tenantId")
    })
    public MedicalOrder updateOrder(UUID id, UUID tenantId, MedicalOrder updates) {
        MedicalOrder order = getOrderById(id, tenantId);

        // Only allow updates if not yet delivered
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update delivered or cancelled order");
        }

        // Update fields
        if (updates.getProductName() != null) {
            order.setProductName(updates.getProductName());
        }

        if (updates.getProductType() != null) {
            order.setProductType(updates.getProductType());
        }

        if (updates.getDescription() != null) {
            order.setDescription(updates.getDescription());
        }

        if (updates.getQuantity() != null) {
            order.setQuantity(updates.getQuantity());
        }

        if (updates.getUnitPrice() != null) {
            order.setUnitPrice(updates.getUnitPrice());
        }

        if (updates.getManufacturerName() != null) {
            order.setManufacturerName(updates.getManufacturerName());
        }

        if (updates.getManufacturerEmail() != null) {
            order.setManufacturerEmail(updates.getManufacturerEmail());
        }

        if (updates.getManufacturerPhone() != null) {
            order.setManufacturerPhone(updates.getManufacturerPhone());
        }

        if (updates.getExpectedDeliveryDate() != null) {
            order.setExpectedDeliveryDate(updates.getExpectedDeliveryDate());
        }

        if (updates.getSpecialInstructions() != null) {
            order.setSpecialInstructions(updates.getSpecialInstructions());
        }

        if (updates.getNotes() != null) {
            order.setNotes(updates.getNotes());
        }

        MedicalOrder saved = orderRepository.save(order);
        log.info("Updated order: {}", saved.getId());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "orders", allEntries = true, condition = "#tenantId != null")
    public MedicalOrder sendOrder(UUID id, UUID tenantId) {
        MedicalOrder order = getOrderById(id, tenantId);
        order.markAsSent();
        MedicalOrder saved = orderRepository.save(order);
        log.info("Sent order {} to manufacturer", id);
        return saved;
    }

    @Transactional
    @CacheEvict(value = "orders", allEntries = true, condition = "#tenantId != null")
    public MedicalOrder markAsInProduction(UUID id, UUID tenantId) {
        MedicalOrder order = getOrderById(id, tenantId);
        order.markAsInProduction();
        MedicalOrder saved = orderRepository.save(order);
        log.info("Marked order {} as in production", id);
        return saved;
    }

    @Transactional
    @CacheEvict(value = "orders", allEntries = true, condition = "#tenantId != null")
    public MedicalOrder markAsShipped(UUID id, UUID tenantId, String trackingNumber) {
        MedicalOrder order = getOrderById(id, tenantId);
        order.markAsShipped(trackingNumber);
        MedicalOrder saved = orderRepository.save(order);
        log.info("Marked order {} as shipped with tracking: {}", id, trackingNumber);
        return saved;
    }

    @Transactional
    @CacheEvict(value = "orders", allEntries = true, condition = "#tenantId != null")
    public MedicalOrder markAsReceived(UUID id, UUID tenantId) {
        MedicalOrder order = getOrderById(id, tenantId);
        order.markAsReceived();
        MedicalOrder saved = orderRepository.save(order);
        log.info("Marked order {} as received", id);
        return saved;
    }

    @Transactional
    @CacheEvict(value = "orders", allEntries = true, condition = "#tenantId != null")
    public MedicalOrder markAsReadyForPickup(UUID id, UUID tenantId) {
        MedicalOrder order = getOrderById(id, tenantId);
        order.markAsReadyForPickup();
        MedicalOrder saved = orderRepository.save(order);
        log.info("Marked order {} as ready for pickup", id);
        // Note: SMS notification should be triggered here or via scheduled job
        return saved;
    }

    @Transactional
    @CacheEvict(value = "orders", allEntries = true, condition = "#tenantId != null")
    public MedicalOrder markAsDelivered(UUID id, UUID tenantId) {
        MedicalOrder order = getOrderById(id, tenantId);
        order.markAsDelivered();
        MedicalOrder saved = orderRepository.save(order);
        log.info("Marked order {} as delivered to patient", id);
        return saved;
    }

    @Transactional
    @CacheEvict(value = "orders", allEntries = true, condition = "#tenantId != null")
    public MedicalOrder cancelOrder(UUID id, UUID tenantId) {
        MedicalOrder order = getOrderById(id, tenantId);
        order.cancel();
        MedicalOrder saved = orderRepository.save(order);
        log.info("Cancelled order: {}", id);
        return saved;
    }

    @Transactional
    @CacheEvict(value = "orders", allEntries = true, condition = "#tenantId != null")
    public void markPatientNotified(UUID id, UUID tenantId) {
        MedicalOrder order = getOrderById(id, tenantId);
        order.setPatientNotifiedAt(Instant.now());
        orderRepository.save(order);
        log.info("Marked patient notified for order: {}", id);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "orders", allEntries = true, condition = "#tenantId != null"),
        @CacheEvict(value = "orders", key = "#id + '-' + #tenantId")
    })
    public void softDeleteOrder(UUID id, UUID tenantId) {
        MedicalOrder order = getOrderById(id, tenantId);
        order.softDelete();
        orderRepository.save(order);
        log.info("Soft deleted order: {}", id);
    }

    public long countOrders(UUID tenantId) {
        return orderRepository.countByTenantIdAndDeletedAtIsNull(tenantId);
    }

    public long countOrdersByStatus(UUID tenantId, OrderStatus status) {
        return orderRepository.countByTenantIdAndStatusAndDeletedAtIsNull(tenantId, status);
    }

    public long countPendingOrdersForManufacturer(UUID tenantId, String manufacturerName) {
        return orderRepository.countPendingOrdersForManufacturer(tenantId, manufacturerName);
    }
}
