package ecommerce.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import ecommerce.entity.Invoice;
import ecommerce.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class SteadfastService {
    @Value("${steadfast.api.key}")
    private String apiKey;

    @Value("${steadfast.api.secret}")
    private String secretKey;

    @Value("${steadfast.api.bulk-pickup-endpoint}")
    private String bulkPickupEndpoint;

    @Value("${steadfast.api.single-pickup-endpoint}")
    private String singlePickupEndpoint;

    private final RestTemplate restTemplate = new RestTemplate();

    public JsonNode sendBulkPickupRequest(List<Order> orders, Map<Long, Invoice> invoiceMap) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Key", apiKey);
        headers.set("Secret-Key", secretKey);

        // 1. Build the list of order objects
        List<Map<String, Object>> orderDataList = new ArrayList<>();
        
        for (Order order : orders) {
            Invoice invoice = invoiceMap.get(order.getId());
            if (invoice == null) continue;

            Map<String, Object> item = new HashMap<>();
            // Steadfast uses 'invoice' as their unique identifier mapping
            item.put("invoice", invoice.getInvoiceNumber()); 
            item.put("recipient_name", order.getName() != null ? order.getName() : "N/A");
            item.put("recipient_address", order.getShippingAddress() + ", " + order.getArea() + ", " + order.getCity());
            item.put("recipient_phone", order.getPhoneNumber() != null ? order.getPhoneNumber() : "");
            
            double codAmount = order.getPaymentMethod() == ecommerce.enums.PaymentMethod.COD ? invoice.getTotalAmount() : 0.0;
            item.put("cod_amount", codAmount);
            item.put("note", order.getOrderNote());
            
            orderDataList.add(item);
        }

        // 2. Wrap in 'data' object as per their PHP doc example
        Map<String, Object> payload = new HashMap<>();
        payload.put("data", orderDataList);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            // Using JsonNode handles both direct Arrays [ ] and Objects { "data": [ ] }
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(bulkPickupEndpoint, request, JsonNode.class);
            return response.getBody();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to Steadfast API: " + e.getMessage());
        }
    }

    public JsonNode sendSinglePickupRequest(Order order, Invoice invoice) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Key", apiKey);
        headers.set("Secret-Key", secretKey);

        if (invoice == null) {
            throw new IllegalArgumentException("Invoice cannot be null");
        }

        // Build the single order payload directly (No need to wrap in a 'data' array)
        Map<String, Object> payload = new HashMap<>();
        payload.put("invoice", invoice.getInvoiceNumber());
        payload.put("recipient_name", order.getName() != null ? order.getName() : "N/A");
        payload.put("recipient_address", order.getShippingAddress() + ", " + order.getArea() + ", " + order.getCity());
        payload.put("recipient_phone", order.getPhoneNumber() != null ? order.getPhoneNumber() : "");

        double codAmount = order.getPaymentMethod() == ecommerce.enums.PaymentMethod.COD ? invoice.getTotalAmount() : 0.0;
        payload.put("cod_amount", codAmount);
        payload.put("note", order.getOrderNote());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            // Assuming you have a variable like singlePickupEndpoint configured similarly to bulkPickupEndpoint
            // Usually, Steadfast's single endpoint ends in /create_order
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(singlePickupEndpoint, request, JsonNode.class);
            return response.getBody();

        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to Steadfast Single API: " + e.getMessage());
        }
    }
}