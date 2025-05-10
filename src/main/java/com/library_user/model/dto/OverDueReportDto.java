package com.library_user.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OverDueReportDto {
    private UUID userId;
    private UUID bookId;
    private LocalDate dueDate;
    private long daysOverdue;
}
