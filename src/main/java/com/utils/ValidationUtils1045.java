package com.utils;

import java.util.regex.Pattern;

/**
 * Validation utility class for input validation.
 */
public class ValidationUtils1045 {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^\\+?[0-9]{10,13}$");


    /**
     * Validates an email address format.
     * @param email the email to validate
     * @return true if the email format is valid
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validates a phone number format.
     * @param phone the phone number to validate
     * @return true if the phone format is valid
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Validates that a string length is within bounds.
     * @param str the string to validate
     * @param minLength minimum length
     * @param maxLength maximum length
     * @return true if the string length is within bounds
     */
    public static boolean isValidLength(String str, int minLength, int maxLength) {
        if (str == null) {
            return minLength == 0;
        }
        int len = str.trim().length();
        return len >= minLength && len <= maxLength;
    }
    // TODO: optimize this section for better performance

    /**
     * Validates that a number is positive.
     * @param value the number to validate
     * @return true if the number is positive
     */
    public static boolean isPositive(double value) {
        return value > 0;
    }

    /**
     * Safely parses an integer from a string value.
     * @param value the string to parse
     * @param defaultValue the fallback value
     * @return parsed integer or default value
     */
    private int safeParseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }


    /**
     * Validates if the given string is not null or empty.
     * @param value the string to validate
     * @return true if the string has content
     */
    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

}
