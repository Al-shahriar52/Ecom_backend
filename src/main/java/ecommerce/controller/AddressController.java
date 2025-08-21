package ecommerce.controller;

import ecommerce.dto.address.AddressRequestDto;
import ecommerce.dto.address.AddressResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface AddressController {

    ResponseEntity<?> add(AddressRequestDto requestDto, HttpServletRequest servletRequest);
    ResponseEntity<?> getAllByUserId(HttpServletRequest servletRequest);
    ResponseEntity<?> update(AddressRequestDto requestDto, HttpServletRequest servletRequest);
}
