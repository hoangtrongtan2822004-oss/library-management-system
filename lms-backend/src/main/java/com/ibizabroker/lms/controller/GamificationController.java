package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dto.*;
import com.ibizabroker.lms.entity.*;
import com.ibizabroker.lms.service.GamificationService;
import com.ibizabroker.lms.util.JwtUtil;
import com.ibizabroker.lms.dao.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    private final JwtUtil jwtUtil;
    private final UsersRepository usersRepository;

    // ============ USER ENDPOINTS ============

    @GetMapping("/user/gamification/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GamificationStatsDto> getMyStats(Authentication auth) {
        Integer userId = null;
        try {
            userId = jwtUtil.extractUserIdFromAuth(auth);
        } catch (Exception e) {
            String username = auth.getName();
            userId = usersRepository.findByUsername(username)
                    .map(u -> u.getUserId())
                    .orElseThrow(() -> new IllegalStateException("Cannot find userId for username: " + username));
        }
        return ResponseEntity.ok(gamificationService.getUserStats(userId));
    }

    @GetMapping("/user/gamification/points")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserPoints> getMyPoints(Authentication auth) {
        Integer userId = null;
        try {
            userId = jwtUtil.extractUserIdFromAuth(auth);
        } catch (Exception e) {
            String username = auth.getName();
            userId = usersRepository.findByUsername(username)
                    .map(u -> u.getUserId())
                    .orElseThrow(() -> new IllegalStateException("Cannot find userId for username: " + username));
        }
        return ResponseEntity.ok(gamificationService.getUserPoints(userId));
    }

    @GetMapping("/user/gamification/badges")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserBadge>> getMyBadges(Authentication auth) {
        Integer userId = null;
        try {
            userId = jwtUtil.extractUserIdFromAuth(auth);
        } catch (Exception e) {
            String username = auth.getName();
            userId = usersRepository.findByUsername(username)
                    .map(u -> u.getUserId())
                    .orElseThrow(() -> new IllegalStateException("Cannot find userId for username: " + username));
        }
        return ResponseEntity.ok(gamificationService.getUserBadges(userId));
    }

    @GetMapping("/user/gamification/rank")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getMyRank(Authentication auth) {
        Integer userId = null;
        try {
            userId = jwtUtil.extractUserIdFromAuth(auth);
        } catch (Exception e) {
            String username = auth.getName();
            userId = usersRepository.findByUsername(username)
                    .map(u -> u.getUserId())
                    .orElseThrow(() -> new IllegalStateException("Cannot find userId for username: " + username));
        }
        long rank = gamificationService.getUserRank(userId);
        return ResponseEntity.ok(Map.of("rank", rank));
    }

    @GetMapping("/user/gamification/challenges")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserChallengeProgress>> getMyChallenges(Authentication auth) {
        Integer userId = null;
        try {
            userId = jwtUtil.extractUserIdFromAuth(auth);
        } catch (Exception e) {
            String username = auth.getName();
            userId = usersRepository.findByUsername(username)
                    .map(u -> u.getUserId())
                    .orElseThrow(() -> new IllegalStateException("Cannot find userId for username: " + username));
        }
        return ResponseEntity.ok(gamificationService.getUserChallenges(userId));
    }

    @PostMapping("/user/gamification/challenges/{challengeId}/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserChallengeProgress> joinChallenge(
            @PathVariable Long challengeId,
            Authentication auth) {
        Integer userId = null;
        try {
            userId = jwtUtil.extractUserIdFromAuth(auth);
        } catch (Exception e) {
            String username = auth.getName();
            userId = usersRepository.findByUsername(username)
                    .map(u -> u.getUserId())
                    .orElseThrow(() -> new IllegalStateException("Cannot find userId for username: " + username));
        }
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
        Integer userId = Integer.parseInt(userDetails.getUsername());
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
        Integer userId = Integer.parseInt(userDetails.getUsername());
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
        Integer userId = Integer.parseInt(userDetails.getUsername());
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
        Integer userId = Integer.parseInt(userDetails.getUsername());
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
        Integer userId = Integer.parseInt(userDetails.getUsername());
        return ResponseEntity.ok(gamificationService.getPointHistory(userId, days));
    }

    /**
     * Purchase streak freeze
     */
    @PostMapping("/user/gamification/streak-freeze")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> purchaseStreakFreeze(@AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = Integer.parseInt(userDetails.getUsername());
        gamificationService.purchaseStreakFreeze(userId);
        
        UserPoints userPoints = gamificationService.getUserPoints(userId);
        return ResponseEntity.ok(Map.of(
                "message", "Đã mua Streak Freeze thành công! ❄️",
                "remainingPoints", userPoints.getTotalPoints(),
                "streakFreezeCount", userPoints.getStreakFreezeCount()
        ));
    }
}
