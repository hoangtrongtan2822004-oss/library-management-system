package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.dto.*;
import com.ibizabroker.lms.entity.*;
import com.ibizabroker.lms.exceptions.NotFoundException;
import com.ibizabroker.lms.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feature 9: Gamification Controller
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;
    private final UsersRepository usersRepository;

    // ============ HELPER METHOD ============

    /**
     * Helper method to get userId from username
     * Fixes NumberFormatException when trying to parse username as integer
     */
    private Integer getUserIdFromUsername(String username) {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        return user.getUserId();
    }

    // ============ USER ENDPOINTS ============

    @GetMapping("/user/gamification/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GamificationStatsDto> getMyStats(@AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = getUserIdFromUsername(userDetails.getUsername());
        return ResponseEntity.ok(gamificationService.getUserStats(userId));
    }

    @GetMapping("/user/gamification/points")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserPoints> getMyPoints(@AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = getUserIdFromUsername(userDetails.getUsername());
        return ResponseEntity.ok(gamificationService.getUserPoints(userId));
    }

    @GetMapping("/user/gamification/badges")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserBadge>> getMyBadges(@AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = getUserIdFromUsername(userDetails.getUsername());
        return ResponseEntity.ok(gamificationService.getUserBadges(userId));
    }

    @GetMapping("/user/gamification/rank")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getMyRank(@AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = getUserIdFromUsername(userDetails.getUsername());
        long rank = gamificationService.getUserRank(userId);
        return ResponseEntity.ok(Map.of("rank", rank));
    }

    @GetMapping("/user/gamification/challenges")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserChallengeProgress>> getMyChallenges(@AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = getUserIdFromUsername(userDetails.getUsername());
        return ResponseEntity.ok(gamificationService.getUserChallenges(userId));
    }

    @PostMapping("/user/gamification/challenges/{challengeId}/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserChallengeProgress> joinChallenge(
            @PathVariable Long challengeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = getUserIdFromUsername(userDetails.getUsername());
        return ResponseEntity.ok(gamificationService.joinChallenge(userId, challengeId));
    }

    // ============ PUBLIC ENDPOINTS ============

    @GetMapping("/public/gamification/leaderboard")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(gamificationService.getLeaderboard(limit));
    }

    @GetMapping("/public/gamification/badges")
    public ResponseEntity<List<Badge>> getAllBadges() {
        return ResponseEntity.ok(gamificationService.getAllBadges());
    }

    @GetMapping("/public/gamification/challenges")
    public ResponseEntity<List<ReadingChallenge>> getActiveChallenges() {
        return ResponseEntity.ok(gamificationService.getActiveChallenges());
    }

    // Alias to support /active path used by some clients
    @GetMapping("/public/gamification/challenges/active")
    public ResponseEntity<List<ReadingChallenge>> getActiveChallengesAlias() {
        return ResponseEntity.ok(gamificationService.getActiveChallenges());
    }

    // ============ ADMIN ENDPOINTS ============

    @GetMapping("/admin/gamification/users/{userId}/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GamificationStatsDto> getUserStats(@PathVariable Integer userId) {
        return ResponseEntity.ok(gamificationService.getUserStats(userId));
    }

    @PostMapping("/admin/gamification/users/{userId}/points")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> awardPoints(
            @PathVariable Integer userId,
            @RequestParam int points,
            @RequestParam(defaultValue = "Admin bonus") String reason) {
        gamificationService.awardPoints(userId, points, reason);
        return ResponseEntity.ok(Map.of("message", "Đã trao " + points + " điểm cho user " + userId));
    }

    // ============ NEW GAMIFICATION FEATURES ============

    /**
     * Get all available rewards with user's current points
     */
    @GetMapping("/user/gamification/rewards")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RewardItemsResponse> getRewards(@AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = getUserIdFromUsername(userDetails.getUsername());
        return ResponseEntity.ok(gamificationService.getRewardItems(userId));
    }

    /**
     * Redeem a reward
     */
    @PostMapping("/user/gamification/rewards/{rewardId}/redeem")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> redeemReward(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long rewardId) {
        Integer userId = getUserIdFromUsername(userDetails.getUsername());
        gamificationService.redeemReward(userId, rewardId);
        
        UserPoints userPoints = gamificationService.getUserPoints(userId);
        return ResponseEntity.ok(Map.of(
                "message", "Đã đổi phần thưởng thành công!",
                "remainingPoints", userPoints.getTotalPoints()
        ));
    }

    /**
     * Get daily quests with progress
     */
    @GetMapping("/user/gamification/daily-quests")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DailyQuestsResponse> getDailyQuests(@AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = getUserIdFromUsername(userDetails.getUsername());
        return ResponseEntity.ok(gamificationService.getDailyQuests(userId));
    }

    /**
     * Update quest progress (called when user performs action)
     */
    @PostMapping("/user/gamification/quests/{questType}/progress")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> updateQuestProgress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String questType) {
        Integer userId = getUserIdFromUsername(userDetails.getUsername());
        gamificationService.updateQuestProgress(userId, questType);
        return ResponseEntity.ok(Map.of("message", "Quest progress updated"));
    }

    /**
     * Get point transaction history
     */
    @GetMapping("/user/gamification/point-history")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PointHistoryResponse> getPointHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "30") int days) {
        Integer userId = getUserIdFromUsername(userDetails.getUsername());
        return ResponseEntity.ok(gamificationService.getPointHistory(userId, days));
    }

    /**
     * Purchase streak freeze
     */
    @PostMapping("/user/gamification/streak-freeze")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> purchaseStreakFreeze(@AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = getUserIdFromUsername(userDetails.getUsername());
        gamificationService.purchaseStreakFreeze(userId);
        
        UserPoints userPoints = gamificationService.getUserPoints(userId);
        return ResponseEntity.ok(Map.of(
                "message", "Đã mua Streak Freeze thành công! ❄️",
                "remainingPoints", userPoints.getTotalPoints(),
                "streakFreezeCount", userPoints.getStreakFreezeCount()
        ));
    }

    // ============ ADMIN MANAGEMENT ENDPOINTS ============

    /**
     * Admin: Get all rewards
     */
    @GetMapping("/admin/gamification/rewards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Reward>> getAllRewardsAdmin() {
        return ResponseEntity.ok(gamificationService.getAllRewards());
    }

    /**
     * Admin: Create reward
     */
    @PostMapping("/admin/gamification/rewards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Reward> createReward(@RequestBody Reward reward) {
        return ResponseEntity.ok(gamificationService.createReward(reward));
    }

    /**
     * Admin: Update reward
     */
    @PutMapping("/admin/gamification/rewards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Reward> updateReward(@PathVariable Long id, @RequestBody Reward reward) {
        return ResponseEntity.ok(gamificationService.updateReward(id, reward));
    }

    /**
     * Admin: Delete reward
     */
    @DeleteMapping("/admin/gamification/rewards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteReward(@PathVariable Long id) {
        gamificationService.deleteReward(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa phần thưởng"));
    }

    /**
     * Admin: Get all challenges
     */
    @GetMapping("/admin/gamification/challenges")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReadingChallenge>> getAllChallengesAdmin() {
        return ResponseEntity.ok(gamificationService.getAllChallenges());
    }

    /**
     * Admin: Create challenge
     */
    @PostMapping("/admin/gamification/challenges")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReadingChallenge> createChallenge(@RequestBody ReadingChallenge challenge) {
        return ResponseEntity.ok(gamificationService.createChallenge(challenge));
    }

    /**
     * Admin: Update challenge
     */
    @PutMapping("/admin/gamification/challenges/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReadingChallenge> updateChallenge(@PathVariable Long id, @RequestBody ReadingChallenge challenge) {
        return ResponseEntity.ok(gamificationService.updateChallenge(id, challenge));
    }

    /**
     * Admin: Delete challenge
     */
    @DeleteMapping("/admin/gamification/challenges/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteChallenge(@PathVariable Long id) {
        gamificationService.deleteChallenge(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa thử thách"));
    }

    /**
     * Admin: Get all badges
     */
    @GetMapping("/admin/gamification/badges")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Badge>> getAllBadgesAdmin() {
        return ResponseEntity.ok(gamificationService.getAllBadges());
    }

    /**
     * Admin: Create badge
     */
    @PostMapping("/admin/gamification/badges")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createBadge(@RequestBody Badge badge) {
        try {
            Badge created = gamificationService.createBadge(badge);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(com.ibizabroker.lms.dto.ApiResponse.error(ex.getMessage(), 400));
        }
    }

    /**
     * Admin: Update badge
     */
    @PutMapping("/admin/gamification/badges/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Badge> updateBadge(@PathVariable Long id, @RequestBody Badge badge) {
        return ResponseEntity.ok(gamificationService.updateBadge(id, badge));
    }

    /**
     * Admin: Delete badge
     */
    @DeleteMapping("/admin/gamification/badges/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteBadge(@PathVariable Long id) {
        gamificationService.deleteBadge(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa huy hiệu"));
    }
}