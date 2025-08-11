package ecommerce.utils;

import java.util.regex.Pattern;

public class ContactValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(\\+\\d{1,3}[- ]?)?\\d{7,15}$"
    );

    public static ContactType identifyContactType(String input) {
        if (input == null || input.trim().isEmpty()) {
            return ContactType.INVALID;
        }

        String trimmed = input.trim();

        if (EMAIL_PATTERN.matcher(trimmed).matches()) {
            return ContactType.EMAIL;
        } else if (PHONE_PATTERN.matcher(trimmed).matches()) {
            return ContactType.PHONE;
        } else {
            return ContactType.INVALID;
        }
    }

    public enum ContactType {
        EMAIL, PHONE, INVALID
    }
}
