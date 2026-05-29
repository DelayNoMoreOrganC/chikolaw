package com.lawfirm.service;

import com.lawfirm.entity.Calendar;
import com.lawfirm.entity.Case;
import com.lawfirm.repository.CalendarRepository;
import com.lawfirm.repository.CaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseCalendarSyncServiceTest {

    @Mock
    private CalendarRepository calendarRepository;
    @Mock
    private CaseRepository caseRepository;

    @InjectMocks
    private CaseCalendarSyncService syncService;

    @Test
    void syncFromCase_createsHearingCalendar() {
        Case c = new Case();
        c.setId(10L);
        c.setOwnerId(1L);
        c.setCaseName("测试案");
        c.setHearingDate(LocalDate.of(2026, 6, 1));

        when(calendarRepository.findFirstByCaseIdAndSyncSourceAndDeletedFalse(
                eq(10L), eq(CaseCalendarSyncService.SYNC_HEARING)))
                .thenReturn(Optional.empty());
        when(calendarRepository.findFirstByCaseIdAndSyncSourceAndDeletedFalse(
                eq(10L), eq(CaseCalendarSyncService.SYNC_DEADLINE)))
                .thenReturn(Optional.empty());
        when(calendarRepository.save(any(Calendar.class))).thenAnswer(inv -> inv.getArgument(0));

        syncService.syncFromCase(c, 1L);

        ArgumentCaptor<Calendar> cap = ArgumentCaptor.forClass(Calendar.class);
        verify(calendarRepository, org.mockito.Mockito.atLeastOnce()).save(cap.capture());
        Calendar saved = cap.getAllValues().stream()
                .filter(cal -> CaseCalendarSyncService.SYNC_HEARING.equals(cal.getSyncSource()))
                .findFirst()
                .orElse(null);
        assertNotNull(saved);
        assertEquals("HEARING", saved.getCalendarType());
        assertEquals(LocalDateTime.of(2026, 6, 1, 9, 0), saved.getStartTime());
    }

    @Test
    void syncFromCalendar_updatesCaseHearingDate() {
        Calendar cal = new Calendar();
        cal.setId(5L);
        cal.setCaseId(10L);
        cal.setCalendarType("hearing");
        cal.setStartTime(LocalDateTime.of(2026, 7, 15, 10, 0));
        cal.setEndTime(LocalDateTime.of(2026, 7, 15, 11, 0));

        Case c = new Case();
        c.setId(10L);
        when(caseRepository.findById(10L)).thenReturn(Optional.of(c));
        when(caseRepository.save(any(Case.class))).thenAnswer(inv -> inv.getArgument(0));

        syncService.syncFromCalendar(cal);

        ArgumentCaptor<Case> caseCap = ArgumentCaptor.forClass(Case.class);
        verify(caseRepository).save(caseCap.capture());
        assertEquals(LocalDate.of(2026, 7, 15), caseCap.getValue().getHearingDate());
    }

    @Test
    void syncFromCase_createsDeadlineCalendar() {
        Case c = new Case();
        c.setId(11L);
        c.setOwnerId(1L);
        c.setCaseName("审限案");
        c.setDeadlineDate(LocalDate.of(2026, 8, 20));

        when(calendarRepository.findFirstByCaseIdAndSyncSourceAndDeletedFalse(
                eq(11L), eq(CaseCalendarSyncService.SYNC_HEARING)))
                .thenReturn(Optional.empty());
        when(calendarRepository.findFirstByCaseIdAndSyncSourceAndDeletedFalse(
                eq(11L), eq(CaseCalendarSyncService.SYNC_DEADLINE)))
                .thenReturn(Optional.empty());
        when(calendarRepository.save(any(Calendar.class))).thenAnswer(inv -> inv.getArgument(0));

        syncService.syncFromCase(c, 1L);

        ArgumentCaptor<Calendar> cap = ArgumentCaptor.forClass(Calendar.class);
        verify(calendarRepository, org.mockito.Mockito.atLeastOnce()).save(cap.capture());
        Calendar deadline = cap.getAllValues().stream()
                .filter(cal -> CaseCalendarSyncService.SYNC_DEADLINE.equals(cal.getSyncSource()))
                .findFirst()
                .orElse(null);
        assertNotNull(deadline);
        assertEquals("DEADLINE", deadline.getCalendarType());
    }

    @Test
    void onCalendarDeleted_clearsMatchingHearingDate() {
        Calendar cal = new Calendar();
        cal.setCaseId(12L);
        cal.setSyncSource(CaseCalendarSyncService.SYNC_HEARING);
        cal.setCalendarType("HEARING");
        cal.setStartTime(LocalDateTime.of(2026, 9, 1, 9, 0));

        Case c = new Case();
        c.setId(12L);
        c.setHearingDate(LocalDate.of(2026, 9, 1));
        when(caseRepository.findById(12L)).thenReturn(Optional.of(c));
        when(caseRepository.save(any(Case.class))).thenAnswer(inv -> inv.getArgument(0));

        syncService.onCalendarDeleted(cal);

        ArgumentCaptor<Case> caseCap = ArgumentCaptor.forClass(Case.class);
        verify(caseRepository).save(caseCap.capture());
        assertNull(caseCap.getValue().getHearingDate());
    }
}
