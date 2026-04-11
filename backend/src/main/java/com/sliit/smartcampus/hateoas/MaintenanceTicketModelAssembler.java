package com.sliit.smartcampus.hateoas;

import com.sliit.smartcampus.controller.MaintenanceController;
import com.sliit.smartcampus.dto.HateoasLink;
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
        String id = t.id();
        
        // Populate _links in the response DTO for explicit HATEOAS support
        var selfLink = linkTo(methodOn(MaintenanceController.class).get(id)).toUri();
        var ticketsLink = linkTo(methodOn(MaintenanceController.class).list()).toUri();
        var imagesLink = linkTo(MaintenanceController.class).slash(id).slash("images").toUri();
        var commentsLink = linkTo(MaintenanceController.class).slash(id).slash("comments").toUri();
        var resolutionLink = linkTo(MaintenanceController.class).slash(id).slash("resolution").toUri();
        var technicianLink = linkTo(MaintenanceController.class).slash(id).slash("technician").toUri();
        
        t.links().put("self", new HateoasLink(selfLink.toString(), "GET", "Get Ticket Details"));
        t.links().put("tickets", new HateoasLink(ticketsLink.toString(), "GET", "List All Tickets"));
        t.links().put("images", new HateoasLink(imagesLink.toString(), "GET", "View Images"));
        t.links().put("comments", new HateoasLink(commentsLink.toString(), "GET", "View Comments"));
        t.links().put("resolution", new HateoasLink(resolutionLink.toString(), "PUT", "Resolve Ticket"));
        t.links().put("technician", new HateoasLink(technicianLink.toString(), "PUT", "Assign Technician"));
        
        if (t.status() == TicketStatus.RESOLVED || t.status() == TicketStatus.CLOSED) {
            var reopenLink = linkTo(MaintenanceController.class).slash(id).slash("reopen").toUri();
            t.links().put("reopen", new HateoasLink(reopenLink.toString(), "POST", "Reopen Ticket"));
        }
        
        // Also add Spring HATEOAS links to EntityModel for REST clients that use them
        EntityModel<TicketResponse> model = EntityModel.of(t);
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
