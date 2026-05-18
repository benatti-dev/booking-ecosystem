package com.booker.booking.repository;

import com.booker.booking.entity.ScheduleOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleOverrideRepository extends JpaRepository<ScheduleOverride, Long> {

    @Query("SELECT so FROM ScheduleOverride so WHERE so.employee.id = :employeeId AND so.overrideDate = :date")
    Optional<ScheduleOverride> findByEmployeeIdAndDate(@Param("employeeId") Long employeeId,
                                                        @Param("date") LocalDate date);

    @Query("SELECT so FROM ScheduleOverride so WHERE so.resource.id = :resourceId AND so.overrideDate = :date")
    Optional<ScheduleOverride> findByResourceIdAndDate(@Param("resourceId") Long resourceId,
                                                        @Param("date") LocalDate date);

    @Query("SELECT so FROM ScheduleOverride so WHERE so.employee.id = :employeeId AND so.overrideDate >= :from ORDER BY so.overrideDate")
    List<ScheduleOverride> findUpcomingByEmployeeId(@Param("employeeId") Long employeeId,
                                                     @Param("from") LocalDate from);

    @Query("SELECT so FROM ScheduleOverride so WHERE so.resource.id = :resourceId AND so.overrideDate >= :from ORDER BY so.overrideDate")
    List<ScheduleOverride> findUpcomingByResourceId(@Param("resourceId") Long resourceId,
                                                     @Param("from") LocalDate from);
}
