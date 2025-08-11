package ecommerce.exceptionHandling;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceNotFound extends RuntimeException {

    private String resourceName;
    private String fieldName;
    private Long value;

    public ResourceNotFound(String s) {
        super(s);
    }

    public ResourceNotFound(String resourceName, String fieldName, Long value) {

        super(String.format("%s not found with %s is : %s", resourceName, fieldName, value));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.value = value;
    }
}
