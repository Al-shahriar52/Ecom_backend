package ecommerce.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class SmsService {

    @Value("${bdbulksms.token}")
    private String token;

    @Value("${bdbulksms.api-url:https://api.bdbulksms.net/api.php}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * OTP SMS (~50 characters -> Strictly 1 SMS count / 70 max limit)
     */
    @Async
    public void sendOtpSms(String toPhoneNumber, String otp) {
        String messageText = "[বিউটিহাট] আপনার OTP: " + otp + "। মেয়াদ ৫ মিনিট।";
        sendSms(toPhoneNumber, messageText);
    }

    /**
     * Temporary Password SMS (~65 characters -> Strictly 1 SMS count / 70 max limit)
     */
    @Async
    public void sendTemporaryPasswordSms(String toPhoneNumber, String name, String tempPassword) {
        String messageText = "[বিউটিহাট] আপনার পাসওয়ার্ড: " + tempPassword + "। অনুগ্রহ করে লগইন করে পরিবর্তন করুন।";
        sendSms(toPhoneNumber, messageText);
    }

    /**
     * Order Confirmation SMS (~60 characters -> Strictly 1 SMS count / 70 max limit)
     */
    @Async
    public void sendOrderConfirmationSms(String toPhoneNumber, String orderId, double totalAmount) {
        String messageText = String.format("[বিউটিহাট] অর্ডার #%s কনফার্ম হয়েছে। মোট: ৳%.2f। ধন্যবাদ!", orderId, totalAmount);
        sendSms(toPhoneNumber, messageText);
    }

    /**
     * Core reusable method to dispatch POST requests to BDBulkSMS
     */
    @Async
    public void sendSms(String toPhoneNumber, String messageText) {
        if (toPhoneNumber == null || toPhoneNumber.isBlank()) {
            System.err.println("Skipping SMS dispatch: Phone number is missing.");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("token", token);
            map.add("to", toPhoneNumber.trim());
            map.add("message", messageText);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            String response = restTemplate.postForObject(apiUrl, request, String.class);
            System.out.println("BDBulkSMS Response for " + toPhoneNumber + ": " + response);

        } catch (Exception e) {
            System.err.println("Failed to send SMS to " + toPhoneNumber + ": " + e.getMessage());
        }
    }
}