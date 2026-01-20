# 🎮 Gamification Upgrade - Backend Implementation Guide

## ✅ Hoàn Thành

Đã implement 4 features gamification mới với đầy đủ backend APIs:

### 1. 🏪 Rewards System (Hệ Thống Phần Thưởng)

**Entities Created:**

- `Reward.java` - Định nghĩa phần thưởng
- `UserReward.java` - Lịch sử đổi thưởng

**Endpoints:**

- `GET /api/user/gamification/rewards` - Lấy danh sách phần thưởng
- `POST /api/user/gamification/rewards/{id}/redeem` - Đổi phần thưởng

**Features:**

- ✅ 4 loại reward: Extension (🎫), Priority (⭐), Cosmetic (🖼️), Special (❄️)
- ✅ Validation: Check điểm đủ, check available, check max redemptions
- ✅ Auto deduct points khi đổi
- ✅ Log transaction history
- ✅ Set expiry date (30 days cho extension rewards)

**Default Rewards:**

```sql
🎫 Vé Gia Hạn 7 Ngày - 500 điểm
⭐ Ưu Tiên Mượn Sách - 1000 điểm
🖼️ Avatar Viền Vàng - 800 điểm
❄️ Streak Freeze - 200 điểm
```

---

### 2. 📋 Daily Quests (Nhiệm Vụ Hàng Ngày)

**Entities Created:**

- `DailyQuest.java` - Định nghĩa quest
- `UserQuestProgress.java` - Tiến độ của user

**Endpoints:**

- `GET /api/user/gamification/daily-quests` - Lấy danh sách quest hôm nay

**Features:**

- ✅ 3 quest types: login, search, review
- ✅ Track progress theo ngày
- ✅ Auto reset mỗi ngày
- ✅ Award points khi complete
- ✅ Unique constraint (user_id, quest_id, date)

**Default Quests:**

```sql
🎯 Đăng Nhập Hàng Ngày - +10 điểm
🔍 Tìm Kiếm Sách - +5 điểm
📝 Viết Đánh Giá - +20 điểm
```

**Service Methods:**

- `getDailyQuests(userId)` - Get quests with progress
- `updateQuestProgress(userId, questType)` - Update progress, award points

**Integration Points:**

- `AuthController` login → call `updateQuestProgress(userId, "login")`
- `BooksController` search → call `updateQuestProgress(userId, "search")`
- `ReviewService` create → call `updateQuestProgress(userId, "review")`

---

### 3. 📊 Point History (Lịch Sử Điểm)

**Entity Created:**

- `PointTransaction.java` - Ghi nhận mọi thay đổi điểm

**Endpoint:**

- `GET /api/user/gamification/point-history?days=30` - Lấy lịch sử 30 ngày

**Features:**

- ✅ Log every point change (earn + spend)
- ✅ Store balance_after để check consistency
- ✅ Group by date và aggregate
- ✅ Support các loại: borrow, return, review, reward, admin, quest
- ✅ Include referenceId để trace nguồn gốc

**Transaction Types:**

```java
borrow   → Mượn sách (+10đ)
return   → Trả sách đúng hạn (+15đ)
review   → Viết đánh giá (+20đ)
quest    → Hoàn thành nhiệm vụ (+5/10/20đ)
reward   → Đổi phần thưởng (-200/500/800/1000đ)
admin    → Admin trao điểm
special  → Mua streak freeze (-200đ)
```

**Service Method:**

- `logPointTransaction(userId, points, type, reason, refId, balanceAfter)`

**Auto Integration:**

- ✅ Called by `awardPoints()` → log when earn
- ✅ Called by `redeemReward()` → log when spend
- ✅ Called by `purchaseStreakFreeze()` → log freeze purchase
- ✅ Called by `updateQuestProgress()` → log quest completion

---

### 4. ❄️ Streak Freeze (Bảo Vệ Chuỗi)

**Entity Updated:**

- `UserPoints.java` - Added `streakFreezeCount` field

**Endpoint:**

- `POST /api/user/gamification/streak-freeze` - Mua freeze (200 điểm)

**Features:**

- ✅ Cost: 200 points
- ✅ Increment `streakFreezeCount`
- ✅ Log transaction
- ✅ Return remaining points + freeze count

**Logic:**

```java
if (userPoints.getTotalPoints() >= 200) {
    userPoints.setTotalPoints(totalPoints - 200);
    userPoints.setStreakFreezeCount(freezeCount + 1);
    // Log transaction
}
```

**Future Integration:**
Cần implement logic check freeze khi reset streak:

