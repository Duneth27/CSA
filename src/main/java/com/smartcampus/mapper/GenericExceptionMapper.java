package com.smartcampus.mapper;

import com.smartcampus.exception.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOGGER = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        ErrorResponse error = new ErrorResponse(
                500,
                "An unexpected error occurred. Please contact support.",
                "INTERNAL_SERVER_ERROR"
        );
        
        // Log the actual exception for debugging purposes (not exposed to client)
        LOGGER.log(Level.SEVERE, "Unhandled exception caught by global mapper", exception);
        
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .build();
    }
}
