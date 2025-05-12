package com.library_user.repository;

import com.library_user.model.entity.Borrowing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BorrowingRepository extends JpaRepository<Borrowing, UUID> {

    Page<Borrowing> findByUserId(UUID userId, Pageable pageable);
    Page<Borrowing> findByReturnDateIsNullAndDueDateBefore(LocalDate date, Pageable pageable);
    List<Borrowing> findAllByDueDateBeforeAndOverdueFalseAndReturnDateIsNull(LocalDate date);

}