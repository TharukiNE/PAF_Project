package com.sliit.smartcampus.controller;

import com.sliit.smartcampus.dto.resource.ResourceRequest;
import com.sliit.smartcampus.dto.resource.ResourceResponse;
import com.sliit.smartcampus.hateoas.CampusResourceModelAssembler;
import com.sliit.smartcampus.service.CampusResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class CampusResourceController {

    private final CampusResourceService campusResourceService;
    private final CampusResourceModelAssembler resourceAssembler;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CollectionModel<EntityModel<ResourceResponse>>> list() {
        List<ResourceResponse> raw = campusResourceService.findAll();
        List<EntityModel<ResourceResponse>> content = raw.stream()
                .map(resourceAssembler::toModel)
                .toList();
        CollectionModel<EntityModel<ResourceResponse>> body = CollectionModel.of(content,
                linkTo(methodOn(CampusResourceController.class).list()).withSelfRel());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(body);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntityModel<ResourceResponse>> get(@PathVariable String id) {
        ResourceResponse r = campusResourceService.findById(id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(resourceAssembler.toModel(r));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntityModel<ResourceResponse>> create(@Valid @RequestBody ResourceRequest request) {
        ResourceResponse saved = campusResourceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(resourceAssembler.toModel(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntityModel<ResourceResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody ResourceRequest request) {
        ResourceResponse saved = campusResourceService.update(id, request);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(resourceAssembler.toModel(saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        campusResourceService.delete(id);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
