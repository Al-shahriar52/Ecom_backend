package ecommerce.utils;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import ecommerce.entity.Product;
import ecommerce.entity.ProductImage;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageUtil {

    private final Cloudinary cloudinary;

    /**
     * Compresses the image to a maximum of 80% quality and 1920x1080 resolution
     */
    private byte[] compressImage(byte[] imageBytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Thumbnails.of(bais)
                .size(1920, 1080)
                .outputQuality(0.8)
                .toOutputStream(baos);

        return baos.toByteArray();
    }

    public String uploadFile(MultipartFile file) throws IOException {
        // Compress image before uploading
        byte[] compressedImageBytes = compressImage(file.getBytes());

        Map<String, Object> uploadResult = cloudinary.uploader().upload(compressedImageBytes,
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
