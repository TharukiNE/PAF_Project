package com.sliit.smartcampus.hateoas;

import com.sliit.smartcampus.controller.BookingController;
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
     * @param listQueryAll same as {@code GET /api/bookings?all=} for the collection {@code rel=bookings} link
     */
    @NonNull
    public EntityModel<BookingResponse> toModel(BookingResponse b, boolean listQueryAll) {
        EntityModel<BookingResponse> model = EntityModel.of(b);

        model.add(linkTo(methodOn(BookingController.class).get(b.id())).withSelfRel());
        model.add(linkTo(methodOn(BookingController.class).list(listQueryAll)).withRel("bookings"));

        BookingStatus s = b.status();
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
