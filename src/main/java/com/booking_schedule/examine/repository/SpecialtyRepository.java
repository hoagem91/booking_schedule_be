package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.Specialty;
import com.booking_schedule.examine.enums.EStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Specialty> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Specialty s where s.id = :id")
    Optional<Specialty> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    Optional<Specialty> findByCode(String code);

    // Kiểm tra mã chuyên khoa đã tồn tại chưa (dùng khi create)
    boolean existsByCode(String code);

    // Kiểm tra mã trùng với specialty khác (dùng khi update)
    boolean existsByCodeAndIdNot(String code, Long id);

    // Kiểm tra tên trùng không phân biệt hoa thường (dùng khi create)
    boolean existsByNameIgnoreCase(String name);

    // Kiểm tra tên trùng với specialty khác không phân biệt hoa thường (dùng khi update)
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    // Lấy danh sách theo status (dùng cho public API, admin filter)
    List<Specialty> findByStatus(EStatus status);
}
