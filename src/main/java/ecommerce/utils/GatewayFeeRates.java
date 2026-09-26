package ecommerce.utils;

import java.util.Map;

/**
 * NOTE: hardcoded for now - the Payment Gateways settings screen in the
 * frontend isn't wired to a real config endpoint yet, so these values need
 * to be kept in sync manually until that module is built.
 */
public class GatewayFeeRates {
    public static final Map<String, Double> RATES = Map.of(
            "BKASH", 1.85, "NAGAD", 1.99, "ROCKET", 1.80, "CARD", 2.75, "COD", 1.20
    );

    public static double rateFor(String paymentMethod) {
        return RATES.getOrDefault(paymentMethod, 0.0);
    }
}
