package ecommerce.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import ecommerce.dto.ProductDto;
import ecommerce.dto.pageResponse.ProductResponse;
import ecommerce.entity.Product;
import ecommerce.exceptionHandling.ResourceNotFound;
import ecommerce.repository.ProductRepository;
import ecommerce.service.ProductService;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductServiceImpl implements ProductService {

    private final ModelMapper mapper;
    private final ProductRepository productRepository;

    public ProductServiceImpl(ModelMapper mapper, ProductRepository productRepository) {
        this.mapper = mapper;
        this.productRepository = productRepository;
    }

    @Override
    public ProductDto add(ProductDto productDto) {

        Product product = mapToEntity(productDto);

        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dgxol8iyp");
        config.put("api_key", "825365231133592");
        config.put("api_secret", "tsFvRKhylO2BZzkrqOlyFRsHwXw");
        Cloudinary cloudinary = new Cloudinary(config);

        try {
            cloudinary.uploader().upload("https://upload.wikimedia.org/wikipedia/commons/a/ae/Olympic_flag.jpg",
                    ObjectUtils.asMap("public_id", "cat_flag"));

        } catch (IOException exception) {
            System.out.println(exception.getMessage());
        }

        String url = cloudinary.url().transformation(new Transformation()
                        .width(100)
                        .height(150)
                        .crop("fill")).generate("cat_flag");

        product.setImage(url);
        Product data = productRepository.save(product);

        return mapToDto(data);
    }

    @Override
    public ProductDto getById(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(()->
                new ResourceNotFound("Product", "id", productId));

        return mapToDto(product);
    }

    @Override
    public String delete(Long productId) {

        Product product = productRepository.findById(productId).orElseThrow(()->
                new ResourceNotFound("Product", "id", productId));

        productRepository.delete(product);
        return "your product :"+product.getName()+" is successfully deleted.";
    }

    @Override
    public ProductDto update(Long productId, ProductDto productDto) {

        Product product = productRepository.findById(productId).orElseThrow(()->
                new ResourceNotFound("Product", "id", productId));

        product.setName(productDto.getName());
        product.setBrand(productDto.getBrand());
        product.setImage(productDto.getImage());
        product.setCategory(productDto.getCategory());
        product.setCountInStock(productDto.getCountInStock());
        product.setDescription(productDto.getDescription());
        product.setNumReviews(productDto.getNumReviews());
        product.setPrice(productDto.getPrice());
        product.setRating(productDto.getRating());

        productRepository.save(product);
        return mapToDto(product);
    }

    @Override
    public ProductResponse search(int pageNo, int pageSize, String sortBy, String query) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));
        Page<Product> products = productRepository.search(pageable, query);

        List<Product> productList = products.getContent();
        List<ProductDto> content = productList.stream().map((this::mapToDto)).toList();

        return getProductResult(content, products);
    }

    public ProductResponse getProductResult(List<ProductDto> content, Page<Product> products) {

        ProductResponse response = new ProductResponse();
        response.setContent(content);
        response.setPageNo(products.getNumber());
        response.setPageSize(products.getSize());
        response.setTotalPages(products.getTotalPages());
        response.setTotalElements(products.getTotalElements());
        response.setLast(products.isLast());

        return response;
    }

    private String uploadImage(MultipartFile imageFile) {
        return "/upload/" + imageFile.getOriginalFilename();
    }

    public Product mapToEntity(ProductDto productDto) {
        return mapper.map(productDto, Product.class);
    }

    public ProductDto mapToDto(Product product) {
        return mapper.map(product, ProductDto.class);
    }
}
