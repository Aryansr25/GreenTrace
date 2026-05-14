package com.greenTrace.Emission.Repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.greenTrace.Emission.Model.EmissionAudit;

public interface EmissionRepository extends MongoRepository<EmissionAudit, String> {
	
	Optional<EmissionAudit> findByShipmentId(Long shipmentId);
}
