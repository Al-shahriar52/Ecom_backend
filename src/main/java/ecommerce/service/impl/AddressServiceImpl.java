package ecommerce.service.impl;

import ecommerce.dto.address.AddressRequestDto;
import ecommerce.dto.address.AddressResponseDto;
import ecommerce.entity.Address;
import ecommerce.entity.User;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.repository.AddressRepository;
import ecommerce.service.ActivityService;
import ecommerce.service.AddressService;
import ecommerce.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import added

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final TokenUtil tokenUtil;
    private final ActivityService activityService;

    @Override
    @Transactional // Ensures database consistency
    public AddressResponseDto add(AddressRequestDto request, HttpServletRequest servletRequest) {

        User user = tokenUtil.extractUserInfo(servletRequest);

        // Enforce single default rule
        if (request.isDefault()) {
            addressRepository.removeDefaultStatusForUser(user.getId());
        }

        Address address = new Address();
        address.setAddressType(request.getAddressType());
        address.setCity(request.getCity());
        address.setArea(request.getArea());
        address.setAddress(request.getAddress());
        address.setUserId(user.getId());
        address.setDefault(request.isDefault());

        addressRepository.save(address);
        activityService.logActivity(user.getId(), "Address added by user");

        return AddressResponseDto.builder()
                .addressType(request.getAddressType())
                .city(request.getCity())
                .area(request.getArea())
                .address(request.getAddress())
                .isDefault(request.isDefault())
                .build();
    }

    @Override
    public List<AddressResponseDto> getAllByUser(HttpServletRequest servletRequest) {
        User user = tokenUtil.extractUserInfo(servletRequest);
        return addressRepository.findAllByUserId(user.getId());
    }

    @Override
    @Transactional // Ensures database consistency
    public AddressResponseDto update(AddressRequestDto request, HttpServletRequest servletRequest) {
        User user = tokenUtil.extractUserInfo(servletRequest);

        if (request.getId() != null && user != null) {
            Address address = addressRepository.findByIdAndUserId(request.getId() , user.getId());
            if (address != null) {

                // Enforce single default rule
                if (request.isDefault()) {
                    addressRepository.removeDefaultStatusForUser(user.getId());
                }

                address.setAddressType(request.getAddressType());
                address.setCity(request.getCity());
                address.setArea(request.getArea());
                address.setAddress(request.getAddress());
                address.setDefault(request.isDefault()); // FIXED: This was missing in your original code!

                addressRepository.save(address);
                activityService.logActivity(user.getId(), "Address updated by user");
            } else {
                throw new BadRequestException("You are not able to update the address");
            }
        } else {
            throw new BadRequestException("Address id cannot be null");
        }

        return AddressResponseDto.builder()
                .addressType(request.getAddressType())
                .city(request.getCity())
                .area(request.getArea())
                .address(request.getAddress())
                .isDefault(request.isDefault())
                .build();
    }
}