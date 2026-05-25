package com.tayota.operationservice.repository.car;

import com.tayota.operationservice.entity.car.Dealership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;

@Repository
public interface DealershipRepository extends JpaRepository<Dealership, UUID> {
    List<Dealership> findByIsActiveTrueOrderByNameAsc();

    Optional<Dealership> findByPlaceId(String placeId);
    List<Dealership> findAll(Sort sort);
}
