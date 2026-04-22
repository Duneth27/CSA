package com.smartcampus.resource;

import com.smartcampus.model.SensorRoom;
import com.smartcampus.repository.DataStore;
import com.smartcampus.exception.CustomExceptions.RoomNotEmptyException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorRoomResource {
    private DataStore dataStore = DataStore.getInstance();

    @GET
    public Response getAllRooms() {
        List<SensorRoom> rooms = dataStore.getAllRooms();
        return Response.ok(rooms).build();
    }

    @POST
    public Response createRoom(SensorRoom room) {
        if (room == null || room.getId() == null || room.getId().isEmpty()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorMsg("Room ID and name are required"))
                    .build();
        }

        if (dataStore.roomExists(room.getId())) {
            return Response
                    .status(Response.Status.CONFLICT)
                    .entity(new ErrorMsg("Room with ID " + room.getId() + " already exists"))
                    .build();
        }

        SensorRoom created = dataStore.createRoom(room);
        return Response
                .status(Response.Status.CREATED)
                .entity(created)
                .build();
    }

    @GET
    @Path("{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        SensorRoom room = dataStore.getRoom(roomId);
        if (room == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMsg("Room with ID " + roomId + " not found"))
                    .build();
        }
        return Response.ok(room).build();
    }

    @DELETE
    @Path("{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        SensorRoom room = dataStore.getRoom(roomId);
        if (room == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMsg("Room with ID " + roomId + " not found"))
                    .build();
        }

        // Check if room has active sensors
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                    "Cannot delete room " + roomId + " because it contains " + 
                    room.getSensorIds().size() + " active sensor(s)",
                    roomId
            );
        }

        boolean deleted = dataStore.deleteRoom(roomId);
        if (deleted) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorMsg("Failed to delete room"))
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
