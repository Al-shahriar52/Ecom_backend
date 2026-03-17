package ecommerce.service.impl;

import ecommerce.entity.Delivery;
import ecommerce.entity.Invoice;
import ecommerce.entity.Order;
import ecommerce.enums.DeliveryStatus;
import ecommerce.enums.OrderStatus;
import ecommerce.enums.PaymentMethod;
import ecommerce.enums.PaymentStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderSpecification {

    public static Specification<Order> getFilteredOrders(
            String search, String paymentMethod, String paymentStatus, 
            String orderStatus, String deliveryStatus, 
            LocalDateTime startDate, LocalDateTime endDate) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // We need to join Invoice and Delivery to filter/search by their fields
            Join<Order, Invoice> invoiceJoin = root.join("invoice", JoinType.LEFT);
            Join<Order, Delivery> deliveryJoin = root.join("delivery", JoinType.LEFT);

            // 1. Search (Matches Invoice Number, Customer Name, or Phone)
            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate matchInvoice = criteriaBuilder.like(criteriaBuilder.lower(invoiceJoin.get("invoiceNumber")), searchPattern);
                Predicate matchName = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchPattern);
                Predicate matchPhone = criteriaBuilder.like(root.get("phoneNumber"), searchPattern);
                predicates.add(criteriaBuilder.or(matchInvoice, matchName, matchPhone));
            }

            // 2. Exact Match Filters (Ignoring "All")
            if (StringUtils.hasText(paymentMethod) && !paymentMethod.equalsIgnoreCase("All")) {
                try {
                    predicates.add(criteriaBuilder.equal(root.get("paymentMethod"), PaymentMethod.valueOf(paymentMethod)));
                } catch (IllegalArgumentException e) {
                    // Ignore if frontend sends an invalid enum string
                }
            }

            if (StringUtils.hasText(paymentStatus) && !paymentStatus.equalsIgnoreCase("All")) {
                try {
                    predicates.add(criteriaBuilder.equal(root.get("paymentStatus"), PaymentStatus.valueOf(paymentStatus)));
                } catch (IllegalArgumentException ignored) {}
            }

            if (StringUtils.hasText(orderStatus) && !orderStatus.equalsIgnoreCase("All")) {
                try {
                    predicates.add(criteriaBuilder.equal(root.get("orderStatus"), OrderStatus.valueOf(orderStatus)));
                } catch (IllegalArgumentException ignored) {}
            }

            if (StringUtils.hasText(deliveryStatus) && !deliveryStatus.equalsIgnoreCase("All")) {
                try {
                    predicates.add(criteriaBuilder.equal(deliveryJoin.get("deliveryStatus"), DeliveryStatus.valueOf(deliveryStatus)));
                } catch (IllegalArgumentException ignored) {}
            }

            // 3. Date Range
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}