import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { OAuth2Service } from '../../services/oauth2.service';
import { UserAuthService } from '../../services/user-auth.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-google-callback',
  templateUrl: './google-callback.component.html',
  styleUrls: ['./google-callback.component.css'],
  standalone: false,
})
export class GoogleCallbackComponent implements OnInit {
  loading = true;
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private oauth2Service: OAuth2Service,
    private userAuthService: UserAuthService,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    // Get authorization code from URL
    const code = this.route.snapshot.queryParamMap.get('code');
    const error = this.route.snapshot.queryParamMap.get('error');

    if (error) {
      this.error = 'Đăng nhập Google bị hủy hoặc thất bại';
      this.toastr.error(this.error);
      setTimeout(() => this.router.navigate(['/login']), 2000);
      return;
    }

    if (!code) {
      this.error = 'Không tìm thấy mã xác thực';
      this.toastr.error(this.error);
      setTimeout(() => this.router.navigate(['/login']), 2000);
      return;
    }

    // Exchange code for token
    this.oauth2Service.googleCallback(code).subscribe({
      next: (response) => {
        // Save authentication data
        this.userAuthService.setToken(response.token);
        this.userAuthService.setRoles([response.role]);

        // Show success message
        if (response.isNewUser) {
          this.toastr.success(
            'Tài khoản mới đã được tạo! Chào mừng bạn đến với thư viện.',
          );
        } else {
          this.toastr.success(`Chào mừng trở lại, ${response.fullName}!`);
        }

        // Redirect based on role
        const isAdmin =
          response.role === 'ADMIN' || response.role === 'ROLE_ADMIN';
        const redirectUrl = isAdmin ? '/admin/dashboard' : '/';

        setTimeout(() => {
          this.router.navigate([redirectUrl]);
        }, 1000);
      },
      error: (err) => {
        console.error('OAuth2 callback error:', err);
        this.error = err.error?.message || 'Đăng nhập Google thất bại';
        this.toastr.error(this.error || 'Đăng nhập Google thất bại');
        this.loading = false;

        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
    });
  }
}
