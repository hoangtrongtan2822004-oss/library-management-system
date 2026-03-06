import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

// ── NgModule-based components (declared in AdminModule) ──────────────────────
import { DashboardComponent } from './dashboard/dashboard.component';
import { LoanManagementComponent } from './loan-management/loan-management.component';
import { ManageFinesComponent } from './manage-fines/manage-fines.component';
import { ManageReviewsComponent } from './manage-reviews/manage-reviews.component';
import { AuditLogsComponent } from './audit-logs/audit-logs.component';
import { MemberCardsComponent } from './member-cards/member-cards.component';
import { CreateLoanComponent } from './create-loan/create-loan.component';
import { AdminSettingsComponent } from './admin-settings/admin-settings.component';
import { RenewalsComponent } from './renewals/renewals.component';

// ── Standalone components (self-contained, no need to declare) ────────────────
import { ManageCategoriesComponent } from './manage-categories/manage-categories.component';
import { ManageAuthorsComponent } from './manage-authors/manage-authors.component';
import { ImportExportComponent } from './import-export/import-export.component';
import { AdminNewsComponent } from './admin-news/admin-news.component';
import { AdminScannerComponent } from './admin-scanner/admin-scanner.component';

/**
 * Child routes for the lazy-loaded AdminModule.
 * Parent path fragment ('admin') is defined in AppRoutingModule.
 * AuthGuard + ROLE_ADMIN check is applied on the parent, so no need to repeat here.
 */
const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

  // ── Dashboard ──────────────────────────────────────────────────────────────
  { path: 'dashboard', component: DashboardComponent },

  // ── Circulation ────────────────────────────────────────────────────────────
  { path: 'loans', component: LoanManagementComponent },
  { path: 'fines', component: ManageFinesComponent },
  { path: 'renewals', component: RenewalsComponent },
  { path: 'create-loan', component: CreateLoanComponent },

  // ── Catalogue ──────────────────────────────────────────────────────────────
  { path: 'categories', component: ManageCategoriesComponent },
  { path: 'authors', component: ManageAuthorsComponent },
  { path: 'import-export', component: ImportExportComponent },

  // ── Community ──────────────────────────────────────────────────────────────
  { path: 'reviews', component: ManageReviewsComponent },
  { path: 'news', component: AdminNewsComponent },

  // ── Operations ─────────────────────────────────────────────────────────────
  { path: 'scanner', component: AdminScannerComponent },
  { path: 'member-cards', component: MemberCardsComponent },
  { path: 'audit-logs', component: AuditLogsComponent },
  { path: 'settings', component: AdminSettingsComponent },

  // ── Gamification (nested lazy) ─────────────────────────────────────────────
  {
    path: 'gamification',
    loadChildren: () =>
      import('./gamification/gamification.routes').then(
        (m) => m.gamificationRoutes,
      ),
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class AdminRoutingModule {}
