package com.clinic.mgmt.appointment.repository;

import com.clinic.mgmt.appointment.domain.Appointment;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AppointmentRepository extends MongoRepository<Appointment, String> {
}
