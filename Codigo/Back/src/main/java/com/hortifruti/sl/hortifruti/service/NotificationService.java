package com.hortifruti.sl.hortifruti.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final SimpMessagingTemplate messagingTemplate;

  @Value("${topic.destination}")
  private String topicDestination;

  public void sendNotification(String message) {
    messagingTemplate.convertAndSend(topicDestination, message);
  }
}
