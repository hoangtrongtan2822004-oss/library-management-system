import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiService } from '../../services/api.service';

export interface Badge {
  id?: number;
  name: string;
  description: string;
  iconUrl?: string;
  requirement: number;
  points: number;
  tier?: string;
}

export interface Challenge {
  id?: number;
  name: string;
  description: string;
  startDate: string;
  endDate: string;
  targetCount: number;
  rewardPoints: number;
  active?: boolean;
}

export interface RewardItem {
  id?: number;
  name: string;
  description: string;
  pointsCost: number;
  stock: number;
  imageUrl?: string;
  active?: boolean;
}

interface BackendReward {
  id?: number;
  name: string;
  description: string;
  icon?: string;
  cost: number;
  category?: string;
  available?: boolean;
  maxRedemptions?: number | null;
}

export interface LeaderboardEntry {
  userId: number;
  userName: string;
  totalPoints: number;
  level: number;
  badgeCount: number;
  rank?: number;
}

@Injectable({
  providedIn: 'root',
})
export class GamificationAdminService {
  constructor(
    private http: HttpClient,
    private apiService: ApiService,
  ) {}

  // ==================== BADGES ====================

  getAllBadges(): Observable<Badge[]> {
    return this.http.get<Badge[]>(
      this.apiService.buildUrl('/public/gamification/badges'),
    );
  }

  createBadge(badge: Badge): Observable<Badge> {
    return this.http.post<Badge>(
      this.apiService.buildUrl('/admin/gamification/badges'),
      badge,
    );
  }

  updateBadge(id: number, badge: Badge): Observable<Badge> {
    return this.http.put<Badge>(
      this.apiService.buildUrl(`/admin/gamification/badges/${id}`),
      badge,
    );
  }

  deleteBadge(id: number): Observable<void> {
    return this.http.delete<void>(
      this.apiService.buildUrl(`/admin/gamification/badges/${id}`),
    );
  }

  uploadBadgeIcon(id: number, file: File): Observable<Badge> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Badge>(
      this.apiService.buildUrl(`/admin/gamification/badges/${id}/icon`),
      formData,
    );
  }

  // ==================== CHALLENGES ====================

  getActiveChallenges(): Observable<Challenge[]> {
    return this.http.get<Challenge[]>(
      this.apiService.buildUrl('/public/gamification/challenges'),
    );
  }

  getAllChallenges(): Observable<Challenge[]> {
    return this.http.get<Challenge[]>(
      this.apiService.buildUrl('/admin/gamification/challenges'),
    );
  }

  createChallenge(challenge: Challenge): Observable<Challenge> {
    return this.http.post<Challenge>(
      this.apiService.buildUrl('/admin/gamification/challenges'),
      challenge,
    );
  }

  updateChallenge(id: number, challenge: Challenge): Observable<Challenge> {
    return this.http.put<Challenge>(
      this.apiService.buildUrl(`/admin/gamification/challenges/${id}`),
      challenge,
    );
  }

  deleteChallenge(id: number): Observable<void> {
    return this.http.delete<void>(
      this.apiService.buildUrl(`/admin/gamification/challenges/${id}`),
    );
  }

  toggleChallengeActive(id: number): Observable<Challenge> {
    return this.http.patch<Challenge>(
      this.apiService.buildUrl(`/admin/gamification/challenges/${id}/toggle`),
      {},
    );
  }

  // ==================== REWARDS ====================

  private toRewardItem(reward: BackendReward): RewardItem {
    return {
      id: reward.id,
      name: reward.name,
      description: reward.description,
      pointsCost: reward.cost,
      stock: reward.maxRedemptions ?? 0,
      imageUrl: reward.icon,
      active: reward.available ?? true,
    };
  }

  private toBackendReward(reward: RewardItem): BackendReward {
    return {
      id: reward.id,
      name: reward.name,
      description: reward.description,
      icon: reward.imageUrl,
      cost: reward.pointsCost,
      category: 'special',
      available: reward.active ?? true,
      maxRedemptions: reward.stock,
    };
  }

  getAllRewards(): Observable<RewardItem[]> {
    return this.http
      .get<
        BackendReward[]
      >(this.apiService.buildUrl('/admin/gamification/rewards'))
      .pipe(
        map((rewards) => rewards.map((reward) => this.toRewardItem(reward))),
      );
  }

  createReward(reward: RewardItem): Observable<RewardItem> {
    return this.http
      .post<BackendReward>(
        this.apiService.buildUrl('/admin/gamification/rewards'),
        this.toBackendReward(reward),
      )
      .pipe(map((created) => this.toRewardItem(created)));
  }

  updateReward(id: number, reward: RewardItem): Observable<RewardItem> {
    return this.http
      .put<BackendReward>(
        this.apiService.buildUrl(`/admin/gamification/rewards/${id}`),
        this.toBackendReward(reward),
      )
      .pipe(map((updated) => this.toRewardItem(updated)));
  }

  deleteReward(id: number): Observable<void> {
    return this.http.delete<void>(
      this.apiService.buildUrl(`/admin/gamification/rewards/${id}`),
    );
  }

  updateRewardStock(id: number, newStock: number): Observable<RewardItem> {
    return this.http
      .patch<BackendReward>(
        this.apiService.buildUrl(`/admin/gamification/rewards/${id}/stock`),
        { stock: newStock },
      )
      .pipe(map((updated) => this.toRewardItem(updated)));
  }

  // ==================== LEADERBOARD & STATS ====================

  getLeaderboard(limit: number = 50): Observable<LeaderboardEntry[]> {
    return this.http.get<LeaderboardEntry[]>(
      this.apiService.buildUrl(
        `/public/gamification/leaderboard?limit=${limit}`,
      ),
    );
  }

  getUserStats(userId: number): Observable<any> {
    return this.http.get(
      this.apiService.buildUrl(`/admin/gamification/users/${userId}/stats`),
    );
  }

  awardPoints(userId: number, points: number, reason: string): Observable<any> {
    return this.http.post(
      this.apiService.buildUrl(`/admin/gamification/users/${userId}/points`),
      null,
      { params: { points: points.toString(), reason } },
    );
  }

  awardBadge(userId: number, badgeId: number): Observable<any> {
    return this.http.post(
      this.apiService.buildUrl(`/admin/gamification/users/${userId}/badges`),
      { badgeId },
    );
  }
}
