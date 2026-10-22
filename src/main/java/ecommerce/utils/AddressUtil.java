package ecommerce.utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Builds one clean address line from street, area and city, without repeating parts that are already inside the street text. */
public final class AddressUtil {
    private AddressUtil() {}

    public static String compose(String street, String area, String city) {
        String a = area == null ? "" : area.trim();
        String c = city == null ? "" : city.trim();

        Set<String> seen = new LinkedHashSet<>();
        List<String> out = new ArrayList<>();
        if (street != null) {
            for (String tok : street.split(",")) {
                String t = tok.trim();
                if (t.isEmpty()) continue;
                String k = t.toLowerCase();
                if (k.equals(a.toLowerCase()) || k.equals(c.toLowerCase())) continue; // added once at the end
                if (seen.add(k)) out.add(t);
            }
        }
        if (!a.isEmpty() && seen.add(a.toLowerCase())) out.add(a);
        if (!c.isEmpty() && seen.add(c.toLowerCase())) out.add(c);
        return String.join(", ", out);
    }
}
