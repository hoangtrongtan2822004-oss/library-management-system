package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.dto.*;
import com.ibizabroker.lms.entity.*;
import com.ibizabroker.lms.exceptions.NotFoundException;
import com.ibizabroker.lms.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/admin/gamification/users/{userId}/badges")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> awardBadgeToUser(
            @PathVariable Integer userId,
            @RequestBody Map<String, Object> body) {
        Long badgeId = ((Number) body.get("badgeId")).longValue();
        gamificationService.awardBadgeToUser(userId, badgeId);
        return ResponseEntity.ok(Map.of("message", "Đã trao huy hiệu cho user " + userId));
    }

    // ============ ADMIN BADGE CRUD ============

    @PostMapping("/admin/gamification/badges")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Badge> createBadge(@RequestBody Map<String, Object> body) {
        Badge badge = new Badge();
        if (body.get("name") != null) badge.setNameVi(body.get("name").toString());
        if (body.get("nameEn") != null) badge.setNameEn(body.get("nameEn").toString());
        else badge.setNameEn(badge.getNameVi());
        if (body.get("description") != null) badge.setDescriptionVi(body.get("description").toString());
        if (body.get("iconUrl") != null) badge.setIconUrl(body.get("iconUrl").toString());
        if (body.get("points") != null) badge.setPointsReward(((Number) body.get("points")).intValue());
        if (body.get("requirement") != null) badge.setRequirementValue(((Number) body.get("requirement")).intValue());
        if (body.get("tier") != null) badge.setCategory(body.get("tier").toString());
        // Auto-generate code from name if not provided or empty
        String rawCode = body.get("code") != null ? body.get("code").toString().trim() : "";
        String code = !rawCode.isEmpty() ? rawCode
                : (badge.getNameVi() != null && !badge.getNameVi().isBlank()
                        ? badge.getNameVi().toUpperCase().replaceAll("[^A-Z0-9]", "_")
                        : "");
        badge.setCode(code);
        return ResponseEntity.status(HttpStatus.CREATED).body(gamificationService.createBadge(badge));
    }

    @PutMapping("/admin/gamification/badges/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Badge> updateBadge(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Badge payload = new Badge();
        if (body.get("name") != null) payload.setNameVi(body.get("name").toString());
        if (body.get("nameEn") != null) payload.setNameEn(body.get("nameEn").toString());
        if (body.get("description") != null) payload.setDescriptionVi(body.get("description").toString());
        if (body.get("iconUrl") != null) payload.setIconUrl(body.get("iconUrl").toString());
        if (body.get("points") != null) payload.setPointsReward(((Number) body.get("points")).intValue());
        if (body.get("requirement") != null) payload.setRequirementValue(((Number) body.get("requirement")).intValue());
        if (body.get("tier") != null) payload.setCategory(body.get("tier").toString());
        return ResponseEntity.ok(gamificationService.updateBadge(id, payload));
    }

    @DeleteMapping("/admin/gamification/badges/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBadge(@PathVariable Long id) {
        gamificationService.deleteBadge(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/gamification/badges/{id}/icon")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Badge> uploadBadgeIcon(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        // Store icon URL as a data URL or save to static path; for now use filename as placeholder
        String iconUrl = "/assets/badges/" + file.getOriginalFilename();
        Badge payload = new Badge();
        payload.setIconUrl(iconUrl);
        return ResponseEntity.ok(gamificationService.updateBadge(id, payload));
    }

    // ============ ADMIN CHALLENGE CRUD ============

    @GetMapping("/admin/gamification/challenges")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReadingChallenge>> getChallengesForAdmin() {
        return ResponseEntity.ok(gamificationService.getAllChallenges());
    }

    @PostMapping("/admin/gamification/challenges")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReadingChallenge> createChallenge(@RequestBody Map<String, Object> body) {
        ReadingChallenge challenge = new ReadingChallenge();
        applyChallengBody(challenge, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(gamificationService.createChallenge(challenge));
    }

    @PutMapping("/admin/gamification/challenges/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReadingChallenge> updateChallenge(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        ReadingChallenge payload = new ReadingChallenge();
        applyChallengBody(payload, body);
        return ResponseEntity.ok(gamificationService.updateChallenge(id, payload));
    }

    @DeleteMapping("/admin/gamification/challenges/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteChallenge(@PathVariable Long id) {
        gamificationService.deleteChallenge(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/gamification/challenges/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReadingChallenge> toggleChallenge(@PathVariable Long id) {
        return ResponseEntity.ok(gamificationService.toggleChallengeActive(id));
    }

    private void applyChallengBody(ReadingChallenge challenge, Map<String, Object> body) {
        if (body.get("name") != null) challenge.setNameVi(body.get("name").toString());
        if (body.get("nameEn") != null) challenge.setNameEn(body.get("nameEn").toString());
        else if (challenge.getNameVi() != null && challenge.getNameEn() == null)
            challenge.setNameEn(challenge.getNameVi());
        if (body.get("description") != null) challenge.setDescriptionVi(body.get("description").toString());
        if (body.get("startDate") != null)
            challenge.setStartDate(java.time.LocalDate.parse(body.get("startDate").toString().substring(0, 10)));
        if (body.get("endDate") != null)
            challenge.setEndDate(java.time.LocalDate.parse(body.get("endDate").toString().substring(0, 10)));
        if (body.get("targetCount") != null)
            challenge.setTargetBooks(((Number) body.get("targetCount")).intValue());
        if (body.get("rewardPoints") != null)
            challenge.setPointsReward(((Number) body.get("rewardPoints")).intValue());
        if (body.get("active") != null)
            challenge.setIsActive(Boolean.parseBoolean(body.get("active").toString()));
    }

    // ============ ADMIN REWARDS ============

    @GetMapping("/admin/gamification/rewards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Reward>> getAllRewardsForAdmin() {
        return ResponseEntity.ok(gamificationService.getAllRewards());
    }

    @PostMapping("/admin/gamification/rewards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Reward> createReward(@RequestBody Reward reward) {
        return ResponseEntity.ok(gamificationService.createReward(reward));
    }

    @PutMapping("/admin/gamification/rewards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Reward> updateReward(
            @PathVariable Long id,
            @RequestBody Reward reward) {
        return ResponseEntity.ok(gamificationService.updateReward(id, reward));
    }

    @DeleteMapping("/admin/gamification/rewards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReward(@PathVariable Long id) {
        gamificationService.deleteReward(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/gamification/rewards/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Reward> updateRewardStock(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> payload) {
        Integer stock = payload.get("stock");
        if (stock == null || stock < 0) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(gamificationService.updateRewardStock(id, stock));
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
}