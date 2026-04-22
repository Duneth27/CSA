package com.smartcampus.mapper;

import com.smartcampus.exception.CustomExceptions.RoomNotEmptyException;
import com.smartcampus.exception.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {
    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        ErrorResponse error = new ErrorResponse(
                409,
                exception.getMessage(),
                "ROOM_NOT_EMPTY"
        );
        return Response
                .status(Response.Status.CONFLICT)
                .entity(error)
                .build();
    }
}
