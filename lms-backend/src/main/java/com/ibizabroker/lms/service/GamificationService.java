package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.*;
import com.ibizabroker.lms.dto.*;
import com.ibizabroker.lms.entity.*;
import com.ibizabroker.lms.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Feature 9: Gamification Service
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Mặc định tất cả là chỉ đọc để tối ưu
public class GamificationService {

    private final UserPointsRepository pointsRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final ReadingChallengeRepository challengeRepository;
    private final UserChallengeProgressRepository progressRepository;
    private final UsersRepository usersRepository;
    
    // Feature 9: New gamification features
    private final RewardRepository rewardRepository;
    private final UserRewardRepository userRewardRepository;
    private final DailyQuestRepository dailyQuestRepository;
    private final UserQuestProgressRepository questProgressRepository;
    private final PointTransactionRepository transactionRepository;

    // ============ POINTS ============
    
    // FIX: Đổi thành Read-Write vì hàm này sẽ gọi initializeUserPoints (INSERT) nếu chưa có dữ liệu
    @Transactional 
    public UserPoints getUserPoints(Integer userId) {
        return pointsRepository.findByUserId(userId)
                .orElseGet(() -> initializeUserPoints(userId));
    }

    // FIX: Thêm Transactional để cho phép Insert
    @Transactional
    public UserPoints initializeUserPoints(Integer userId) {
        UserPoints points = new UserPoints();
        points.setUserId(userId);
        points.setTotalPoints(0);
        points.setCurrentLevel(1);
        return pointsRepository.save(points); // Ghi xuống DB
    }

    // FIX: Thêm Transactional để cho phép Update
    @Transactional
    public void awardPoints(Integer userId, int points, String reason) {
        UserPoints userPoints = pointsRepository.findByUserId(userId)
                .orElseGet(() -> initializeUserPoints(userId));
        userPoints.addPoints(points);
        userPoints.setLastActivityDate(LocalDateTime.now());
        pointsRepository.save(userPoints);
        
        // Check for badges
        checkAndAwardBadges(userId, userPoints);
    }

    @Transactional
    public void onBookBorrowed(Integer userId) {
        UserPoints userPoints = getUserPoints(userId);
        userPoints.setBooksBorrowedCount(userPoints.getBooksBorrowedCount() + 1);
        awardPoints(userId, 10, "Mượn sách"); // +10 điểm khi mượn
        
        // Update challenge progress
        updateChallengeProgress(userId);
    }

    @Transactional
    public void onBookReturnedOnTime(Integer userId) {
        UserPoints userPoints = getUserPoints(userId);
        userPoints.setBooksReturnedOnTime(userPoints.getBooksReturnedOnTime() + 1);
        awardPoints(userId, 15, "Trả sách đúng hạn"); // +15 điểm khi trả đúng hạn
    }

    @Transactional
    public void onReviewWritten(Integer userId) {
        UserPoints userPoints = getUserPoints(userId);
        userPoints.setReviewsWritten(userPoints.getReviewsWritten() + 1);
        awardPoints(userId, 20, "Viết đánh giá"); // +20 điểm khi viết review
    }

    // ============ BADGES ============

    @Transactional(readOnly = true)
    public List<Badge> getAllBadges() {
        return badgeRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<UserBadge> getUserBadges(Integer userId) {
        return userBadgeRepository.findByUserId(userId);
    }

    // FIX: Hàm này gọi awardBadge (GHI) nên cần Transactional
    @Transactional
    public void checkAndAwardBadges(Integer userId, UserPoints userPoints) {
        List<Badge> allBadges = badgeRepository.findByIsActiveTrue();
        
        for (Badge badge : allBadges) {
            if (userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())) {
                continue; // Already has this badge
            }
            
            boolean earned = checkBadgeEligibility(badge, userPoints);
            if (earned) {
                awardBadge(userId, badge);
            }
        }
    }

    private boolean checkBadgeEligibility(Badge badge, UserPoints userPoints) {
        if (badge.getRequirementValue() == null) return false;
        
        return switch (badge.getCode()) {
            case "FIRST_BORROW" -> userPoints.getBooksBorrowedCount() >= 1;
            case "BOOKWORM_10" -> userPoints.getBooksBorrowedCount() >= 10;
            case "BOOKWORM_50" -> userPoints.getBooksBorrowedCount() >= 50;
            case "BOOKWORM_100" -> userPoints.getBooksBorrowedCount() >= 100;
            case "PUNCTUAL_5" -> userPoints.getBooksReturnedOnTime() >= 5;
            case "PUNCTUAL_20" -> userPoints.getBooksReturnedOnTime() >= 20;
            case "REVIEWER_5" -> userPoints.getReviewsWritten() >= 5;
            case "REVIEWER_20" -> userPoints.getReviewsWritten() >= 20;
            case "LEVEL_5" -> userPoints.getCurrentLevel() >= 5;
            case "STREAK_7" -> userPoints.getStreakDays() >= 7;
            case "STREAK_30" -> userPoints.getStreakDays() >= 30;
            default -> false;
        };
    }

