package com.sliit.smartcampus.hateoas;

import com.sliit.smartcampus.controller.MaintenanceController;
import com.sliit.smartcampus.dto.maintenance.TicketResponse;
import com.sliit.smartcampus.entity.enums.TicketStatus;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class MaintenanceTicketModelAssembler implements RepresentationModelAssembler<TicketResponse, EntityModel<TicketResponse>> {

    @Override
    @NonNull
    public EntityModel<TicketResponse> toModel(@NonNull TicketResponse t) {
        EntityModel<TicketResponse> model = EntityModel.of(t);
        String id = t.id();
        model.add(linkTo(methodOn(MaintenanceController.class).get(id)).withSelfRel());
        model.add(linkTo(methodOn(MaintenanceController.class).list()).withRel("tickets"));
        model.add(linkTo(MaintenanceController.class).slash(id).slash("images").withRel("images"));
        model.add(linkTo(MaintenanceController.class).slash(id).slash("comments").withRel("comments"));
        model.add(linkTo(MaintenanceController.class).slash(id).slash("resolution").withRel("resolution"));
        model.add(linkTo(MaintenanceController.class).slash(id).slash("technician").withRel("technician"));
        if (t.status() == TicketStatus.RESOLVED || t.status() == TicketStatus.CLOSED) {
            model.add(linkTo(MaintenanceController.class).slash(id).slash("reopen").withRel("reopen"));
        }
        return model;
    }
}
