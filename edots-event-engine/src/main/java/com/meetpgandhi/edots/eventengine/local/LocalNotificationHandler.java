package com.meetpgandhi.edots.eventengine.local;

import com.meetpgandhi.edots.eventengine.model.NotificationTriggerEvent;

public interface LocalNotificationHandler {
    void handleNotification(NotificationTriggerEvent event);
}
