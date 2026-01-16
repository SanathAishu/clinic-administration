package com.clinic.backend.repository;

import com.clinic.common.entity.operational.MedicalOrder;
import com.clinic.common.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<MedicalOrder, UUID> {

    // Tenant-scoped queries
    Page<MedicalOrder> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<MedicalOrder> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // Find orders by patient
    @Query("SELECT o FROM MedicalOrder o WHERE o.patient.id = :patientId AND " +
           "o.tenantId = :tenantId AND o.deletedAt IS NULL ORDER BY o.orderDate DESC")
    Page<MedicalOrder> findByPatientId(@Param("patientId") UUID patientId,
                                        @Param("tenantId") UUID tenantId,
                                        Pageable pageable);

    List<MedicalOrder> findByPatientIdAndTenantIdAndDeletedAtIsNull(UUID patientId, UUID tenantId);

    // Find by status
    List<MedicalOrder> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, OrderStatus status);

    Page<MedicalOrder> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId,
                                                                   OrderStatus status,
                                                                   Pageable pageable);

    // Find orders ready for pickup (to trigger SMS)
    @Query("SELECT o FROM MedicalOrder o WHERE o.tenantId = :tenantId AND " +
           "o.status = 'READY_FOR_PICKUP' AND o.patientNotifiedAt IS NULL AND o.deletedAt IS NULL")
    List<MedicalOrder> findOrdersReadyForPickupNotNotified(@Param("tenantId") UUID tenantId);

    // Find by date range
    @Query("SELECT o FROM MedicalOrder o WHERE o.tenantId = :tenantId AND " +
           "o.orderDate BETWEEN :startDate AND :endDate AND o.deletedAt IS NULL")
    List<MedicalOrder> findByDateRange(@Param("tenantId") UUID tenantId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    // Find overdue orders (expected delivery date passed, not yet received)
    @Query("SELECT o FROM MedicalOrder o WHERE o.tenantId = :tenantId AND " +
           "o.expectedDeliveryDate < :today AND " +
           "o.status NOT IN ('RECEIVED', 'READY_FOR_PICKUP', 'DELIVERED', 'CANCELLED') AND " +
           "o.deletedAt IS NULL")
    List<MedicalOrder> findOverdueOrders(@Param("tenantId") UUID tenantId,
                                          @Param("today") LocalDate today);

    // Search by product name
    @Query("SELECT o FROM MedicalOrder o WHERE o.tenantId = :tenantId AND " +
           "LOWER(o.productName) LIKE LOWER(CONCAT('%', :productName, '%')) AND " +
           "o.deletedAt IS NULL")
    Page<MedicalOrder> searchByProductName(@Param("tenantId") UUID tenantId,
                                            @Param("productName") String productName,
                                            Pageable pageable);

    // Search by patient name or phone
    @Query("SELECT o FROM MedicalOrder o WHERE o.tenantId = :tenantId AND " +
           "(LOWER(o.patient.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.patient.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "o.patient.phone LIKE CONCAT('%', :search, '%')) AND " +
           "o.deletedAt IS NULL")
    Page<MedicalOrder> searchByPatientNameOrPhone(@Param("tenantId") UUID tenantId,
                                                    @Param("search") String search,
                                                    Pageable pageable);

    // Counting
    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);

    long countByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, OrderStatus status);

    // Count pending orders for manufacturer
    @Query("SELECT COUNT(o) FROM MedicalOrder o WHERE o.tenantId = :tenantId AND " +
           "o.manufacturerName = :manufacturerName AND o.status = 'PENDING' AND o.deletedAt IS NULL")
    long countPendingOrdersForManufacturer(@Param("tenantId") UUID tenantId,
                                            @Param("manufacturerName") String manufacturerName);
}
