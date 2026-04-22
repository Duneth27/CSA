package com.smartcampus.resource;

import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.repository.DataStore;
import com.smartcampus.exception.CustomExceptions.SensorUnavailableException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {
    private String sensorId;
    private DataStore dataStore = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getAllReadings() {
        Sensor sensor = dataStore.getSensor(sensorId);
        if (sensor == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMsg("Sensor with ID " + sensorId + " not found"))
                    .build();
        }

        List<SensorReading> readings = dataStore.getReadings(sensorId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sensorId", sensorId);
        response.put("readings", readings);
        response.put("count", readings.size());

        return Response.ok(response).build();
    }

    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = dataStore.getSensor(sensorId);
        if (sensor == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMsg("Sensor with ID " + sensorId + " not found"))
                    .build();
        }

        // Check if sensor is available for readings
        if ("MAINTENANCE".equals(sensor.getStatus()) || "OFFLINE".equals(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Cannot add reading to sensor " + sensorId + 
                    " because it is currently in " + sensor.getStatus() + " state",
                    sensorId,
                    sensor.getStatus()
            );
        }

        if (reading == null || reading.getValue() == 0) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorMsg("Reading value is required"))
                    .build();
        }

        SensorReading created = dataStore.addReading(sensorId, reading);
        return Response
                .status(Response.Status.CREATED)
                .entity(created)
                .build();
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
