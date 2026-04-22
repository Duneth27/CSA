package com.smartcampus;

import com.smartcampus.filter.LoggingFilter;
import com.smartcampus.mapper.RoomNotEmptyExceptionMapper;
import com.smartcampus.mapper.LinkedResourceNotFoundExceptionMapper;
import com.smartcampus.mapper.SensorUnavailableExceptionMapper;
import com.smartcampus.mapper.GenericExceptionMapper;
import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.SensorRoomResource;
import com.smartcampus.resource.SensorResource;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        
        // Register Resource Classes
        classes.add(DiscoveryResource.class);
        classes.add(SensorRoomResource.class);
        classes.add(SensorResource.class);
        
        // Register Exception Mappers
        classes.add(RoomNotEmptyExceptionMapper.class);
        classes.add(LinkedResourceNotFoundExceptionMapper.class);
        classes.add(SensorUnavailableExceptionMapper.class);
        classes.add(GenericExceptionMapper.class);
        
        // Register Filters
        classes.add(LoggingFilter.class);
        
        return classes;
    }
}
