import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { SharedModule } from '../shared/shared.module';
import { AdminRoutingModule } from './admin-routing.module';

// ── NgModule-based admin page components ──────────────────────────────────────
import { DashboardComponent } from './dashboard/dashboard.component';
import { LoanManagementComponent } from './loan-management/loan-management.component';
import { ManageFinesComponent } from './manage-fines/manage-fines.component';
import { ManageReviewsComponent } from './manage-reviews/manage-reviews.component';
import { AuditLogsComponent } from './audit-logs/audit-logs.component';
import { MemberCardsComponent } from './member-cards/member-cards.component';
import { CreateLoanComponent } from './create-loan/create-loan.component';
import { AdminSettingsComponent } from './admin-settings/admin-settings.component';
import { RenewalsComponent } from './renewals/renewals.component';
import { ReportsComponent } from './reports/reports.component';

/**
 * 🔐 AdminModule — Lazy-loaded feature module for all admin pages.
 *
 * Loaded via: AppRoutingModule → { path: 'admin', loadChildren: AdminModule }
 *
 * Benefits:
 * • Regular users never download this code bundle (~250 KB+ saved on initial load)
 * • Independent compilation unit — changes here rebuild only this chunk
 * • Clear boundaries: all admin dependencies declared here, not in AppModule
 *
 * Architecture notes:
 * • NgModule-based components: declared below
 * • Standalone components (ManageCategories, ManageAuthors, ImportExport,
 *   AdminNews, AdminScanner): routed directly as components — Angular loads
 *   them without requiring declaration here
 * • Gamification: double-lazy (nested loadChildren inside this module)
 */
@NgModule({
  declarations: [
    DashboardComponent,
    LoanManagementComponent,
    ManageFinesComponent,
    ManageReviewsComponent,
    AuditLogsComponent,
    MemberCardsComponent,
    CreateLoanComponent,
    AdminSettingsComponent,
    RenewalsComponent,
    ReportsComponent,
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    SharedModule,
    AdminRoutingModule,
  ],
})
export class AdminModule {}
