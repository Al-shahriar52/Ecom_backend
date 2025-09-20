package ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class BrandMenuData {

    @JsonProperty("topBrands")
    private List<TopBrand> topBrands;

    @JsonProperty("allBrands")
    private List<AllBrand> allBrands;
}