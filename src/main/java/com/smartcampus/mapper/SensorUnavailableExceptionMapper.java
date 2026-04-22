package com.smartcampus.mapper;

import com.smartcampus.exception.CustomExceptions.SensorUnavailableException;
import com.smartcampus.exception.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {
    @Override
    public Response toResponse(SensorUnavailableException exception) {
        ErrorResponse error = new ErrorResponse(
                403,
                exception.getMessage(),
                "SENSOR_UNAVAILABLE"
        );
        return Response
                .status(Response.Status.FORBIDDEN)
                .entity(error)
                .build();
    }
}
