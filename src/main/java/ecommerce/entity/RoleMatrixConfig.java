package ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "role_matrix_config")
@Data
public class RoleMatrixConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // We store the entire JSON object here to easily support dynamic properties
    @Column(columnDefinition = "TEXT")
    private String configData; 
}