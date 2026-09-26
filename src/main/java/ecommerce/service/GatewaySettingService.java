package ecommerce.service;

import ecommerce.dto.admin.GatewaySettingDto;

import java.util.List;

public interface GatewaySettingService {
    /** Used internally by fee calculations elsewhere - falls back to a sensible default if not yet configured. */
    double getRate(String paymentMethod);

    List<GatewaySettingDto> getAllWithStats();

    GatewaySettingDto updateRate(String paymentMethod, double feeRatePct);
}
