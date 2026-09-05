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
     * Sends OTP via SMS in Bangla Font
     */
    @Async
    public void sendOtpSms(String toPhoneNumber, String otp) {
        String messageText = "বিউটিহাট-এ আপনার ভেরিফিকেশন কোড: " + otp + "। কোডটি ৫ মিনিটের জন্য কার্যকর থাকবে।";
        sendSms(toPhoneNumber, messageText);
    }

    /**
     * Sends temporary password created by admin via SMS in Bangla Font
     */
    @Async
    public void sendTemporaryPasswordSms(String toPhoneNumber, String name, String tempPassword) {
        String messageText = "প্রিয় " + name + ", বিউটিহাট-এ আপনার অ্যাকাউন্ট তৈরি করা হয়েছে। টেম্পোরারি পাসওয়ার্ড: " + tempPassword + " । অনুগ্রহ করে লগইন করে পাসওয়ার্ড পরিবর্তন করুন।";
        sendSms(toPhoneNumber, messageText);
    }

    /**
     * Sends order confirmation details via SMS in Bangla Font
     */
    @Async
    public void sendOrderConfirmationSms(String toPhoneNumber, String orderId, double totalAmount) {
        String messageText = String.format("বিউটিহাট-এ আপনার অর্ডার #%s সফলভাবে নিশ্চিত করা হয়েছে! মোট টাকা: ৳%.2f। আমাদের সাথে থাকার জন্য ধন্যবাদ!", orderId, totalAmount);
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