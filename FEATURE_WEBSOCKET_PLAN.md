# 🔔 Real-time Notifications với WebSocket

## 🎯 Mục tiêu

User và Admin nhận thông báo real-time khi có sự kiện quan trọng (không cần refresh page).

## 📋 Use Cases

### Admin nhận thông báo khi:

- ✅ User đặt trước sách mới
- ✅ Sách sắp hết hạn trả (1 ngày trước)
- ✅ User mới đăng ký
- ✅ Review mới cần duyệt

### User nhận thông báo khi:

- ✅ Sách đặt trước đã sẵn sàng
- ✅ Sách sắp hết hạn (2 ngày trước)
- ✅ Admin duyệt review của mình
- ✅ Có người comment vào review

## 🛠️ Tech Stack

**Backend (Spring Boot):**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

**Frontend (Angular):**

```bash
npm install @stomp/rx-stomp @stomp/stompjs --save
```

## 📝 Implementation Steps

### Step 1: Backend WebSocket Config (30 phút)

**File:** `lms-backend/src/main/java/com/ibizabroker/lms/configuration/WebSocketConfig.java`

```java
package com.ibizabroker.lms.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
```

### Step 2: Notification Service (20 phút)

**File:** `lms-backend/src/main/java/com/ibizabroker/lms/service/NotificationService.java`

```java
package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(Integer userId, String message, String type) {
        NotificationDto notification = NotificationDto.builder()
                .message(message)
                .type(type)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/user/" + userId, notification);
    }

    public void sendToAllAdmins(String message, String type) {
        NotificationDto notification = NotificationDto.builder()
                .message(message)
                .type(type)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/admin", notification);
    }
}
```

### Step 3: Trigger Notifications (10 phút)

**Update CirculationService.java:**

```java
// Khi user mượn sách
public LoanDto borrowBook(Integer bookId, Integer userId) {
    // ... existing code ...

    // Gửi thông báo cho admin
    notificationService.sendToAllAdmins(
        "User " + user.getName() + " vừa mượn sách: " + book.getName(),
        "BORROW"
    );

    return loanDto;
}
```

### Step 4: Frontend WebSocket Service (30 phút)

**File:** `lms-frontend/src/app/services/websocket.service.ts`

```typescript
import { Injectable } from "@angular/core";
import { RxStomp } from "@stomp/rx-stomp";
import { Observable } from "rxjs";

@Injectable({
  providedIn: "root",
})
export class WebSocketService {
  private rxStomp = new RxStomp();

  constructor() {
    this.rxStomp.configure({
      brokerURL: "ws://localhost:8080/ws",
      connectHeaders: {},
      heartbeatIncoming: 0,
      heartbeatOutgoing: 20000,
      reconnectDelay: 5000,
    });

    this.rxStomp.activate();
  }

  watchUserNotifications(userId: number): Observable<any> {
    return this.rxStomp.watch(`/topic/user/${userId}`);
  }

  watchAdminNotifications(): Observable<any> {
    return this.rxStomp.watch("/topic/admin");
  }
}
```

### Step 5: UI Notification Component (40 phút)

**File:** `lms-frontend/src/app/shared/notification-toast/notification-toast.component.ts`

```typescript
import { Component, OnInit } from "@angular/core";
import { WebSocketService } from "../../services/websocket.service";

@Component({
  selector: "app-notification-toast",
  template: `
    <div class="notification-container">
      <div
        *ngFor="let notif of notifications"
        class="toast show"
        [class.bg-success]="notif.type === 'SUCCESS'"
        [class.bg-warning]="notif.type === 'WARNING'"
      >
        <div class="toast-body">
          {{ notif.message }}
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .notification-container {
        position: fixed;
        top: 70px;
        right: 20px;
        z-index: 9999;
      }
      .toast {
        min-width: 300px;
        margin-bottom: 10px;
      }
    `,
  ],
})
export class NotificationToastComponent implements OnInit {
  notifications: any[] = [];

  constructor(private wsService: WebSocketService) {}

  ngOnInit() {
    const userId = this.getCurrentUserId();

    this.wsService.watchUserNotifications(userId).subscribe((message) => {
      this.showNotification(JSON.parse(message.body));
    });
  }

  showNotification(notif: any) {
    this.notifications.push(notif);
    setTimeout(() => {
      this.notifications.shift();
    }, 5000);
  }

  getCurrentUserId(): number {
    return JSON.parse(localStorage.getItem("user") || "{}").userId;
  }
}
```

## ⏱️ Time Estimate

- Backend: 1 giờ
- Frontend: 1.5 giờ
- Testing: 30 phút
- **Total: 3 giờ**

## 🎯 Demo Value

- ⭐⭐⭐⭐⭐ Cực ấn tượng với recruiter
- Real-time = Modern technology
- Dễ demo: Mở 2 tab (Admin + User), thấy notification ngay lập tức

## 📚 Resources

- [Spring WebSocket Docs](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket)
- [RxStomp Guide](https://stomp-js.github.io/guide/rx-stomp/)
