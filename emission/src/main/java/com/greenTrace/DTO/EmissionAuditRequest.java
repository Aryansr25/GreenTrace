package com.greenTrace.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmissionAuditRequest {
	@NotNull(message = "Shipment ID cannot be null")
	private Long shipmentId;
	@NotNull(message = "Carbon value is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total carbon cannot be negative")
    private Double totalCarbon;
	@NotNull(message = "Transport type is mandatory")
    private String transportType;
}
