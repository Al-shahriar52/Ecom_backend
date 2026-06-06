package ecommerce.controller.impl;

import ecommerce.dto.ContactRequest;
import ecommerce.service.impl.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contact")
public class ContactController {

    @Autowired
    private EmailService emailService;

    @PostMapping
    public ResponseEntity<String> submitContactForm(@RequestBody ContactRequest request) {
        try {
            emailService.sendContactEmail(request.getName(), request.getEmail(), request.getMessage());
            return ResponseEntity.ok("Email sent successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error sending email");
        }
    }
}