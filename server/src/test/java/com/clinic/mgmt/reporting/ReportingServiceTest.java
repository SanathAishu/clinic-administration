package com.clinic.mgmt.reporting;

import java.time.Instant;
import com.clinic.mgmt.common.exception.InvalidRequestException;
import com.clinic.mgmt.reporting.service.ReportingService;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ReportingServiceTest {

	private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
	private final ReportingService service = new ReportingService(mongoTemplate);

	@Test
	void dailySummaryRejectsInvalidDateRange() {
		Instant dateFrom = Instant.parse("2025-01-02T00:00:00Z");
		Instant dateTo = Instant.parse("2025-01-01T00:00:00Z");

		assertThrows(
				InvalidRequestException.class,
				() -> service.dailySummary("org-1", null, null, dateFrom, dateTo)
		);
	}

	@Test
	void revenueAnalysisRejectsUnknownGroupBy() {
		assertThrows(
				InvalidRequestException.class,
				() -> service.revenueAnalysis("org-1", null, null, null, null, "unknown")
		);
	}

	@Test
	void orderStatusRejectsInvalidDateRange() {
		Instant dateFrom = Instant.parse("2025-02-01T00:00:00Z");
		Instant dateTo = Instant.parse("2025-01-01T00:00:00Z");

		assertThrows(
				InvalidRequestException.class,
				() -> service.orderStatus("org-1", null, null, dateFrom, dateTo)
		);
	}
}
