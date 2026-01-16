package com.clinic.backend.mapper;

import com.clinic.backend.dto.order.CreateOrderRequest;
import com.clinic.backend.dto.order.OrderDTO;
import com.clinic.backend.dto.order.UpdateOrderRequest;
import com.clinic.common.entity.operational.MedicalOrder;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface OrderMapper {

    @Mapping(target = "totalAmount", ignore = true)
    MedicalOrder toEntity(CreateOrderRequest request);

    @Mapping(source = "patient.id", target = "patientId")
    @Mapping(source = "patient.firstName", target = "patientName")
    OrderDTO toDTO(MedicalOrder order);

    List<OrderDTO> toDTOList(List<MedicalOrder> orders);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "orderDate", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "receivedAt", ignore = true)
    @Mapping(target = "deliveredAt", ignore = true)
    @Mapping(target = "actualDeliveryDate", ignore = true)
    @Mapping(target = "patientNotifiedAt", ignore = true)
    void updateEntityFromRequest(UpdateOrderRequest request, @MappingTarget MedicalOrder order);
}
