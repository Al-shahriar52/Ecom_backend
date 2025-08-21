package ecommerce.service;

import ecommerce.dto.address.AddressRequestDto;
import ecommerce.dto.address.AddressResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface AddressService {
    AddressResponseDto add(AddressRequestDto request, HttpServletRequest servletRequest);
    List<AddressResponseDto> getAllByUser(HttpServletRequest servletRequest);
    AddressResponseDto update(AddressRequestDto request, HttpServletRequest servletRequest);
}
