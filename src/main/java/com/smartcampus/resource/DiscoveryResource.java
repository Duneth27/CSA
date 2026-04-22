package com.smartcampus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response getApiMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        
        metadata.put("api_version", "1.0.0");
        metadata.put("title", "Smart Campus Sensor & Room Management API");
        metadata.put("description", "RESTful API for managing rooms and sensors in a smart campus environment");
        metadata.put("contact", new Contact("API Support", "support@smartcampus.edu"));
        
        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms", "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        resources.put("health", "/api/v1/health");
        
        metadata.put("resources", resources);
        metadata.put("timestamp", System.currentTimeMillis());
        
        return Response.ok(metadata).build();
    }

    @GET
    @Path("health")
    public Response healthCheck() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("service", "Smart Campus API");
        
        return Response.ok(health).build();
    }

    // Inner class for contact information
    public static class Contact {
        private String name;
        private String email;

        public Contact() {
        }

        public Contact(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
