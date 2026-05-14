package com.greenTrace.Emission.Controller;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.greenTrace.DTO.EmissionAuditRequest;
import com.greenTrace.Emission.Model.EmissionAudit;
import com.greenTrace.Emission.Service.EmissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/emissions")
@RequiredArgsConstructor
public class EmissionController {
	private static final Logger log = LoggerFactory.getLogger(EmissionController.class);

    private final EmissionService emissionService;

    @Operation(summary = "Create emission audit log")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Audit logged successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<String> logAudit(
            @Valid @RequestBody EmissionAuditRequest request) {
    	log.info("POST /emissions - shipmentId={}, transportType={}",
                request.getShipmentId(),
                request.getTransportType());
        EmissionAudit audit = EmissionAudit.builder()
                .shipmentId(request.getShipmentId())
                .totalCarbon(request.getTotalCarbon())
                .transportType(request.getTransportType())
                .timestamp(LocalDateTime.now())
                .build();

        emissionService.recordAudit(audit);

        return ResponseEntity.ok("Audit logged to MongoDB successfully!");
    }

    @Operation(summary = "Get emission audit by shipment ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Audit found"),
        @ApiResponse(responseCode = "404", description = "Audit not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{shipmentId}")
    public ResponseEntity<EmissionAudit> getAuditByShipment(
            @Parameter(description = "Shipment ID", example = "5")
            @PathVariable Long shipmentId) {
    	log.info("GET /emissions/{} - fetch audit", shipmentId);
        return ResponseEntity.ok(
                emissionService.getAuditByShipmentId(shipmentId)
        );
    }
}