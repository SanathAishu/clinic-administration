package com.clinic.backend.repository;

import com.clinic.backend.entity.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, UUID> {

    // Prescription items (One-to-Many relationship)
    List<PrescriptionItem> findByPrescriptionId(UUID prescriptionId);

    // Medication search
    @Query("SELECT pi FROM PrescriptionItem pi WHERE pi.prescription.tenantId = :tenantId AND " +
           "LOWER(pi.medicationName) LIKE LOWER(CONCAT('%', :medicationName, '%'))")
    List<PrescriptionItem> findByMedicationNameContaining(@Param("tenantId") UUID tenantId,
                                                           @Param("medicationName") String medicationName);

    // Patient medication history
    @Query("SELECT pi FROM PrescriptionItem pi WHERE pi.prescription.patient.id = :patientId AND " +
           "pi.prescription.tenantId = :tenantId AND pi.prescription.deletedAt IS NULL " +
           "ORDER BY pi.prescription.prescriptionDate DESC")
    List<PrescriptionItem> findPatientMedicationHistory(@Param("patientId") UUID patientId,
                                                         @Param("tenantId") UUID tenantId);

    // Specific medication for patient
    @Query("SELECT pi FROM PrescriptionItem pi WHERE pi.prescription.patient.id = :patientId AND " +
           "pi.prescription.tenantId = :tenantId AND LOWER(pi.medicationName) = LOWER(:medicationName) AND " +
           "pi.prescription.deletedAt IS NULL")
    List<PrescriptionItem> findPatientSpecificMedication(@Param("patientId") UUID patientId,
                                                          @Param("tenantId") UUID tenantId,
                                                          @Param("medicationName") String medicationName);

    // Count items per prescription
    long countByPrescriptionId(UUID prescriptionId);
}
