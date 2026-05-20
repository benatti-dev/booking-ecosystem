package com.booker.booking.repository;

import com.booker.booking.entity.ScheduleBreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScheduleBreakRepository extends JpaRepository<ScheduleBreak, Long> {

    @Query("SELECT sb FROM ScheduleBreak sb WHERE sb.scheduleRule.id = :ruleId ORDER BY sb.startTime")
    List<ScheduleBreak> findByScheduleRuleId(@Param("ruleId") Long ruleId);

    /**
     * Batch-fetches all breaks for the given list of rule IDs in a single query,
     * preventing N+1 when converting multiple ScheduleRules to DTOs.
     */
    @Query("SELECT sb FROM ScheduleBreak sb WHERE sb.scheduleRule.id IN :ruleIds ORDER BY sb.scheduleRule.id, sb.startTime")
    List<ScheduleBreak> findByScheduleRuleIdIn(@Param("ruleIds") List<Long> ruleIds);
}
