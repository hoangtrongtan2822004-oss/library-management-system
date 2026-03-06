import { Routes } from '@angular/router';
import { GamificationAdminComponent } from './gamification-admin.component';
import { ManageBadgesComponent } from './manage-badges/manage-badges.component';
import { ManageChallengesComponent } from './manage-challenges/manage-challenges.component';
import { ManageRewardsComponent } from './manage-rewards/manage-rewards.component';
import { ManageLeaderboardComponent } from './manage-leaderboard/manage-leaderboard.component';

export const gamificationRoutes: Routes = [
  {
    path: '',
    component: GamificationAdminComponent,
    children: [
      { path: '', redirectTo: 'badges', pathMatch: 'full' },
      { path: 'badges', component: ManageBadgesComponent },
      { path: 'challenges', component: ManageChallengesComponent },
      { path: 'rewards', component: ManageRewardsComponent },
      { path: 'leaderboard', component: ManageLeaderboardComponent },
    ],
  },
];
