package com.sliit.smartcampus.hateoas;

import com.sliit.smartcampus.controller.MaintenanceController;
import com.sliit.smartcampus.dto.maintenance.TicketImageResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class TicketImageModelAssembler {

    @NonNull
    public EntityModel<TicketImageResponse> toModel(@NonNull TicketImageResponse img, @NonNull String ticketId) {
        EntityModel<TicketImageResponse> model = EntityModel.of(img);
        model.add(linkTo(methodOn(MaintenanceController.class).downloadImage(img.id())).withRel("download"));
        model.add(linkTo(methodOn(MaintenanceController.class).get(ticketId)).withRel("ticket"));
        model.add(linkTo(MaintenanceController.class).slash(ticketId).slash("images").withRel("images"));
        return model;
    }
}
