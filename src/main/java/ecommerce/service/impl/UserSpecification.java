package ecommerce.service.impl;

import ecommerce.entity.AccountState;
import ecommerce.entity.Role;
import ecommerce.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    public static Specification<User> filterUsers(String search, String roleStr, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), term);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), term);
                Predicate phoneMatch = cb.like(cb.lower(root.get("phone")), term);
                predicates.add(cb.or(nameMatch, emailMatch, phoneMatch));
            }

            if (roleStr != null && !roleStr.equalsIgnoreCase("all")) {
                try {
                    Role roleEnum = Role.valueOf(roleStr.toUpperCase());
                    predicates.add(cb.isMember(roleEnum, root.get("roles")));
                } catch (IllegalArgumentException ignored) {}
            }

            if (status != null && !status.equalsIgnoreCase("all")) {
                try {
                    // Replace 'AccountState' below with your actual Status Enum class name
                    AccountState statusEnum = AccountState.valueOf(status.toUpperCase());
                    predicates.add(cb.equal(root.get("accountState"), statusEnum));
                } catch (IllegalArgumentException ignored) {}
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}