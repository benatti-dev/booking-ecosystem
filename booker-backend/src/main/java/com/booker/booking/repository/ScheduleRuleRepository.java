package com.booker.booking.repository;

import com.booker.booking.entity.ScheduleRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduleRuleRepository extends JpaRepository<ScheduleRule, Long> {

    @Query("SELECT sr FROM ScheduleRule sr WHERE sr.employee.id = :employeeId AND sr.dayOfWeek = :dayOfWeek")
    Optional<ScheduleRule> findByEmployeeIdAndDayOfWeek(@Param("employeeId") Long employeeId,
                                                         @Param("dayOfWeek") int dayOfWeek);

    @Query("SELECT sr FROM ScheduleRule sr WHERE sr.resource.id = :resourceId AND sr.dayOfWeek = :dayOfWeek")
    Optional<ScheduleRule> findByResourceIdAndDayOfWeek(@Param("resourceId") Long resourceId,
                                                         @Param("dayOfWeek") int dayOfWeek);

    @Query("SELECT sr FROM ScheduleRule sr WHERE sr.employee.id = :employeeId ORDER BY sr.dayOfWeek")
    List<ScheduleRule> findByEmployeeId(@Param("employeeId") Long employeeId);

    @Query("SELECT sr FROM ScheduleRule sr WHERE sr.resource.id = :resourceId ORDER BY sr.dayOfWeek")
    List<ScheduleRule> findByResourceId(@Param("resourceId") Long resourceId);
}
