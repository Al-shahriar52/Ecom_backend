package ecommerce.service.impl;

import ecommerce.entity.Invoice;
import ecommerce.entity.Order;
import ecommerce.entity.OrderItem;
import ecommerce.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
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

    @Async
    public void sendOrderConfirmationEmail(User user, Order order, Invoice invoice) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(order.getEmail());
            helper.setFrom("notification@beautyhaat.com");
            helper.setSubject("Order Confirmed! Invoice " + invoice.getInvoiceNumber() + " - BeautyHaat");

            // Build itemized rows dynamically
            StringBuilder itemsTableRows = new StringBuilder();
            for (OrderItem item : order.getOrderItems()) {
                double lineTotal = item.getPrice() * item.getQuantity();
                itemsTableRows.append(String.format("""
                    <tr>
                        <td style="padding: 12px; border-bottom: 1px solid #edf2f7; color: #2d3748; font-size: 14px;">%s</td>
                        <td style="padding: 12px; border-bottom: 1px solid #edf2f7; color: #718096; font-size: 14px; text-align: center;">%d</td>
                        <td style="padding: 12px; border-bottom: 1px solid #edf2f7; color: #718096; font-size: 14px; text-align: right;">৳%.2f</td>
                        <td style="padding: 12px; border-bottom: 1px solid #edf2f7; color: #2d3748; font-size: 14px; text-align: right; font-weight: 600;">৳%.2f</td>
                    </tr>
                    """,
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPrice(),
                        lineTotal
                ));
            }

            // High-end, professional HTML E-commerce template
            String htmlContent = """
                <div style="font-family: 'Segoe UI', Helvetica, Arial, sans-serif; max-width: 650px; margin: 0 auto; padding: 20px; background-color: #f7f9fa;">
                    
                    <div style="background-color: #ffffff; padding: 30px; border-radius: 12px 12px 0 0; border-top: 6px solid #E91E63; border-bottom: 1px solid #edf2f7; text-align: center;">
                        <h1 style="color: #E91E63; margin: 0; font-size: 28px; font-weight: 700; letter-spacing: 1px;">BeautyHaat</h1>
                        <p style="color: #4a5568; margin: 5px 0 0 0; font-size: 14px; font-weight: 500;">Thank you for your order!</p>
                    </div>

                    <div style="background-color: #ffffff; padding: 30px; border-radius: 0 0 12px 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.02);">
                        
                        <p style="color: #2d3748; font-size: 16px; margin-top: 0;">Hello <strong>%s</strong>,</p>
                        <p style="color: #4a5568; font-size: 14px; line-height: 1.6;">
                            Your order has been successfully placed and is being processed. Below you will find your official invoice confirmation details.
                        </p>

                        <table style="width: 100%%; margin: 25px 0; font-size: 14px; border-collapse: collapse;">
                            <tr>
                                <td style="padding: 6px 0; color: #718096;"><strong>Invoice Number:</strong></td>
                                <td style="padding: 6px 0; color: #2d3748; text-align: right;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 6px 0; color: #718096;"><strong>Order ID:</strong></td>
                                <td style="padding: 6px 0; color: #2d3748; text-align: right;">#%d</td>
                            </tr>
                            <tr>
                                <td style="padding: 6px 0; color: #718096;"><strong>Payment Method:</strong></td>
                                <td style="padding: 6px 0; color: #2d3748; text-align: right;">%s</td>
                            </tr>
                        </table>

                        <hr style="border: 0; border-top: 1px solid #edf2f7; margin: 20px 0;" />

                        <h3 style="color: #2d3748; font-size: 15px; margin-bottom: 10px; text-transform: uppercase; letter-spacing: 0.5px;">Shipping Information</h3>
                        <div style="background-color: #f8f9fa; padding: 15px; border-radius: 8px; font-size: 14px; color: #4a5568; line-height: 1.5; margin-bottom: 25px;">
                            <strong>Name:</strong> %s<br/>
                            <strong>Phone:</strong> %s<br/>
                            <strong>Address:</strong> %s, %s, %s
                        </div>

                        <h3 style="color: #2d3748; font-size: 15px; margin-bottom: 10px; text-transform: uppercase; letter-spacing: 0.5px;">Order Summary</h3>
                        <table style="width: 100%%; border-collapse: collapse; margin-bottom: 20px;">
                            <thead>
                                <tr style="background-color: #f8f9fa;">
                                    <th style="padding: 12px; color: #4a5568; font-size: 13px; font-weight: 600; text-align: left; border-bottom: 2px solid #edf2f7;">Item</th>
                                    <th style="padding: 12px; color: #4a5568; font-size: 13px; font-weight: 600; text-align: center; border-bottom: 2px solid #edf2f7;">Qty</th>
                                    <th style="padding: 12px; color: #4a5568; font-size: 13px; font-weight: 600; text-align: right; border-bottom: 2px solid #edf2f7;">Price</th>
                                    <th style="padding: 12px; color: #4a5568; font-size: 13px; font-weight: 600; text-align: right; border-bottom: 2px solid #edf2f7;">Total</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                        </table>

                        <table style="width: 280px; margin-left: auto; font-size: 14px; color: #4a5568; line-height: 2;">
                            <tr>
                                <td style="text-align: left;">Subtotal:</td>
                                <td style="text-align: right; font-weight: 500;">৳%.2f</td>
                            </tr>
                            <tr>
                                <td style="text-align: left;">Shipping Cost:</td>
                                <td style="text-align: right; font-weight: 500;">৳%.2f</td>
                            </tr>
                            <tr style="font-size: 16px; color: #2d3748; font-weight: 700;">
                                <td style="text-align: left; padding-top: 10px; border-top: 1px solid #edf2f7;">Grand Total:</td>
                                <td style="text-align: right; padding-top: 10px; border-top: 1px solid #edf2f7; color: #E91E63;">৳%.2f</td>
                            </tr>
                        </table>

                        <div style="text-align: center; margin: 35px 0 15px 0;">
                            <a href="https://beautyhaat.com/order-success/%d" 
                               style="display: inline-block; padding: 13px 30px; background-color: #E91E63; color: #ffffff; text-decoration: none; font-weight: 600; font-size: 15px; border-radius: 6px; box-shadow: 0 4px 10px rgba(233,30,99,0.3);">
                               Track Your Order
                            </a>
                        </div>
                    </div>

                    <div style="text-align: center; margin-top: 25px; color: #a0aec0; font-size: 12px; line-height: 1.5;">
                        If you have any questions, reply to this email or contact support.<br/>
                        &copy; 2026 <strong>BeautyHaat</strong>. All rights reserved.
                    </div>
                </div>
                """.formatted(
                    order.getName(),
                    invoice.getInvoiceNumber(),
                    order.getId(),
                    order.getPaymentMethod().name(),
                    order.getName(),
                    order.getPhoneNumber(),
                    order.getShippingAddress(), order.getArea(), order.getCity(),
                    itemsTableRows.toString(),
                    invoice.getSubTotal(),
                    invoice.getShippingAmount(),
                    invoice.getTotalAmount(),
                    order.getId()
            );

            helper.setText(htmlContent, true);

            // Generate and attach PDF
            byte[] pdfBytes = generatePdfInvoice(order, invoice);
            ByteArrayResource pdfAttachment = new ByteArrayResource(pdfBytes);
            helper.addAttachment(invoice.getInvoiceNumber() + ".pdf", pdfAttachment);

            mailSender.send(message);

        } catch (MessagingException e) {
            // Logs an exception instead of blocking the critical checkout flow
            System.err.println("Failed to send order confirmation email for Order ID: " + order.getId() + ". Error: " + e.getMessage());
        }
    }

    @Async
    public void sendContactEmail(String name, String customerEmail, String message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();

        // Sending to your business emails
        mailMessage.setTo("beautyhaat52@gmail.com");
        mailMessage.setSubject("New Contact Form Submission from: " + name);

        // The body of the email
        String emailBody = "You have received a new message from your website.\n\n" +
                "Name: " + name + "\n" +
                "Email: " + customerEmail + "\n" +
                "Message:\n" + message;

        mailMessage.setText(emailBody);
        mailMessage.setReplyTo(customerEmail); // So you can reply directly to the customer

        mailSender.send(mailMessage);
    }

    public byte[] generatePdfInvoice(Order order, Invoice invoice) {
        // 1. Build itemized rows dynamically for the PDF table
        StringBuilder itemsTableRows = new StringBuilder();
        for (OrderItem item : order.getOrderItems()) {
            double lineTotal = item.getPrice() * item.getQuantity();
            itemsTableRows.append(String.format("""
            <tr>
                <td>%s</td>
                <td class="center">%d</td>
                <td class="right">৳ %.2f</td>
                <td class="right">৳ %.2f</td>
            </tr>
            """,
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getPrice(),
                    lineTotal
            ));
        }

        // 2. Format Date beautifully (e.g., June 07, 2026)
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy");
        String invoiceDate = invoice.getIssuedAt() != null ? invoice.getIssuedAt().format(formatter) : "N/A";

        // 3. The Professional HTML/CSS Template
        // OpenHTMLToPDF is strict, so we ensure all tags are closed properly (e.g., <br/>, <meta />)
        String htmlTemplate = String.format("""
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8" />
            <title>Invoice - BeautyHaat</title>
            <style>
                @page {
                    size: A4;
                    margin: 20mm 15mm;
                }
                body {
                    font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                    margin: 0;
                    padding: 0;
                    color: #333;
                }
                .header-table { width: 100%%; margin-bottom: 30px; border-bottom: 3px solid #E91E63; padding-bottom: 20px; }
                .brand { font-size: 32pt; font-weight: bold; color: #E91E63; margin: 0; letter-spacing: 1px; }
                .tagline { font-size: 10pt; color: #777; margin-top: 5px; }
                .invoice-title { font-size: 24pt; color: #2c3e50; margin: 0; text-transform: uppercase; text-align: right; }
                .meta-table { width: 100%%; margin-bottom: 30px; }
                .meta-cell { width: 50%%; vertical-align: top; }
                .section-title { font-size: 12pt; font-weight: bold; color: #E91E63; text-transform: uppercase; margin-bottom: 10px; border-bottom: 1px solid #ddd; padding-bottom: 5px; }
                .info-block { font-size: 10pt; line-height: 1.6; color: #444; }
                .items-table { width: 100%%; border-collapse: collapse; margin-bottom: 30px; }
                .items-table th { background-color: #E91E63; color: white; padding: 10px; font-size: 10pt; text-align: left; }
                .items-table th.center { text-align: center; }
                .items-table th.right { text-align: right; }
                .items-table td { padding: 12px 10px; border-bottom: 1px solid #eee; font-size: 10pt; }
                .items-table td.center { text-align: center; }
                .items-table td.right { text-align: right; }
                .totals-table { width: 300px; margin-left: auto; border-collapse: collapse; }
                .totals-table td { padding: 8px 10px; font-size: 11pt; text-align: right; }
                .totals-table td.label { text-align: left; color: #666; }
                .grand-total { font-weight: bold; color: #E91E63; font-size: 14pt; background-color: #fdf2f6; }
                .footer { margin-top: 50px; text-align: center; font-size: 9pt; color: #888; border-top: 1px solid #ddd; padding-top: 20px; }
            </style>
        </head>
        <body>
            <table class="header-table">
                <tr>
                    <td>
                        <h1 class="brand">BeautyHaat</h1>
                        <div class="tagline">Cosmetics &#8226; Bags &#8226; Shoes &#8226; Fashion</div>
                    </td>
                    <td style="text-align: right;">
                        <h2 class="invoice-title">Invoice</h2>
                        <div style="font-size: 11pt; margin-top: 5px;"><strong>%s</strong></div>
                        <div style="font-size: 10pt; color: #666;">Date: %s</div>
                    </td>
                </tr>
            </table>

            <table class="meta-table">
                <tr>
                    <td class="meta-cell" style="padding-right: 20px;">
                        <div class="section-title">Billed To</div>
                        <div class="info-block">
                            <strong>%s</strong><br/>
                            %s<br/>
                            %s<br/>
                            %s, %s<br/>
                            %s
                        </div>
                    </td>
                    <td class="meta-cell" style="padding-left: 20px;">
                        <div class="section-title">Order Details</div>
                        <div class="info-block">
                            <strong>Order ID:</strong> #%d<br/>
                            <strong>Payment Method:</strong> %s<br/>
                            <strong>Payment Status:</strong> %s<br/>
                            <strong>Shipping Cost:</strong> ৳ %.2f
                        </div>
                    </td>
                </tr>
            </table>

            <table class="items-table">
                <thead>
                    <tr>
                        <th>Description</th>
                        <th class="center">Quantity</th>
                        <th class="right">Unit Price</th>
                        <th class="right">Total</th>
                    </tr>
                </thead>
                <tbody>
                    %s
                </tbody>
            </table>

            <table class="totals-table">
                <tr>
                    <td class="label">Subtotal:</td>
                    <td>৳ %.2f</td>
                </tr>
                <tr>
                    <td class="label">Shipping:</td>
                    <td>৳ %.2f</td>
                </tr>
                <tr class="grand-total">
                    <td class="label" style="color: #E91E63;">Total:</td>
                    <td>৳ %.2f</td>
                </tr>
            </table>

            <div class="footer">
                <strong>Thank you for shopping with BeautyHaat!</strong><br/>
                If you have any questions concerning this invoice, please contact notification@beautyhaat.com.<br/>
                https://beautyhaat.com
            </div>
        </body>
        </html>
        """,
                // 4. Inject variables into the HTML in the exact order they appear above
                invoice.getInvoiceNumber(),
                invoiceDate,

                // Customer Details
                order.getName(),
                order.getPhoneNumber(),
                order.getEmail() != null ? order.getEmail() : "N/A",
                order.getShippingAddress(), order.getArea(),
                order.getCity(),

                // Order Details
                order.getId(),
                order.getPaymentMethod().name(),
                order.getPaymentStatus().name(),
                invoice.getShippingAmount(),

                // Item Rows String
                itemsTableRows.toString(),

                // Totals
                invoice.getSubTotal(),
                invoice.getShippingAmount(),
                invoice.getTotalAmount()
        );

        // 5. Generate PDF
        try (java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream()) {
            com.openhtmltopdf.pdfboxout.PdfRendererBuilder builder = new com.openhtmltopdf.pdfboxout.PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlTemplate, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }
}