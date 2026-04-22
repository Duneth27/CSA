package com.smartcampus.exception;

public class CustomExceptions {

    public static class RoomNotEmptyException extends RuntimeException {
        private String roomId;

        public RoomNotEmptyException(String message) {
            super(message);
        }

        public RoomNotEmptyException(String message, String roomId) {
            super(message);
            this.roomId = roomId;
        }

        public String getRoomId() {
            return roomId;
        }
    }

    public static class LinkedResourceNotFoundException extends RuntimeException {
        private String resourceType;
        private String resourceId;

        public LinkedResourceNotFoundException(String message) {
            super(message);
        }

        public LinkedResourceNotFoundException(String message, String resourceType, String resourceId) {
            super(message);
            this.resourceType = resourceType;
            this.resourceId = resourceId;
        }

        public String getResourceType() {
            return resourceType;
        }

        public String getResourceId() {
            return resourceId;
        }
    }

    public static class SensorUnavailableException extends RuntimeException {
        private String sensorId;
        private String status;

        public SensorUnavailableException(String message) {
            super(message);
        }

        public SensorUnavailableException(String message, String sensorId, String status) {
            super(message);
            this.sensorId = sensorId;
            this.status = status;
        }

        public String getSensorId() {
            return sensorId;
        }

        public String getStatus() {
            return status;
        }
    }
}
