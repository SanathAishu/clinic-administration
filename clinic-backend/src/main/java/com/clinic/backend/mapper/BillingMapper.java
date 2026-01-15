package com.clinic.backend.mapper;

import com.clinic.common.dto.request.CreateBillingRequest;
import com.clinic.common.dto.response.BillingResponse;
import com.clinic.common.entity.operational.Billing;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for Billing entity to/from DTOs.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BillingMapper {

    /**
     * Convert CreateBillingRequest to Billing entity.
     * Note: patient and appointment relationships must be set separately.
     */
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "appointment", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    Billing toEntity(CreateBillingRequest request);

    /**
     * Convert Billing entity to BillingResponse.
     */
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "appointmentId", source = "appointment.id")
    @Mapping(target = "paymentStatus", expression = "java(billing.getPaymentStatus() != null ? billing.getPaymentStatus().name() : null)")
    @Mapping(target = "paymentMethod", expression = "java(billing.getPaymentMethod() != null ? billing.getPaymentMethod().name() : null)")
    BillingResponse toResponse(Billing billing);

    /**
     * Convert list of Billing entities to list of BillingResponses.
     */
    List<BillingResponse> toResponseList(List<Billing> billings);
}
