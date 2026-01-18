package com.clinic.mgmt.audit.repository;

import com.clinic.mgmt.audit.domain.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
}
