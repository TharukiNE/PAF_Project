package com.sliit.smartcampus.hateoas;

import com.sliit.smartcampus.controller.CampusResourceController;
import com.sliit.smartcampus.dto.HateoasLink;
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
        // Populate _links in the response DTO for explicit HATEOAS support
        var selfLink = linkTo(methodOn(CampusResourceController.class).get(r.id())).toUri();
        var resourcesLink = linkTo(methodOn(CampusResourceController.class).list()).toUri();
        var updateLink = linkTo(CampusResourceController.class).slash(r.id()).toUri();
        
        r.links().put("self", new HateoasLink(selfLink.toString(), "GET", "Get Resource Details"));
        r.links().put("resources", new HateoasLink(resourcesLink.toString(), "GET", "List All Resources"));
        r.links().put("update", new HateoasLink(updateLink.toString(), "PUT", "Update Resource"));
        r.links().put("delete", new HateoasLink(updateLink.toString(), "DELETE", "Delete Resource"));
        
        // Also add Spring HATEOAS links to EntityModel for REST clients that use them
        EntityModel<ResourceResponse> model = EntityModel.of(r);
        model.add(linkTo(methodOn(CampusResourceController.class).get(r.id())).withSelfRel());
        model.add(linkTo(methodOn(CampusResourceController.class).list()).withRel("resources"));
        model.add(linkTo(CampusResourceController.class).slash(r.id()).withRel("update"));
        return model;
    }
}
