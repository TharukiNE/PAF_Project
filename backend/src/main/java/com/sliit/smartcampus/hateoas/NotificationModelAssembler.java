package com.sliit.smartcampus.hateoas;

import com.sliit.smartcampus.controller.NotificationController;
import com.sliit.smartcampus.dto.HateoasLink;
import com.sliit.smartcampus.dto.notification.NotificationResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class NotificationModelAssembler implements RepresentationModelAssembler<NotificationResponse, EntityModel<NotificationResponse>> {

    @Override
    @NonNull
    public EntityModel<NotificationResponse> toModel(@NonNull NotificationResponse n) {
        // Populate _links in the response DTO for explicit HATEOAS support
        var selfLink = linkTo(NotificationController.class).slash(n.id()).toUri();
        var notificationsLink = linkTo(methodOn(NotificationController.class).list()).toUri();
        var markReadLink = linkTo(methodOn(NotificationController.class).markRead(n.id())).toUri();
        var deleteLink = linkTo(methodOn(NotificationController.class).delete(n.id())).toUri();
        
        n.links().put("self", new HateoasLink(selfLink.toString(), "GET", "Get Notification"));
        n.links().put("notifications", new HateoasLink(notificationsLink.toString(), "GET", "List All Notifications"));
        n.links().put("mark-read", new HateoasLink(markReadLink.toString(), "PUT", "Mark as Read"));
        n.links().put("delete", new HateoasLink(deleteLink.toString(), "DELETE", "Delete Notification"));
        
        // Also add Spring HATEOAS links to EntityModel for REST clients that use them
        EntityModel<NotificationResponse> model = EntityModel.of(n);
        model.add(linkTo(NotificationController.class).slash(n.id()).withSelfRel());
        model.add(linkTo(methodOn(NotificationController.class).list()).withRel("notifications"));
        model.add(linkTo(methodOn(NotificationController.class).markRead(n.id())).withRel("mark-read"));
        model.add(linkTo(methodOn(NotificationController.class).delete(n.id())).withRel("delete"));
        return model;
    }
}
