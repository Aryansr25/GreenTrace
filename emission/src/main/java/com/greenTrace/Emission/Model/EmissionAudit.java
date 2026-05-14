package com.greenTrace.Emission.Model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "emissions_audit")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmissionAudit {
	@Id
	private String id;
	private Long shipmentId;
    private Double totalCarbon;
    private String transportType;
    private LocalDateTime timestamp;
}
