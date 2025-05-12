package com.library_user.scheduler;

import com.library_user.model.entity.Borrowing;
import com.library_user.repository.BorrowingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class OverdueChecker {

    private final BorrowingRepository borrowingRepository;

    public OverdueChecker(BorrowingRepository borrowingRepository) {
        this.borrowingRepository = borrowingRepository;
    }

    @Scheduled(cron = "0 0 * * * ?")// every minute
    public void markOverdueBooks() {
        LocalDate today = LocalDate.now();
        List<Borrowing> overdueBorrowings = borrowingRepository.findAllByDueDateBeforeAndOverdueFalseAndReturnDateIsNull(today);
        for (Borrowing borrowing : overdueBorrowings) {
            borrowing.setOverdue(true);
        }
        borrowingRepository.saveAll(overdueBorrowings);
    }
}