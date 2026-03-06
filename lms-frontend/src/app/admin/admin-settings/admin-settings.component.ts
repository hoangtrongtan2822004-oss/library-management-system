import { Component, OnInit } from '@angular/core';
import { AdminService } from '../../services/admin.service';
import { ToastrService } from 'ngx-toastr';
import {
  GroupedSettingsResponse,
  CategoryGroup,
  SettingDto,
  SettingDataType,
  SettingCategory,
} from '../../models/setting';

@Component({
  selector: 'app-admin-settings',
  templateUrl: './admin-settings.component.html',
  styleUrls: ['./admin-settings.component.css'],
  standalone: false,
})
export class AdminSettingsComponent implements OnInit {
  groupedSettings: GroupedSettingsResponse | null = null;
  categories: string[] = [];
  filteredCategories: string[] = [];
  categoryQuery: string = '';
  activeTab: string = 'LOAN_POLICY';
  loading = false;
  savingKeys = new Set<string>();
  resettingKeys = new Set<string>();

  // Expose enums to template
  SettingDataType = SettingDataType;
  SettingCategory = SettingCategory;

  // Reset confirmation modal state
  showResetModal = false;
  resetModalKey: string = '';
  resetModalTitle: string = '';
  resetModalDefaultValue: string = '';
  isResettingCategory = false;

  // Create setting modal state
  showCreateModal = false;
  isSubmittingCreate = false;
  createForm: {
    key: string;
    value: string;
    defaultValue: string;
    description: string;
    category: string;
    dataType: string;
  } = this.emptyCreateForm();

  dataTypeOptions = [
    { label: 'Văn bản (TEXT)', value: 'TEXT' },
    { label: 'Số (NUMBER)', value: 'NUMBER' },
    { label: 'Bật/Tắt (BOOLEAN)', value: 'BOOLEAN' },
    { label: 'Văn bản dài (TEXTAREA)', value: 'TEXTAREA' },
    { label: 'Giờ (TIME)', value: 'TIME' },
  ];
  categoryOptions = [
    { label: 'Chính sách mượn trả', value: 'LOAN_POLICY' },
    { label: 'Email & Thông báo', value: 'EMAIL_NOTIFICATION' },
    { label: 'Hệ thống', value: 'SYSTEM' },
  ];

  constructor(
    private adminService: AdminService,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.adminService.getGroupedSettings().subscribe({
      next: (response) => {
        this.groupedSettings = response;
        this.categories = Object.keys(response.groups);
        this.filteredCategories = [...this.categories];
        if (this.categories.length > 0 && !this.activeTab) {
          this.activeTab = this.categories[0];
        }
      },
      error: () => this.toastr.error('Không tải được cấu hình'),
      complete: () => (this.loading = false),
    });
  }

  filterCategories(): void {
    const q = this.categoryQuery.toLowerCase().trim();
    if (!q) {
      this.filteredCategories = [...this.categories];
      return;
    }
    this.filteredCategories = this.categories.filter((cat) =>
      this.getCategoryDisplayName(cat).toLowerCase().includes(q),
    );
  }

  refreshGroup(): void {
    this.load();
    this.toastr.info('Dữ liệu đã được làm mới', '', { timeOut: 1500 });
  }

  getActiveGroup(): CategoryGroup | null {
    if (!this.groupedSettings || !this.activeTab) return null;
    return this.groupedSettings.groups[this.activeTab] || null;
  }

  saveSetting(setting: SettingDto): void {
    // Validate based on dataType
    if (setting.dataType === SettingDataType.NUMBER) {
      const num = Number(setting.value);
      if (isNaN(num)) {
        this.toastr.warning('Giá trị phải là số hợp lệ');
        return;
      }
      if (num < 0) {
        this.toastr.warning('Không được nhập số âm');
        return;
      }
    }

    this.savingKeys.add(setting.key);
    this.adminService.updateSetting(setting.key, setting.value).subscribe({
      next: () => {
        this.toastr.success(`Đã lưu: ${setting.description || setting.key}`);
        this.load(); // Reload to get updated audit info
      },
      error: () => this.toastr.error('Lưu thiết lập thất bại'),
      complete: () => this.savingKeys.delete(setting.key),
    });
  }

  isSaving(key: string): boolean {
    return this.savingKeys.has(key);
  }

  isResetting(key: string): boolean {
    return this.resettingKeys.has(key);
  }

