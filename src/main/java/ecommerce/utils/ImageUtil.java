package ecommerce.utils;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import ecommerce.entity.Product;
import ecommerce.entity.ProductImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageUtil {

    private final Cloudinary cloudinary;
    public String uploadFile(MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "auto",
                        "quality", "auto",
                        "fetch_format", "auto"     // Converts to modern formats (like WebP/AVIF if supported)
                ));
        return uploadResult.get("secure_url").toString();
    }

    public List<ProductImage> uploadFiles(List<MultipartFile> files, Product product) throws IOException {
        List<ProductImage> productImages = new ArrayList<>();
        for (MultipartFile file : files) {
            String url = uploadFile(file);
            ProductImage productImage = new ProductImage();
            productImage.setProduct(product);
            productImage.setImageUrl(url);
            productImages.add(productImage);
        }
        return productImages;
    }
}