```java
// In streak checking logic:
if (missedDay && userPoints.getStreakFreezeCount() > 0) {
    userPoints.setStreakFreezeCount(freezeCount - 1); // Use 1 freeze
    // Don't reset streak
} else if (missedDay) {
    userPoints.setStreakDays(0); // Reset streak
}
```

---

## 📁 Files Created/Modified

### Entities (5 new + 1 updated)

```
✅ entity/Reward.java
✅ entity/UserReward.java
✅ entity/DailyQuest.java
✅ entity/UserQuestProgress.java
✅ entity/PointTransaction.java
✅ entity/UserPoints.java (added streakFreezeCount)
```

### Repositories (5 new)

```
✅ dao/RewardRepository.java
✅ dao/UserRewardRepository.java
✅ dao/DailyQuestRepository.java
✅ dao/UserQuestProgressRepository.java
✅ dao/PointTransactionRepository.java
```

### DTOs (3 new)

```
✅ dto/RewardItemsResponse.java
✅ dto/DailyQuestsResponse.java
✅ dto/PointHistoryResponse.java
```

### Services

```
✅ service/GamificationService.java (added 9 new methods)
```

### Controllers

```
✅ controller/GamificationController.java (added 5 new endpoints)
```

### Database

```
✅ resources/db-migration-gamification-upgrade.sql
```

---

## 🚀 API Endpoints Summary

### User Endpoints (Requires Authentication)

#### 1. Get Rewards

```http
GET /api/user/gamification/rewards
Authorization: Bearer <token>

Response:
{
  "rewards": [
    {
      "id": 1,
      "name": "Vé Gia Hạn 7 Ngày",
      "description": "Gia hạn thêm 7 ngày cho sách đang mượn",
      "icon": "🎫",
      "cost": 500,
      "category": "extension",
      "available": true,
      "canAfford": true
    }
  ],
  "userPoints": 1250
}
```

#### 2. Redeem Reward

```http
POST /api/user/gamification/rewards/1/redeem
Authorization: Bearer <token>

Response:
{
  "message": "Đã đổi phần thưởng thành công!",
  "remainingPoints": 750
}
```

#### 3. Get Daily Quests

```http
GET /api/user/gamification/daily-quests
Authorization: Bearer <token>

Response:
{
  "quests": [
    {
      "id": 1,
      "title": "Đăng Nhập Hàng Ngày",
      "description": "Đăng nhập vào hệ thống",
      "points": 10,
      "completed": true,
      "progress": 1,
      "target": 1
    },
    {
      "id": 2,
      "title": "Tìm Kiếm Sách",
      "description": "Tìm kiếm sách trong thư viện",
      "points": 5,
      "completed": false,
      "progress": 0,
      "target": 1
    }
  ]
}
```

#### 4. Get Point History

```http
GET /api/user/gamification/point-history?days=30
Authorization: Bearer <token>

Response:
{
  "history": [
    {
      "date": "2024-01-15",
      "points": 1250,
      "change": +20,
      "reason": "Viết đánh giá"
    },
    {
      "date": "2024-01-14",
      "points": 1230,
      "change": -500,
      "reason": "Đổi phần thưởng: Vé Gia Hạn 7 Ngày"
    }
  ]
}
```

#### 5. Purchase Streak Freeze

```http
POST /api/user/gamification/streak-freeze
Authorization: Bearer <token>

Response:
{
  "message": "Đã mua Streak Freeze thành công! ❄️",
  "remainingPoints": 1050,
  "streakFreezeCount": 1
}
```

---

## 🔧 Service Method Reference

### GamificationService New Methods

```java
// Rewards
RewardItemsResponse getRewardItems(Integer userId)
void redeemReward(Integer userId, Long rewardId)

// Daily Quests
DailyQuestsResponse getDailyQuests(Integer userId)
void updateQuestProgress(Integer userId, String questType)

// Point History
PointHistoryResponse getPointHistory(Integer userId, int days)
void logPointTransaction(userId, points, type, reason, refId, balanceAfter)

// Streak Freeze
void purchaseStreakFreeze(Integer userId)
```

---

## 🗄️ Database Schema

### New Tables

```sql
rewards (
  id, name, description, icon, cost,
  category, available, max_redemptions,
  created_at, updated_at
)

user_rewards (
  id, user_id, reward_id, points_spent,
  redeemed_at, used, used_at, expires_at
)

daily_quests (
  id, title, description, points,
  quest_type, target, active, created_at
)

user_quest_progress (
  id, user_id, quest_id, progress,
  completed, date, points_earned
  UNIQUE(user_id, quest_id, date)
)

point_transactions (
  id, user_id, points, transaction_type,
  reason, reference_id, timestamp, balance_after
)
```

### Updated Tables

