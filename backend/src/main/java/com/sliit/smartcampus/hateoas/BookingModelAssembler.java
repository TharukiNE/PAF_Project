package com.sliit.smartcampus.hateoas;

import com.sliit.smartcampus.controller.BookingController;
import com.sliit.smartcampus.dto.HateoasLink;
import com.sliit.smartcampus.dto.booking.BookingResponse;
import com.sliit.smartcampus.entity.enums.BookingStatus;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class BookingModelAssembler implements RepresentationModelAssembler<BookingResponse, EntityModel<BookingResponse>> {

    /**
     * Converts BookingResponse to HAL EntityModel with both Spring HATEOAS links and explicit _links in response DTO.
     * @param listQueryAll same as {@code GET /api/bookings?all=} for the collection {@code rel=bookings} link
     */
    @NonNull
    public EntityModel<BookingResponse> toModel(BookingResponse b, boolean listQueryAll) {
        // Populate _links in the response DTO for explicit HATEOAS support
        var selfLink = linkTo(methodOn(BookingController.class).get(b.id())).toUri();
        var bookingsLink = linkTo(methodOn(BookingController.class).list(listQueryAll)).toUri();
        
        b.links().put("self", new HateoasLink(selfLink.toString(), "GET", "Get Booking Details"));
        b.links().put("bookings", new HateoasLink(bookingsLink.toString(), "GET", "List Bookings"));

        BookingStatus s = b.status();
        if (s == BookingStatus.PENDING || s == BookingStatus.APPROVED) {
            var cancelLink = linkTo(methodOn(BookingController.class).cancel(b.id())).toUri();
            b.links().put("cancel", new HateoasLink(cancelLink.toString(), "POST", "Cancel Booking"));
            
            var timesLink = linkTo(BookingController.class).slash(b.id()).slash("times").toUri();
            b.links().put("times", new HateoasLink(timesLink.toString(), "PUT", "Reschedule Booking"));
        }
        if (s == BookingStatus.PENDING || s == BookingStatus.CANCELLED || s == BookingStatus.REJECTED) {
            var deleteLink = linkTo(methodOn(BookingController.class).delete(b.id())).toUri();
            b.links().put("delete", new HateoasLink(deleteLink.toString(), "DELETE", "Delete Booking"));
        }
        var statusLink = linkTo(BookingController.class).slash(b.id()).slash("status").toUri();
        b.links().put("status", new HateoasLink(statusLink.toString(), "PUT", "Update Booking Status"));

        // Also add Spring HATEOAS links to EntityModel for REST clients that use them
        EntityModel<BookingResponse> model = EntityModel.of(b);
        model.add(linkTo(methodOn(BookingController.class).get(b.id())).withSelfRel());
        model.add(linkTo(methodOn(BookingController.class).list(listQueryAll)).withRel("bookings"));

        if (s == BookingStatus.PENDING || s == BookingStatus.APPROVED) {
            model.add(linkTo(methodOn(BookingController.class).cancel(b.id())).withRel("cancel"));
            model.add(linkTo(BookingController.class).slash(b.id()).slash("times").withRel("times"));
        }
        if (s == BookingStatus.PENDING || s == BookingStatus.CANCELLED || s == BookingStatus.REJECTED) {
            model.add(linkTo(methodOn(BookingController.class).delete(b.id())).withRel("delete"));
        }
        model.add(linkTo(BookingController.class).slash(b.id()).slash("status").withRel("status"));
        
        return model;
    }

    @Override
    @NonNull
    public EntityModel<BookingResponse> toModel(@NonNull BookingResponse entity) {
        return toModel(entity, false);
    }
}
