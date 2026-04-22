package com.smartcampus.repository;

import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.model.SensorRoom;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore {
    private static DataStore instance;
    private final Map<String, SensorRoom> rooms;
    private final Map<String, Sensor> sensors;
    private final Map<String, List<SensorReading>> readings;

    private DataStore() {
        this.rooms = new ConcurrentHashMap<>();
        this.sensors = new ConcurrentHashMap<>();
        this.readings = new ConcurrentHashMap<>();
        initializeSampleData();
    }

    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    private void initializeSampleData() {
        // Create sample rooms
        SensorRoom room1 = new SensorRoom("LIB-301", "Library Quiet Study", 50);
        SensorRoom room2 = new SensorRoom("LEC-101", "Lecture Theater 101", 200);
        SensorRoom room3 = new SensorRoom("LAB-205", "Computer Lab", 40);

        rooms.put(room1.getId(), room1);
        rooms.put(room2.getId(), room2);
        rooms.put(room3.getId(), room3);

        // Create sample sensors
        Sensor sensor1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        Sensor sensor2 = new Sensor("CO2-001", "CO2", "ACTIVE", 400.0, "LIB-301");
        Sensor sensor3 = new Sensor("OCC-001", "Occupancy", "ACTIVE", 15.0, "LEC-101");
        Sensor sensor4 = new Sensor("TEMP-002", "Temperature", "MAINTENANCE", 21.0, "LAB-205");

        sensors.put(sensor1.getId(), sensor1);
        sensors.put(sensor2.getId(), sensor2);
        sensors.put(sensor3.getId(), sensor3);
        sensors.put(sensor4.getId(), sensor4);

        // Add sensor IDs to rooms
        room1.addSensorId(sensor1.getId());
        room1.addSensorId(sensor2.getId());
        room2.addSensorId(sensor3.getId());
        room3.addSensorId(sensor4.getId());

        // Initialize reading storage
        readings.put("TEMP-001", new ArrayList<>());
        readings.put("CO2-001", new ArrayList<>());
        readings.put("OCC-001", new ArrayList<>());
        readings.put("TEMP-002", new ArrayList<>());
    }

    // Room operations
    public SensorRoom createRoom(SensorRoom room) {
        rooms.put(room.getId(), room);
        return room;
    }

    public SensorRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public List<SensorRoom> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public boolean roomExists(String roomId) {
        return rooms.containsKey(roomId);
    }

    public boolean deleteRoom(String roomId) {
        if (rooms.containsKey(roomId)) {
            rooms.remove(roomId);
            return true;
        }
        return false;
    }

    // Sensor operations
    public Sensor createSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        readings.put(sensor.getId(), new ArrayList<>());
        
        if (roomExists(sensor.getRoomId())) {
            getRoom(sensor.getRoomId()).addSensorId(sensor.getId());
        }
        return sensor;
    }

    public Sensor getSensor(String sensorId) {
        return sensors.get(sensorId);
    }

    public List<Sensor> getAllSensors() {
        return new ArrayList<>(sensors.values());
    }

    public List<Sensor> getSensorsByType(String type) {
        List<Sensor> result = new ArrayList<>();
        for (Sensor sensor : sensors.values()) {
            if (sensor.getType().equalsIgnoreCase(type)) {
                result.add(sensor);
            }
        }
        return result;
    }

    public List<Sensor> getSensorsByRoom(String roomId) {
        List<Sensor> result = new ArrayList<>();
        for (Sensor sensor : sensors.values()) {
            if (sensor.getRoomId().equals(roomId)) {
                result.add(sensor);
            }
        }
        return result;
    }

    public boolean sensorExists(String sensorId) {
        return sensors.containsKey(sensorId);
    }

    public boolean deleteSensor(String sensorId) {
        Sensor sensor = sensors.get(sensorId);
        if (sensor != null) {
            sensors.remove(sensorId);
            readings.remove(sensorId);
            
            if (roomExists(sensor.getRoomId())) {
                getRoom(sensor.getRoomId()).removeSensorId(sensorId);
            }
            return true;
        }
        return false;
    }

    // Reading operations
    public SensorReading addReading(String sensorId, SensorReading reading) {
        readings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);
        
        // Update sensor's current value
        Sensor sensor = sensors.get(sensorId);
        if (sensor != null) {
            sensor.setCurrentValue(reading.getValue());
        }
        
        return reading;
    }

    public List<SensorReading> getReadings(String sensorId) {
        return new ArrayList<>(readings.getOrDefault(sensorId, new ArrayList<>()));
    }

    public void clearAllData() {
        rooms.clear();
        sensors.clear();
        readings.clear();
    }

    public int getTotalRooms() {
        return rooms.size();
    }

    public int getTotalSensors() {
        return sensors.size();
    }

    public int getTotalReadings(String sensorId) {
        return readings.getOrDefault(sensorId, new ArrayList<>()).size();
    }
}
