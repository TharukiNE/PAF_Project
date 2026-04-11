package com.sliit.smartcampus.hateoas;

import com.sliit.smartcampus.controller.MaintenanceController;
import com.sliit.smartcampus.dto.maintenance.TicketCommentResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class TicketCommentModelAssembler {

    @NonNull
    public EntityModel<TicketCommentResponse> toModel(@NonNull TicketCommentResponse c) {
        EntityModel<TicketCommentResponse> model = EntityModel.of(c);
        String tid = c.ticketId();
        model.add(linkTo(MaintenanceController.class).slash("comments").slash(c.id()).withSelfRel());
        model.add(linkTo(methodOn(MaintenanceController.class).get(tid)).withRel("ticket"));
        model.add(linkTo(methodOn(MaintenanceController.class).deleteComment(c.id())).withRel("delete"));
        return model;
    }
}
