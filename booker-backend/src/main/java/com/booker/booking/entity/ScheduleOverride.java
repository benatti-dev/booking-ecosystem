package com.booker.booking.entity;

import com.booker.business.entity.BookableResource;
import com.booker.business.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "schedule_overrides")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private BookableResource resource;

    @Column(name = "override_date", nullable = false)
    private LocalDate overrideDate;

    /** null = full day off */
    @Column(name = "start_time")
    private LocalTime startTime;

    /** null = full day off */
    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(length = 255)
    private String reason;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduleOverride other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
