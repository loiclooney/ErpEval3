package itu.mg.erp.util;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReflectionUtil {
    public static Map<String, Object> toMap(Object obj) throws IllegalArgumentException, IllegalAccessException {
        Map<String, Object> map = new HashMap<>();
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    map.put(field.getName(), field.get(obj));
                } catch (SecurityException | InaccessibleObjectException e) {
                    // Skip inaccessible fields
                }
            }
            clazz = clazz.getSuperclass();
        }
        return map;
    }
    
    private static String capitalize(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}

