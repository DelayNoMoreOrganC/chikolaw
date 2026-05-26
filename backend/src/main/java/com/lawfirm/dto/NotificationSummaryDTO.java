package com.lawfirm.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class NotificationSummaryDTO {
    private long unreadCount;
    private Map<String, Long> unreadByGroup = new LinkedHashMap<>();
}