    @Transactional
    public void awardBadge(Integer userId, Badge badge) {
        if (userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())) {
            return; // Already awarded
        }
        
        UserBadge userBadge = new UserBadge();
        userBadge.setUserId(userId);
        userBadge.setBadge(badge);
        userBadge.setEarnedAt(LocalDateTime.now());
        userBadgeRepository.save(userBadge);
        
        // Award bonus points for earning badge
        if (badge.getPointsReward() > 0) {
            awardPoints(userId, badge.getPointsReward(), "Đạt huy hiệu: " + badge.getNameVi());
        }
    }

    // ============ CHALLENGES ============

    @Transactional(readOnly = true)
    public List<ReadingChallenge> getActiveChallenges() {
        return challengeRepository.findActiveChallenges(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<UserChallengeProgress> getUserChallenges(Integer userId) {
        return progressRepository.findByUserId(userId);
    }

    @Transactional
    public UserChallengeProgress joinChallenge(Integer userId, Long challengeId) {
        @SuppressWarnings("null")
        ReadingChallenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thử thách với ID: " + challengeId));
        
        // Check if already joined
        if (progressRepository.findByUserIdAndChallengeId(userId, challengeId).isPresent()) {
            throw new IllegalStateException("Bạn đã tham gia thử thách này rồi.");
        }
        
        UserChallengeProgress progress = new UserChallengeProgress();
        progress.setUserId(userId);
        progress.setChallenge(challenge);
        progress.setBooksCompleted(0);
        progress.setIsCompleted(false);
        progress.setJoinedAt(LocalDateTime.now());
        
        return progressRepository.save(progress);
    }

    @Transactional
    public void updateChallengeProgress(Integer userId) {
        List<UserChallengeProgress> activeProgress = progressRepository.findActiveProgressByUser(userId);
        
        for (UserChallengeProgress progress : activeProgress) {
            progress.incrementProgress();
            
            if (progress.getIsCompleted()) {
                // Award challenge completion bonus
                ReadingChallenge challenge = progress.getChallenge();
                awardPoints(userId, challenge.getPointsReward(), "Hoàn thành thử thách: " + challenge.getNameVi());
                
                // Award badge if specified
                if (challenge.getBadgeId() != null) {
                    @SuppressWarnings("null")
                    Badge badge = badgeRepository.findById(challenge.getBadgeId()).orElse(null);
                    if (badge != null) {
                        awardBadge(userId, badge);
                    }
                }
            }
            
            progressRepository.save(progress);
        }
    }

    // ============ LEADERBOARD ============

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> getLeaderboard(int limit) {
        List<UserPoints> topUsers = pointsRepository.findLeaderboard(PageRequest.of(0, limit));
        
        return topUsers.stream()
                .map(up -> {
                    @SuppressWarnings("null")
                    String userName = usersRepository.findById(up.getUserId())
                            .map(u -> u.getName())
                            .orElse("Unknown");
                    long badgeCount = userBadgeRepository.countByUserId(up.getUserId());
                    
                    return new LeaderboardEntryDto(
                            up.getUserId(),
                            userName,
                            up.getTotalPoints(),
                            up.getCurrentLevel(),
                            (int) badgeCount
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUserRank(Integer userId) {
        return pointsRepository.countUsersWithMorePoints(userId) + 1;
    }

    // FIX: Đổi thành Read-Write vì logic này gọi getUserPoints -> có thể trigger initializeUserPoints (Save)
    @Transactional
    public GamificationStatsDto getUserStats(Integer userId) {
        UserPoints points = getUserPoints(userId);
        List<UserBadge> badges = getUserBadges(userId);
        List<UserChallengeProgress> challenges = getUserChallenges(userId);
        long rank = getUserRank(userId);
        
        return new GamificationStatsDto(
                points.getTotalPoints(),
                points.getCurrentLevel(),
                (int) rank,
                badges.size(),
                points.getBooksBorrowedCount(),
                points.getBooksReturnedOnTime(),
                points.getReviewsWritten(),
                points.getStreakDays(),
                challenges.stream().filter(c -> !c.getIsCompleted()).count(),
                challenges.stream().filter(UserChallengeProgress::getIsCompleted).count()
        );
    }

    // ============ REWARDS SYSTEM ============

    @Transactional(readOnly = true)
    public RewardItemsResponse getRewardItems(Integer userId) {
        UserPoints userPoints = getUserPoints(userId);
        List<Reward> rewards = rewardRepository.findByAvailableTrue();
        
        List<RewardItemsResponse.RewardItemDTO> rewardDTOs = rewards.stream()
                .map(reward -> {
                    boolean canAfford = userPoints.getTotalPoints() >= reward.getCost();
                    long redemptionsCount = userRewardRepository.countByRewardId(reward.getId());
                    boolean available = reward.getAvailable() && 
                            (reward.getMaxRedemptions() == null || redemptionsCount < reward.getMaxRedemptions());
                    
                    return new RewardItemsResponse.RewardItemDTO(
                            reward.getId(),
                            reward.getName(),
                            reward.getDescription(),
                            reward.getIcon(),
                            reward.getCost(),
                            reward.getCategory(),
                            available,
                            canAfford
                    );
                })
                .collect(Collectors.toList());
        
        return new RewardItemsResponse(rewardDTOs, userPoints.getTotalPoints());
    }

    @Transactional(readOnly = true)
    public List<Reward> getAllRewards() {
        return rewardRepository.findAll();
    }

    @Transactional
    public Reward createReward(Reward reward) {
        if (reward.getCategory() == null || reward.getCategory().isBlank()) {
            reward.setCategory("special");
        }
        if (reward.getAvailable() == null) {
            reward.setAvailable(Boolean.TRUE);
        }
        reward.setCreatedAt(LocalDateTime.now());
        reward.setUpdatedAt(LocalDateTime.now());
        return rewardRepository.save(reward);
    }

    @Transactional
    public Reward updateReward(Long rewardId, Reward payload) {
        Reward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new NotFoundException("Phần thưởng không tồn tại: " + rewardId));

        reward.setName(payload.getName());
        reward.setDescription(payload.getDescription());
        reward.setIcon(payload.getIcon());
        reward.setCost(payload.getCost());
        reward.setCategory(
                payload.getCategory() == null || payload.getCategory().isBlank()
                        ? reward.getCategory()
                        : payload.getCategory()
        );
        reward.setAvailable(payload.getAvailable() != null ? payload.getAvailable() : reward.getAvailable());
        reward.setMaxRedemptions(payload.getMaxRedemptions());
        reward.setUpdatedAt(LocalDateTime.now());

        return rewardRepository.save(reward);
    }

    @Transactional
    public void deleteReward(Long rewardId) {
        Reward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new NotFoundException("Phần thưởng không tồn tại: " + rewardId));
        rewardRepository.delete(reward);
    }

    @Transactional
    public Reward updateRewardStock(Long rewardId, Integer stock) {
        Reward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new NotFoundException("Phần thưởng không tồn tại: " + rewardId));
        reward.setMaxRedemptions(stock);
        reward.setUpdatedAt(LocalDateTime.now());
        return rewardRepository.save(reward);
    }

    @SuppressWarnings("null")
    @Transactional
    public void redeemReward(Integer userId, Long rewardId) {
        UserPoints userPoints = getUserPoints(userId);
        Reward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new NotFoundException("Phần thưởng không tồn tại"));
        
        // Validate
        if (!reward.getAvailable()) {
            throw new IllegalStateException("Phần thưởng không khả dụng");
        }
        if (userPoints.getTotalPoints() < reward.getCost()) {
            throw new IllegalStateException("Không đủ điểm để đổi phần thưởng này");
        }
        
        // Check redemption limit
        if (reward.getMaxRedemptions() != null) {
            long count = userRewardRepository.countByRewardId(rewardId);
            if (count >= reward.getMaxRedemptions()) {
                throw new IllegalStateException("Phần thưởng đã hết lượt đổi");
            }
        }
        
        // Deduct points
        userPoints.setTotalPoints(userPoints.getTotalPoints() - reward.getCost());
        userPoints.setUpdatedAt(LocalDateTime.now());
        pointsRepository.save(userPoints);
        
        // Create redemption record
        UserReward userReward = new UserReward();
        userReward.setUserId(userId);
        userReward.setRewardId(rewardId);
        userReward.setPointsSpent(reward.getCost());
        userReward.setRedeemedAt(LocalDateTime.now());
        
        // Set expiry (30 days for extension rewards)
        if ("extension".equals(reward.getCategory())) {
            userReward.setExpiresAt(LocalDateTime.now().plusDays(30));
        }
        
        userRewardRepository.save(userReward);
        
        // Log transaction
        logPointTransaction(userId, -reward.getCost(), "reward", 
                "Đổi phần thưởng: " + reward.getName(), rewardId, userPoints.getTotalPoints());
    }

    // ============ DAILY QUESTS ============

    @Transactional(readOnly = true)
    public DailyQuestsResponse getDailyQuests(Integer userId) {
        List<DailyQuest> quests = dailyQuestRepository.findByActiveTrue();
        LocalDate today = LocalDate.now();
        
        List<DailyQuestsResponse.DailyQuestDTO> questDTOs = quests.stream()
                .map(quest -> {
                    UserQuestProgress progress = questProgressRepository
                            .findByUserIdAndQuestIdAndDate(userId, quest.getId(), today)
                            .orElse(null);
                    
                    return new DailyQuestsResponse.DailyQuestDTO(
                            quest.getId(),
                            quest.getTitle(),
                            quest.getDescription(),
                            quest.getPoints(),
                            progress != null && progress.getCompleted(),
                            progress != null ? progress.getProgress() : 0,
                            quest.getTarget()
                    );
                })
                .collect(Collectors.toList());
        
        return new DailyQuestsResponse(questDTOs);
    }

    @Transactional
    public void updateQuestProgress(Integer userId, String questType) {
        DailyQuest quest = dailyQuestRepository.findByQuestType(questType);
        if (quest == null || !quest.getActive()) {
            return;
        }
        
        LocalDate today = LocalDate.now();
        UserQuestProgress progress = questProgressRepository
                .findByUserIdAndQuestIdAndDate(userId, quest.getId(), today)
                .orElseGet(() -> {
                    UserQuestProgress newProgress = new UserQuestProgress();
                    newProgress.setUserId(userId);
                    newProgress.setQuestId(quest.getId());
                    newProgress.setDate(today);
                    newProgress.setProgress(0);
                    newProgress.setCompleted(false);
                    return newProgress;
                });
        
        if (progress.getCompleted()) {
            return; // Already completed today
        }
        
        progress.setProgress(progress.getProgress() + 1);
        
        // Check if completed
        if (progress.getProgress() >= quest.getTarget()) {
            progress.setCompleted(true);
            progress.setPointsEarned(quest.getPoints());
            
            // Award points
            awardPoints(userId, quest.getPoints(), "Hoàn thành nhiệm vụ: " + quest.getTitle());
            
            // Log transaction
            logPointTransaction(userId, quest.getPoints(), "quest", 
                    "Nhiệm vụ: " + quest.getTitle(), quest.getId(), 
                    getUserPoints(userId).getTotalPoints());
        }
        
        questProgressRepository.save(progress);
    }

    // ============ POINT HISTORY ============

    @Transactional(readOnly = true)
    public PointHistoryResponse getPointHistory(Integer userId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<PointTransaction> transactions = transactionRepository
                .findByUserIdAndTimestampAfter(userId, startDate);
        
        // Group by date and aggregate
        java.util.Map<LocalDate, java.util.List<PointTransaction>> grouped = transactions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        t -> t.getTimestamp().toLocalDate()
                ));
        
        List<PointHistoryResponse.PointHistoryDTO> history = grouped.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<PointTransaction> dayTransactions = entry.getValue();
                    
                    int totalChange = dayTransactions.stream()
                            .mapToInt(PointTransaction::getPoints)
                            .sum();
                    
                    PointTransaction lastTransaction = dayTransactions.get(0);
                    String reason = dayTransactions.size() == 1 
                            ? lastTransaction.getReason()
                            : dayTransactions.size() + " giao dịch";
                    
                    return new PointHistoryResponse.PointHistoryDTO(
                            date,
                            lastTransaction.getBalanceAfter(),
                            totalChange,
                            reason
                    );
                })
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(Collectors.toList());
        
        return new PointHistoryResponse(history);
    }

    @Transactional
    public void logPointTransaction(Integer userId, Integer points, String type, 
                                    String reason, Long referenceId, Integer balanceAfter) {
        PointTransaction transaction = new PointTransaction();
        transaction.setUserId(userId);
        transaction.setPoints(points);
        transaction.setTransactionType(type);
        transaction.setReason(reason);
        transaction.setReferenceId(referenceId);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setTimestamp(LocalDateTime.now());
        transactionRepository.save(transaction);
    }

    // ============ STREAK FREEZE ============

    @Transactional
    public void purchaseStreakFreeze(Integer userId) {
        UserPoints userPoints = getUserPoints(userId);
        final int FREEZE_COST = 200;
        
        if (userPoints.getTotalPoints() < FREEZE_COST) {
            throw new IllegalStateException("Không đủ điểm để mua Streak Freeze (cần 200 điểm)");
        }
        
        // Deduct points
        userPoints.setTotalPoints(userPoints.getTotalPoints() - FREEZE_COST);
        userPoints.setStreakFreezeCount(userPoints.getStreakFreezeCount() + 1);
        userPoints.setUpdatedAt(LocalDateTime.now());
        pointsRepository.save(userPoints);
        
        // Log transaction
        logPointTransaction(userId, -FREEZE_COST, "special", 
                "Mua Streak Freeze (❄️ +1 lần bảo vệ chuỗi)", null, 
                userPoints.getTotalPoints());
    }
}
