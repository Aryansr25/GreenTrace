package com.greenTrace.Emission.Service;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.greenTrace.Emission.Model.EmissionAudit;
import com.greenTrace.Emission.Repository.EmissionRepository;
import com.greenTrace.Emission.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmissionService {
	 private static final Logger log = LoggerFactory.getLogger(EmissionService.class);
	private final EmissionRepository repository;

    public void recordAudit(EmissionAudit audit) {
    	log.info("Recording emission audit for shipmentId={}", audit.getShipmentId());
        audit.setTimestamp(LocalDateTime.now());
        repository.save(audit);
    }
    
    public EmissionAudit getAuditByShipmentId(Long shipmentId) {
    	 log.info("Fetching emission audit for shipmentId={}", shipmentId);
        return repository.findByShipmentId(shipmentId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Audit record not found for Shipment ID: " + shipmentId
                )
            );
    }
}
