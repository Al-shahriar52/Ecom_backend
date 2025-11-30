package ecommerce.service.impl;

import ecommerce.dto.address.AddressRequestDto;
import ecommerce.dto.address.AddressResponseDto;
import ecommerce.entity.Address;
import ecommerce.entity.User;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.repository.AddressRepository;
import ecommerce.service.AddressService;
import ecommerce.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final TokenUtil tokenUtil;
    @Override
    public AddressResponseDto add(AddressRequestDto request, HttpServletRequest servletRequest) {

        User user = tokenUtil.extractUserInfo(servletRequest);

        Address address = new Address();
        address.setAddressType(request.getAddressType());
        address.setCity(request.getCity());
        address.setArea(request.getArea());
        address.setAddress(request.getAddress());
        address.setUserId(user.getId());

        addressRepository.save(address);

        return AddressResponseDto.builder()
                .addressType(request.getAddressType())
                .city(request.getCity())
                .area(request.getArea())
                .address(request.getAddress())
                .build();
    }

    @Override
    public List<AddressResponseDto> getAllByUser(HttpServletRequest servletRequest) {
        User user = tokenUtil.extractUserInfo(servletRequest);
        return addressRepository.findAllByUserId(user.getId());
    }

    @Override
    public AddressResponseDto update(AddressRequestDto request, HttpServletRequest servletRequest) {
        User user = tokenUtil.extractUserInfo(servletRequest);

        if (request.getId() != null && user != null) {
            Address address = addressRepository.findByIdAndUserId(request.getId() , user.getId());
            if (address != null) {
                address.setAddressType(request.getAddressType());
                address.setCity(request.getCity());
                address.setArea(request.getArea());
                address.setAddress(request.getAddress());
                addressRepository.save(address);
            }else {
                throw new BadRequestException("You are able to update the address");
            }
        }else {
            throw new BadRequestException("Address id cannot be null");
        }


        return AddressResponseDto.builder()
                .addressType(request.getAddressType())
                .city(request.getCity())
                .area(request.getArea())
                .address(request.getAddress())
                .build();
    }
}
