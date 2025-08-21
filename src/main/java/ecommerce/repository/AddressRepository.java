package ecommerce.repository;

import ecommerce.dto.address.AddressResponseDto;
import ecommerce.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    @Query("SELECT new ecommerce.dto.address.AddressResponseDto(a.id, a.addressType, a.city, a.area, a.address) " +
            "FROM Address a WHERE a.userId = :userId")
    List<AddressResponseDto> findAllByUserId(@Param("userId") Long userId);

    Address findByIdAndUserId(Long id, Long userId);
}
