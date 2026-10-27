package ecommerce.service.impl;

import ecommerce.dto.admin.create.AdminCreateOrderRequest;
import ecommerce.entity.*;
import ecommerce.enums.InvoiceStatus;
import ecommerce.enums.OrderStatus;
import ecommerce.enums.PaymentMethod;
import ecommerce.enums.PaymentStatus;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.repository.*;
import ecommerce.service.ActivityService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/** Lets an admin look up customers and products and place an order on a customer's behalf. */
@Service
@RequiredArgsConstructor
public class AdminOrderCreateService {

    private static final Logger log = LoggerFactory.getLogger(AdminOrderCreateService.class);
    private static final Pattern PHONE = Pattern.compile("^01[3-9]\\d{8}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final EntityManager em;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InvoiceRepository invoiceRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final ActivityService activityService;

    // ------------------------------------------------------------------ lookups

    @Transactional
    public List<Map<String, Object>> searchCustomers(String q) {
        if (q == null || q.trim().length() < 2) return List.of();
        String term = q.trim();
        List<Map<String, Object>> out = new ArrayList<>();
        for (User u : userRepository.search(PageRequest.of(0, 8), term).getContent()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("email", visibleEmail(u.getEmail()));
            m.put("phone", normalizePhone(u.getPhone()));
            m.put("orders", u.getOrders() == null ? 0 : u.getOrders());
            m.put("registered", Boolean.TRUE.equals(u.getStatus()));
            m.put("joinedAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);

            List<Map<String, Object>> addrs = new ArrayList<>();
            for (Address a : addressRepository.findByUserId(u.getId())) {
                Map<String, Object> am = new LinkedHashMap<>();
                am.put("id", a.getId());
                am.put("type", a.getAddressType() != null ? a.getAddressType().name() : "HOME");
                am.put("address", a.getAddress());
                am.put("city", a.getCity());
                am.put("area", a.getArea());
                am.put("isDefault", a.isDefault());
                addrs.add(am);
            }
            m.put("addresses", addrs);

            List<Order> last = orderRepository.findAllByUserOrderByCreatedAtDesc(u, PageRequest.of(0, 1)).getContent();
            if (!last.isEmpty()) {
                Order o = last.get(0);
                Map<String, Object> lm = new LinkedHashMap<>();
                lm.put("address", o.getShippingAddress());
                lm.put("city", o.getCity());
                lm.put("area", o.getArea());
                lm.put("phone", normalizePhone(o.getPhoneNumber()));
                lm.put("name", o.getName());
                m.put("lastOrder", lm);
            }
            out.add(m);
        }
        return out;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchProducts(String q) {
        if (q == null || q.trim().isEmpty()) return List.of();
        String[] tokens = q.trim().toLowerCase().split("\\s+");
        StringBuilder jpql = new StringBuilder(
                "select distinct p from Product p left join p.brand b where (p.status is null or p.status <> ecommerce.entity.ProductStatus.DRAFT)");
        for (int i = 0; i < tokens.length; i++) {
            jpql.append(" and (lower(p.name) like :t").append(i)
                    .append(" or lower(p.sku) like :t").append(i)
                    .append(" or lower(b.name) like :t").append(i).append(")");
        }
        jpql.append(" order by p.name");
        var query = em.createQuery(jpql.toString(), Product.class);
        for (int i = 0; i < tokens.length; i++) query.setParameter("t" + i, "%" + tokens[i] + "%");
        query.setMaxResults(12);

        List<Map<String, Object>> out = new ArrayList<>();
        for (Product p : query.getResultList()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("sku", p.getSku());
            m.put("brand", p.getBrand() != null ? p.getBrand().getName() : null);
            m.put("price", p.getDiscountedPrice());
            m.put("mrp", p.getOriginalPrice());
            m.put("stock", p.getQuantity());
            String img = null;
            if (p.getImageUrls() != null && !p.getImageUrls().isEmpty()) img = p.getImageUrls().get(0).getImageUrl();
            m.put("image", img);
            LinkedHashSet<String> sizes = new LinkedHashSet<>();
            if (p.getVariations() != null) {
                for (Variation v : p.getVariations()) {
                    if (v.getSize() != null && !v.getSize().isBlank()) sizes.add(v.getSize().trim());
                }
            }
            m.put("size", String.join(" / ", sizes));
            out.add(m);
        }
        return out;
    }

    // ------------------------------------------------------------------ create

    @Transactional
    public Map<String, Object> createOrder(AdminCreateOrderRequest req) {
        // ---- validate input
        if (req.getItems() == null || req.getItems().isEmpty()) throw new BadRequestException("Add at least one product.");
        String name = trim(req.getName());
        if (name.isEmpty()) throw new BadRequestException("Customer name is required.");
        String phone = normalizePhone(req.getPhone());
        if (phone == null || !PHONE.matcher(phone).matches()) throw new BadRequestException("Enter a valid Bangladeshi mobile number (01XXXXXXXXX).");
        String email = trim(req.getEmail());
        if (!email.isEmpty() && !EMAIL.matcher(email).matches()) throw new BadRequestException("Email address is not valid.");
        String street = trim(req.getShippingAddress());
        String city = trim(req.getCity());
        String area = trim(req.getArea());
        if (street.isEmpty() || city.isEmpty() || area.isEmpty()) throw new BadRequestException("Street address, city and area are required.");
        PaymentMethod method = req.getPaymentMethod() != null ? req.getPaymentMethod() : PaymentMethod.COD;
        boolean paid = req.getPaymentStatus() == PaymentStatus.PAID;
        OrderStatus status = req.getOrderStatus() == OrderStatus.PENDING ? OrderStatus.PENDING : OrderStatus.CONFIRMED;

        // ---- customer
        boolean created = false;
        User customer;
        if (req.getCustomerId() != null) {
            customer = userRepository.findById(req.getCustomerId())
                    .orElseThrow(() -> new BadRequestException("Customer not found."));
        } else {
            Optional<User> byPhone = findByPhone(phone);
            Optional<User> byEmail = email.isEmpty() ? Optional.empty() : userRepository.findByEmail(email);
            if (byPhone.isPresent() && byEmail.isPresent() && !byPhone.get().getId().equals(byEmail.get().getId())) {
                throw new BadRequestException("This phone and email belong to two different customers. Pick the right customer from the search box.");
            }
            if (byPhone.isPresent()) customer = byPhone.get();
            else if (byEmail.isPresent()) customer = byEmail.get();
            else {
                User u = new User();
                u.setName(name);
                u.setPhone(phone);
                u.setEmail(email.isEmpty() ? null : email);
                u.setPassword(UUID.randomUUID().toString());
                u.setStatus(false);
                customer = userRepository.save(u);
                created = true;
            }
        }

        // ---- items (merge duplicate products)
        Map<Long, AdminCreateOrderRequest.Item> merged = new LinkedHashMap<>();
        for (AdminCreateOrderRequest.Item it : req.getItems()) {
            if (it.getProductId() == null || it.getQuantity() < 1) throw new BadRequestException("Every product needs a quantity of at least 1.");
            if (it.getUnitPrice() != null && it.getUnitPrice() < 0) throw new BadRequestException("Price cannot be negative.");
            AdminCreateOrderRequest.Item ex = merged.get(it.getProductId());
            if (ex == null) merged.put(it.getProductId(), it);
            else ex.setQuantity(ex.getQuantity() + it.getQuantity());
        }

        Order order = new Order();
        order.setUser(customer);
        order.setName(name);
        order.setPhoneNumber(phone);
        order.setEmail(email.isEmpty() ? null : email);
        order.setShippingAddress(street);
        order.setCity(city);
        order.setArea(area);
        order.setOrderNote(trim(req.getOrderNote()).isEmpty() ? null : trim(req.getOrderNote()));
        order.setPaymentMethod(method);

        double subTotalMrp = 0, subTotal = 0;
        List<OrderItem> orderItems = new ArrayList<>();
        for (AdminCreateOrderRequest.Item it : merged.values()) {
            Product p = productRepository.findById(it.getProductId())
                    .orElseThrow(() -> new BadRequestException("Product not found: " + it.getProductId()));
            if (p.getQuantity() < it.getQuantity()) {
                throw new BadRequestException("Not enough stock for " + p.getName() + " (only " + p.getQuantity() + " left).");
            }
            p.setQuantity(p.getQuantity() - it.getQuantity());
            productRepository.save(p);

            double unit = it.getUnitPrice() != null ? it.getUnitPrice() : p.getDiscountedPrice();
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(p);
            oi.setQuantity(it.getQuantity());
            oi.setPrice(unit);
            orderItems.add(oi);
            subTotalMrp += Math.max(p.getOriginalPrice(), unit) * it.getQuantity();
            subTotal += unit * it.getQuantity();
        }

        double shipping = req.getShippingCost() != null ? req.getShippingCost() : (city.equalsIgnoreCase("Dhaka") ? 60.0 : 120.0);
        if (shipping < 0) throw new BadRequestException("Delivery charge cannot be negative.");
        double discount = req.getDiscount() != null ? req.getDiscount() : 0.0;
        if (discount < 0) throw new BadRequestException("Discount cannot be negative.");
        if (discount > subTotal) throw new BadRequestException("Discount cannot be more than the products total.");
        double total = Math.max(0, subTotal - discount) + shipping;

        order.setShippingCost(shipping);
        order.setDiscountAmount(discount);
        order.setTotalAmount(total);
        order.setOrderItems(orderItems);
        order.setOrderStatus(status);
        LocalDateTime now = LocalDateTime.now();
        if (status == OrderStatus.CONFIRMED) order.setConfirmedAt(now);
        order.setPaymentStatus(paid ? PaymentStatus.PAID : PaymentStatus.PENDING);
        if (paid) order.setPaidAt(now);

        Order saved = orderRepository.save(order);

        Invoice inv = new Invoice();
        inv.setOrder(saved);
        inv.setInvoiceNumber("INV-" + (10000 + saved.getId()));
        inv.setSubTotalMrp(subTotalMrp);
        inv.setProductSavings(Math.max(0, subTotalMrp - subTotal));
        inv.setDiscountedSubTotal(subTotal);
        inv.setCouponDiscountAmount(discount);
        inv.setSubTotal(subTotal);
        inv.setShippingAmount(shipping);
        inv.setDiscountAmount(discount);
        inv.setTaxAmount(0.0);
        inv.setTotalAmount(total);
        inv.setIssuedAt(now);
        inv.setStatus(paid ? InvoiceStatus.PAID : InvoiceStatus.UNPAID);
        if (paid) inv.setPaidAt(now);
        inv.setDueDate(method == PaymentMethod.COD ? now.plusDays(7) : now.plusHours(24));
        invoiceRepository.save(inv);

        // ---- address book
        if (req.isSaveAddress()) {
            try {
                boolean exists = addressRepository.findByUserId(customer.getId()).stream()
                        .anyMatch(a -> street.equalsIgnoreCase(trim(a.getAddress())) && city.equalsIgnoreCase(trim(a.getCity())) && area.equalsIgnoreCase(trim(a.getArea())));
                if (!exists) {
                    Address a = new Address();
                    a.setAddressType(AddressType.HOME);
                    a.setAddress(street);
                    a.setCity(city);
                    a.setArea(area);
                    a.setUserId(customer.getId());
                    a.setDefault(false);
                    addressRepository.save(a);
                }
            } catch (Exception e) {
                log.warn("Could not save address for customer {}: {}", customer.getId(), e.getMessage());
            }
        }

        // ---- notify: the same confirmation email + SMS a customer gets when they order on the website
        boolean emailSent = false, smsSent = false;
        if (req.isNotifyCustomer()) {
            String to = saved.getEmail();
            if (to == null || to.isBlank()) to = visibleEmail(customer.getEmail());
            if (to != null && !to.isBlank() && !saved.getOrderItems().isEmpty()) {
                try {
                    if (saved.getEmail() == null || saved.getEmail().isBlank()) saved.setEmail(to);
                    emailService.sendOrderConfirmationEmail(customer, saved, inv);
                    emailSent = true;
                } catch (Exception e) { log.warn("Order email failed for {}: {}", inv.getInvoiceNumber(), e.getMessage()); }
            }
            try {
                smsService.sendOrderConfirmationSms(saved.getPhoneNumber(), inv.getInvoiceNumber(), saved.getTotalAmount());
                smsSent = true;
            } catch (Exception e) { log.warn("Order SMS failed for {}: {}", inv.getInvoiceNumber(), e.getMessage()); }
        }

        try {
            activityService.logActivity(customer.getId(), "Admin placed order ID: " + saved.getId() + " with total amount: " + total);
        } catch (Exception ignored) { }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("orderId", saved.getId());
        res.put("invoiceNumber", inv.getInvoiceNumber());
        res.put("totalAmount", total);
        res.put("customerCreated", created);
        res.put("emailSent", emailSent);
        res.put("smsSent", smsSent);
        return res;
    }

    // ------------------------------------------------------------------ helpers

    private Optional<User> findByPhone(String normalized) {
        for (String candidate : new String[]{normalized, "+88" + normalized, "88" + normalized, "0088" + normalized}) {
            Optional<User> u = userRepository.findByPhone(candidate);
            if (u.isPresent()) return u;
        }
        return Optional.empty();
    }

    /** 01XXXXXXXXX form, whatever prefix the number was typed or stored with. */
    static String normalizePhone(String raw) {
        if (raw == null) return null;
        String p = raw.replaceAll("[\\s\\-()]", "");
        if (p.startsWith("+88")) p = p.substring(3);
        else if (p.startsWith("0088")) p = p.substring(4);
        else if (p.startsWith("88") && p.length() == 13) p = p.substring(2);
        return p;
    }

    private static String visibleEmail(String e) {
        return (e == null || e.endsWith("@beautyhaat.internal")) ? "" : e;
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }
}