  // Reset modal handlers
  openResetModal(setting: SettingDto): void {
    if (!setting.defaultValue) {
      this.toastr.warning('Thiết lập này không có giá trị mặc định');
      return;
    }
    this.resetModalKey = setting.key;
    this.resetModalTitle = setting.description || setting.key;
    this.resetModalDefaultValue = setting.defaultValue;
    this.isResettingCategory = false;
    this.showResetModal = true;
  }

  openResetCategoryModal(): void {
    const group = this.getActiveGroup();
    if (!group) return;

    this.resetModalKey = this.activeTab;
    this.resetModalTitle = `tất cả thiết lập trong "${group.displayName}"`;
    this.resetModalDefaultValue = '';
    this.isResettingCategory = true;
    this.showResetModal = true;
  }

  confirmReset(): void {
    if (this.isResettingCategory) {
      this.resetCategory();
    } else {
      this.resetSingleSetting();
    }
  }

  resetSingleSetting(): void {
    this.resettingKeys.add(this.resetModalKey);
    this.adminService.resetSettingToDefault(this.resetModalKey).subscribe({
      next: () => {
        this.toastr.success('Đã khôi phục về giá trị mặc định');
        this.load();
        this.closeResetModal();
      },
      error: (err) => {
        const msg = err.error?.message || 'Khôi phục thất bại';
        this.toastr.error(msg);
      },
      complete: () => this.resettingKeys.delete(this.resetModalKey),
    });
  }

  resetCategory(): void {
    this.adminService.resetCategoryToDefaults(this.activeTab).subscribe({
      next: (response: any) => {
        this.toastr.success(response.message || 'Đã khôi phục danh mục');
        this.load();
        this.closeResetModal();
      },
      error: (err) => {
        const msg = err.error?.message || 'Khôi phục thất bại';
        this.toastr.error(msg);
      },
    });
  }

  closeResetModal(): void {
    this.showResetModal = false;
    this.resetModalKey = '';
    this.resetModalTitle = '';
    this.resetModalDefaultValue = '';
    this.isResettingCategory = false;
  }

  // ==================== Create Setting Modal ====================
  private emptyCreateForm() {
    return {
      key: '',
      value: '',
      defaultValue: '',
      description: '',
      category: this.activeTab || 'SYSTEM',
      dataType: 'TEXT',
    };
  }

  openCreateSettingModal(): void {
    this.createForm = {
      key: '',
      value: '',
      defaultValue: '',
      description: '',
      category: this.activeTab || 'SYSTEM',
      dataType: 'TEXT',
    };
    this.isSubmittingCreate = false;
    this.showCreateModal = true;
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
    this.isSubmittingCreate = false;
  }

  submitCreateSetting(): void {
    const key = this.createForm.key.trim().toUpperCase();
    if (!key) {
      this.toastr.warning('Khóa cấu hình không được để trống');
      return;
    }
    if (!this.createForm.value.trim()) {
      this.toastr.warning('Giá trị không được để trống');
      return;
    }

    this.isSubmittingCreate = true;
    this.adminService
      .createSetting({
        key,
        value: this.createForm.value.trim(),
        defaultValue: this.createForm.defaultValue.trim() || undefined,
        description: this.createForm.description.trim() || undefined,
        category: this.createForm.category,
        dataType: this.createForm.dataType,
      })
      .subscribe({
        next: () => {
          this.toastr.success(
            `Cấu hình “${key}” đã được tạo thành công!`,
            'Tạo mới thành công',
            { timeOut: 3500, progressBar: true },
          );
          this.closeCreateModal();
          this.activeTab = this.createForm.category || this.activeTab;
          this.load();
        },
        error: (err) => {
          const msg =
            err.error?.message ||
            'Tạo cấu hình thất bại. Khóa có thể đã tồn tại.';
          this.toastr.error(msg, 'Lỗi tạo cấu hình', { progressBar: true });
          this.isSubmittingCreate = false;
        },
      });
  }

  formatDateTime(dateTime: string | null): string {
    if (!dateTime) return 'Chưa cập nhật';
    const date = new Date(dateTime);
    return date.toLocaleString('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  getCategoryIcon(category: string): string {
    const group = this.groupedSettings?.groups[category];
    return group?.icon || '⚙️';
  }

  getCategoryDisplayName(category: string): string {
    const group = this.groupedSettings?.groups[category];
    return group?.displayName || category;
  }
}
