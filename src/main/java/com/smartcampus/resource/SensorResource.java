package com.smartcampus.resource;

import com.smartcampus.model.Sensor;
import com.smartcampus.repository.DataStore;
import com.smartcampus.exception.CustomExceptions.LinkedResourceNotFoundException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Path("sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {
    private DataStore dataStore = DataStore.getInstance();

    @Context
    private UriInfo uriInfo;

    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensors;
        
        if (type != null && !type.isEmpty()) {
            sensors = dataStore.getSensorsByType(type);
        } else {
            sensors = dataStore.getAllSensors();
        }
        
        return Response.ok(sensors).build();
    }

    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isEmpty()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorMsg("Sensor ID is required"))
                    .build();
        }

        // Validate that the room exists
        if (sensor.getRoomId() == null || !dataStore.roomExists(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "Room with ID " + sensor.getRoomId() + " does not exist",
                    "SensorRoom",
                    sensor.getRoomId()
            );
        }

        if (dataStore.sensorExists(sensor.getId())) {
            return Response
                    .status(Response.Status.CONFLICT)
                    .entity(new ErrorMsg("Sensor with ID " + sensor.getId() + " already exists"))
                    .build();
        }

        // Set default status if not provided
        if (sensor.getStatus() == null || sensor.getStatus().isEmpty()) {
            sensor.setStatus("ACTIVE");
        }

        Sensor created = dataStore.createSensor(sensor);
        URI location = uriInfo.getAbsolutePathBuilder().path(created.getId()).build();
        return Response
                .status(Response.Status.CREATED)
                .location(location)
                .entity(created)
                .build();
    }

    @GET
    @Path("{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = dataStore.getSensor(sensorId);
        if (sensor == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMsg("Sensor with ID " + sensorId + " not found"))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    @Path("{sensorId}/readings")
    public SensorReadingResource getReadingsSubResource(@PathParam("sensorId") String sensorId) {
        Sensor sensor = dataStore.getSensor(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor with ID " + sensorId + " not found");
        }
        return new SensorReadingResource(sensorId);
    }

    @DELETE
    @Path("{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = dataStore.getSensor(sensorId);
        if (sensor == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMsg("Sensor with ID " + sensorId + " not found"))
                    .build();
        }

        boolean deleted = dataStore.deleteSensor(sensorId);
        if (deleted) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorMsg("Failed to delete sensor"))
                .build();
    }

    @PUT
    @Path("{sensorId}")
    public Response updateSensor(@PathParam("sensorId") String sensorId, Sensor updatedSensor) {
        Sensor sensor = dataStore.getSensor(sensorId);
        if (sensor == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMsg("Sensor with ID " + sensorId + " not found"))
                    .build();
        }

        if (updatedSensor.getType() != null) sensor.setType(updatedSensor.getType());
        if (updatedSensor.getStatus() != null) sensor.setStatus(updatedSensor.getStatus());
        if (updatedSensor.getCurrentValue() != 0) sensor.setCurrentValue(updatedSensor.getCurrentValue());

        return Response.ok(sensor).build();
    }

    // Helper class for error messages
    public static class ErrorMsg {
        private String error;

        public ErrorMsg() {
        }

        public ErrorMsg(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
