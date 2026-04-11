package com.sliit.smartcampus.hateoas;

import com.sliit.smartcampus.controller.NotificationController;
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
        EntityModel<NotificationResponse> model = EntityModel.of(n);
        model.add(linkTo(NotificationController.class).slash(n.id()).withSelfRel());
        model.add(linkTo(methodOn(NotificationController.class).list()).withRel("notifications"));
        model.add(linkTo(methodOn(NotificationController.class).markRead(n.id())).withRel("mark-read"));
        model.add(linkTo(methodOn(NotificationController.class).delete(n.id())).withRel("delete"));
        return model;
    }
}
