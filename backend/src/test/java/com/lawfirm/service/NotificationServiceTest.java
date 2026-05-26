package com.lawfirm.service;

import com.lawfirm.dto.NotificationSummaryDTO;
import com.lawfirm.entity.Notification;
import com.lawfirm.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void sendApprovalPending_setsCategoryGroup() {
        notificationService.sendApprovalPendingNotification(2L, 10L, "用印申请");

        ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(cap.capture());
        assertEquals(NotificationService.GROUP_APPROVAL, cap.getValue().getCategoryGroup());
        assertEquals(2L, cap.getValue().getReceiverId());
    }

    @Test
    void getSummary_aggregatesByGroup() {
        when(notificationRepository.countByReceiverIdAndIsReadFalse(1L)).thenReturn(5L);
        when(notificationRepository.countByReceiverIdAndCategoryGroupAndIsReadFalse(1L, "TODO"))
                .thenReturn(2L);
        when(notificationRepository.countByReceiverIdAndCategoryGroupAndIsReadFalse(1L, "CASE"))
                .thenReturn(1L);
        when(notificationRepository.countByReceiverIdAndCategoryGroupAndIsReadFalse(1L, "CALENDAR"))
                .thenReturn(0L);
        when(notificationRepository.countByReceiverIdAndCategoryGroupAndIsReadFalse(1L, "APPROVAL"))
                .thenReturn(2L);
        when(notificationRepository.countByReceiverIdAndCategoryGroupAndIsReadFalse(1L, "SYSTEM"))
                .thenReturn(0L);

        NotificationSummaryDTO summary = notificationService.getSummary(1L);
        assertEquals(5L, summary.getUnreadCount());
        assertEquals(2L, summary.getUnreadByGroup().get("TODO"));
        assertEquals(2L, summary.getUnreadByGroup().get("APPROVAL"));
    }
}
