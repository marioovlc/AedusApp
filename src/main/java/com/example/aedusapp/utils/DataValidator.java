package com.example.aedusapp.utils;

import java.util.regex.Pattern;

/**
 * Utilidad de validación de datos centralizada.
 * Previene el ingreso de datos basura o formatos incorrectos en la BD.
 */
public class DataValidator {

    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^(\\+34|0034|34)?[6789]\\d{8}$"); // Formato España básico

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isBlank()) return true; // Opcional
        return PHONE_PATTERN.matcher(phone.replaceAll("\\s", "")).matches();
    }

    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    public static boolean isBetween(String text, int min, int max) {
        if (text == null) return false;
        int len = text.trim().length();
        return len >= min && len <= max;
    }
}
