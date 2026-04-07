package ecommerce.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // Enable multipart true for HTML support
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setFrom("notification@beautyhaat.com");
            helper.setSubject("Your BeautyHaat Verification Code");

            // Professional HTML Template
            String htmlContent = """
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f8f9fa; border-radius: 10px;">
                    <div style="text-align: center; margin-bottom: 30px;">
                        <h1 style="color: #E91E63; margin: 0;">BeautyHaat</h1>
                    </div>
                    <div style="background-color: #ffffff; padding: 30px; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.05);">
                        <h2 style="color: #2c3e50; text-align: center; margin-top: 0;">Verify Your Email Address</h2>
                        <p style="color: #6c757d; font-size: 16px; line-height: 1.5; text-align: center;">
                            Thank you for registering! Please use the verification code below to complete your setup. This code is valid for 5 minutes.
                        </p>
                        <div style="text-align: center; margin: 30px 0;">
                            <span style="display: inline-block; padding: 15px 30px; background-color: #f8f9fa; border: 2px dashed #E91E63; border-radius: 8px; font-size: 32px; font-weight: bold; color: #2c3e50; letter-spacing: 5px;">
                                %s
                            </span>
                        </div>
                        <p style="color: #6c757d; font-size: 14px; text-align: center; margin-bottom: 0;">
                            If you didn't request this code, you can safely ignore this email.
                        </p>
                    </div>
                    <div style="text-align: center; margin-top: 20px; color: #a0aec0; font-size: 12px;">
                        &copy; 2024 BeautyHaat. All rights reserved.
                    </div>
                </div>
                """.formatted(otp);

            helper.setText(htmlContent, true); // true indicates HTML
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
}