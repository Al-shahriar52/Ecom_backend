package ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "role_matrices")
public class RoleMatrix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Maps directly to your fixed Enum
    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private Role role; 

    // Stores the permission matrix array like [1, 1, 1, 0, 1, 1, 1, 0] from the frontend
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permission_toggles", joinColumns = @JoinColumn(name = "matrix_id"))
    @Column(name = "is_granted")
    @OrderColumn(name = "permission_index") // Ensures the frontend array order stays exact
    private List<Integer> permissions = new ArrayList<>();
}