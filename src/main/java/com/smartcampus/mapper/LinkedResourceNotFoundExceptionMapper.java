package com.smartcampus.mapper;

import com.smartcampus.exception.CustomExceptions.LinkedResourceNotFoundException;
import com.smartcampus.exception.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {
    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        ErrorResponse error = new ErrorResponse(
                422,
                exception.getMessage(),
                "LINKED_RESOURCE_NOT_FOUND"
        );
        return Response
                .status(422)
                .entity(error)
                .build();
    }
}
