package com.booker.booking.repository;

import com.booker.booking.entity.ScheduleBreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScheduleBreakRepository extends JpaRepository<ScheduleBreak, Long> {

    @Query("SELECT sb FROM ScheduleBreak sb WHERE sb.scheduleRule.id = :ruleId ORDER BY sb.startTime")
    List<ScheduleBreak> findByScheduleRuleId(@Param("ruleId") Long ruleId);
}