```sql
ALTER TABLE user_points
ADD COLUMN streak_freeze_count INT NOT NULL DEFAULT 0;
```

---

## ⚙️ Setup Instructions

### 1. Run Database Migration

```bash
# Connect to MySQL
mysql -u root -p lms_db

# Run migration script
source lms-backend/src/main/resources/db-migration-gamification-upgrade.sql
```

### 2. Verify Tables Created

```sql
SHOW TABLES LIKE '%reward%';
SHOW TABLES LIKE '%quest%';
SHOW TABLES LIKE '%transaction%';

SELECT * FROM rewards;
SELECT * FROM daily_quests;
```

### 3. Restart Backend

```bash
cd lms-backend
mvn spring-boot:run
```

### 4. Test Endpoints

```bash
# Login to get token
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@test.com","password":"123456"}'

# Get rewards
curl -X GET http://localhost:8081/api/user/gamification/rewards \
  -H "Authorization: Bearer <your-token>"

# Get daily quests
curl -X GET http://localhost:8081/api/user/gamification/daily-quests \
  -H "Authorization: Bearer <your-token>"
```

---

## 🔗 Frontend Integration

Frontend đã có sẵn mock data, chỉ cần replace với real API calls:

### gamification.service.ts

```typescript
// BEFORE (mock data)
getRewardItems(): Observable<RewardItem[]> {
  return of(this.getMockRewardItems());
}

// AFTER (real API)
getRewardItems(): Observable<RewardItem[]> {
  return this.http.get<RewardItem[]>('/api/user/gamification/rewards');
}
```

Làm tương tự cho:

- `redeemReward(id)` → POST /api/user/gamification/rewards/{id}/redeem
- `getDailyQuests()` → GET /api/user/gamification/daily-quests
- `getPointHistory(days)` → GET /api/user/gamification/point-history?days=30
- `purchaseStreakFreeze()` → POST /api/user/gamification/streak-freeze

---

## 📝 Next Steps

### Must Do:

1. ✅ **Run migration SQL** để tạo tables
2. ✅ **Restart backend** để load entities mới
3. ⏳ **Update frontend** để call real APIs thay vì mock data
4. ⏳ **Integrate quest tracking** vào các actions:
   - Login → `updateQuestProgress(userId, "login")`
   - Search → `updateQuestProgress(userId, "search")`
   - Review → `updateQuestProgress(userId, "review")`

### Optional:

5. ⏳ **Implement CRON job** để reset daily quests mỗi nửa đêm
6. ⏳ **Implement streak freeze logic** khi check streak daily
7. ⏳ **Add admin endpoints** để manage rewards/quests
8. ⏳ **Add notification** khi complete quest hoặc redeem reward

---

## 🎯 Testing Checklist

- [ ] Verify all tables created successfully
- [ ] Test GET /api/user/gamification/rewards (shows 4 default rewards)
- [ ] Test POST redeem reward (deduct points, create UserReward)
- [ ] Test GET daily quests (shows 3 quests with progress 0)
- [ ] Test quest progress update (complete quest, award points)
- [ ] Test GET point history (shows aggregated daily data)
- [ ] Test purchase streak freeze (deduct 200 points, increment count)
- [ ] Verify point transactions logged correctly
- [ ] Test with insufficient points (should throw error)
- [ ] Test with already completed quest (should not double award)

---

## 🐛 Common Issues

**Issue:** Can't find RewardRepository
**Fix:** Make sure you ran `mvn clean install` after creating repositories

**Issue:** SQL error "table already exists"
**Fix:** Add `IF NOT EXISTS` to all CREATE TABLE statements (already done)

**Issue:** Frontend shows 404 for new endpoints
**Fix:** Make sure backend is running and check console for any startup errors

**Issue:** Points not updating after quest completion
**Fix:** Check if `updateQuestProgress()` is being called from the right places

---

## 📚 References

- **Entities**: [lms-backend/src/main/java/com/ibizabroker/lms/entity/](file://c:/Users/Admin/library-management-system/lms-backend/src/main/java/com/ibizabroker/lms/entity/)
- **Services**: [GamificationService.java](file://c:/Users/Admin/library-management-system/lms-backend/src/main/java/com/ibizabroker/lms/service/GamificationService.java)
- **Controller**: [GamificationController.java](file://c:/Users/Admin/library-management-system/lms-backend/src/main/java/com/ibizabroker/lms/controller/GamificationController.java)
- **SQL Migration**: [db-migration-gamification-upgrade.sql](file://c:/Users/Admin/library-management-system/lms-backend/src/main/resources/db-migration-gamification-upgrade.sql)

---

**Status:** ✅ Backend Implementation Complete
**Date:** 2024-01-15
**Author:** GitHub Copilot
