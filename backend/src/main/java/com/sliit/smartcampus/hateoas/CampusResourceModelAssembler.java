package com.sliit.smartcampus.hateoas;

import com.sliit.smartcampus.controller.CampusResourceController;
import com.sliit.smartcampus.dto.resource.ResourceResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class CampusResourceModelAssembler implements RepresentationModelAssembler<ResourceResponse, EntityModel<ResourceResponse>> {

    @Override
    @NonNull
    public EntityModel<ResourceResponse> toModel(@NonNull ResourceResponse r) {
        EntityModel<ResourceResponse> model = EntityModel.of(r);
        model.add(linkTo(methodOn(CampusResourceController.class).get(r.id())).withSelfRel());
        model.add(linkTo(methodOn(CampusResourceController.class).list()).withRel("resources"));
        model.add(linkTo(CampusResourceController.class).slash(r.id()).withRel("update"));
        return model;
    }
}
