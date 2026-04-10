package com.sliit.smartcampus.service;

import com.sliit.smartcampus.dto.resource.ResourceRequest;
import com.sliit.smartcampus.dto.resource.ResourceResponse;
import com.sliit.smartcampus.entity.CampusResource;
import com.sliit.smartcampus.entity.enums.ResourceStatus;
import com.sliit.smartcampus.exception.ApiException;
import com.sliit.smartcampus.repository.CampusResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CampusResourceService {

    private final CampusResourceRepository campusResourceRepository;

    public List<ResourceResponse> findAll() {
        return campusResourceRepository.findAll().stream().map(ResourceResponse::from).toList();
    }

    public ResourceResponse findById(String id) {
        return ResourceResponse.from(getEntity(id));
    }

    public ResourceResponse create(ResourceRequest req) {
        CampusResource r = CampusResource.builder()
                .name(req.name() != null ? req.name() : "")
                .type(req.type() != null ? req.type() : com.sliit.smartcampus.entity.enums.ResourceType.LECTURE_HALL)
                .capacity(req.capacity())
                .location(req.location())
                .floor(req.floor())
                .amenities(req.amenities() != null ? req.amenities() : new java.util.ArrayList<>())
                .status(req.status() != null ? req.status() : ResourceStatus.ACTIVE)
                .build();
        r.touchTimestamps();
        return ResourceResponse.from(campusResourceRepository.save(r));
    }

    public ResourceResponse update(String id, ResourceRequest req) {
        CampusResource r = getEntity(id);
        if (req.name() != null) {
            r.setName(req.name());
        }
        if (req.type() != null) {
            r.setType(req.type());
        }
        r.setCapacity(req.capacity());
        r.setLocation(req.location());
        r.setFloor(req.floor());
        if (req.amenities() != null) {
            r.setAmenities(req.amenities());
        }
        if (req.status() != null) {
            r.setStatus(req.status());
        }
        r.touchTimestamps();
        return ResourceResponse.from(campusResourceRepository.save(r));
    }

    public void delete(String id) {
        if (!campusResourceRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Resource not found");
        }
        campusResourceRepository.deleteById(id);
    }

    public CampusResource getEntity(String id) {
        return campusResourceRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Resource not found"));
    }
}
