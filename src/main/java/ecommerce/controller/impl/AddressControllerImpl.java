package ecommerce.controller.impl;

import ecommerce.controller.AddressController;
import ecommerce.dto.GenericResponseDto;
import ecommerce.dto.address.AddressRequestDto;
import ecommerce.dto.address.AddressResponseDto;
import ecommerce.service.AddressService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/address")
@RequiredArgsConstructor
public class AddressControllerImpl implements AddressController {
    private final AddressService addressService;
    @PostMapping("/add")
    @Override
    public ResponseEntity<?> add(@RequestBody @Valid AddressRequestDto requestDto, HttpServletRequest servletRequest) {
        AddressResponseDto responseDto = addressService.add(requestDto, servletRequest);
        return new ResponseEntity<>(GenericResponseDto.success("Address added successfully", responseDto, HttpStatus.OK.value()), HttpStatus.OK);
    }

    @Override
    @GetMapping("/all")
    public ResponseEntity<?> getAllByUserId(HttpServletRequest servletRequest) {
        List<AddressResponseDto> allByUser = addressService.getAllByUser(servletRequest);
        return new ResponseEntity<>(GenericResponseDto.success("Fetch successfully", allByUser, HttpStatus.OK.value()), HttpStatus.OK);
    }

    @PutMapping("/update")
    @Override
    public ResponseEntity<?> update(@RequestBody @Valid AddressRequestDto requestDto, HttpServletRequest servletRequest) {
        AddressResponseDto responseDto = addressService.update(requestDto, servletRequest);
        return new ResponseEntity<>(GenericResponseDto.success("Address updated successfully", responseDto, HttpStatus.OK.value()), HttpStatus.OK);
    }
}
