package com.sliit.smartcampus.repository;

import com.sliit.smartcampus.entity.MaintenanceTicket;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MaintenanceTicketRepository extends MongoRepository<MaintenanceTicket, String> {

    List<MaintenanceTicket> findAllByOrderByUpdatedAtDesc();

    @Query("{ 'images.id': ?0 }")
    Optional<MaintenanceTicket> findByAnyImageId(String imageId);
}
