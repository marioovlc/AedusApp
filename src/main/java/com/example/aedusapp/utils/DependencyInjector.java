package com.example.aedusapp.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight Dependency Injector / Service Locator.
 * Maintains singletons of our DAOs and Services to prevent tight coupling.
 */
public class DependencyInjector {
    private static final Map<Class<?>, Object> registry = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> type) {
        return (T) registry.get(type);
    }
    
    public static <T> void register(Class<T> type, T instance) {
        registry.put(type, instance);
    }
}
