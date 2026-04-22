package com.smartcampus.model;

import java.util.ArrayList;
import java.util.List;

public class SensorRoom {
    private String id;
    private String name;
    private int capacity;
    private List<String> sensorIds;

    public SensorRoom() {
        this.sensorIds = new ArrayList<>();
    }

    public SensorRoom(String id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.sensorIds = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public List<String> getSensorIds() {
        return sensorIds;
    }

    public void setSensorIds(List<String> sensorIds) {
        this.sensorIds = sensorIds;
    }

    public void addSensorId(String sensorId) {
        if (!this.sensorIds.contains(sensorId)) {
            this.sensorIds.add(sensorId);
        }
    }

    public void removeSensorId(String sensorId) {
        this.sensorIds.remove(sensorId);
    }

    @Override
    public String toString() {
        return "SensorRoom{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", capacity=" + capacity +
                ", sensorIds=" + sensorIds +
                '}';
    }
}
