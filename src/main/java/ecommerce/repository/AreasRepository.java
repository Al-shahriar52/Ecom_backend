package ecommerce.repository;

import ecommerce.dto.location.LocationResponseDto;
import ecommerce.entity.Areas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AreasRepository extends JpaRepository<Areas, Long> {

    @Query("select new ecommerce.dto.location.LocationResponseDto(a.id, a.name) from Areas a where a.cityId=:cityId")
    List<LocationResponseDto> findAllByCityId(@Param("cityId") Long cityId);
}
