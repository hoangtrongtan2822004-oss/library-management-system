import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

export interface OAuth2LoginResponse {
  token: string;
  username: string;
  email: string;
  fullName: string;
  role: string;
  isNewUser: boolean;
}

export interface OAuth2Config {
  clientId: string;
  redirectUri: string;
}

@Injectable({
  providedIn: 'root',
})
export class OAuth2Service {
  constructor(
    private http: HttpClient,
    private apiService: ApiService,
  ) {}

  /**
   * Get Google OAuth2 configuration
   */
  getGoogleConfig(): Observable<OAuth2Config> {
    return this.http.get<OAuth2Config>(
      this.apiService.buildUrl('/auth/google/config'),
    );
  }

  /**
   * Exchange authorization code for JWT token
   */
  googleCallback(code: string): Observable<OAuth2LoginResponse> {
    return this.http.post<OAuth2LoginResponse>(
      this.apiService.buildUrl('/auth/google/callback'),
      { code },
    );
  }

  /**
   * Redirect to Google OAuth2 authorization page
   */
  initiateGoogleLogin(clientId: string, redirectUri: string): void {
    const authUrl =
      'https://accounts.google.com/o/oauth2/v2/auth?' +
      `client_id=${encodeURIComponent(clientId)}&` +
      `redirect_uri=${encodeURIComponent(redirectUri)}&` +
      'response_type=code&' +
      'scope=profile email&' +
      'access_type=offline&' +
      'prompt=consent';

    window.location.href = authUrl;
  }

  /**
   * Get Facebook OAuth2 configuration
   */
  getFacebookConfig(): Observable<OAuth2Config> {
    return this.http.get<OAuth2Config>(
      this.apiService.buildUrl('/auth/facebook/config'),
    );
  }

  /**
   * Exchange Facebook authorization code for JWT token
   */
  facebookCallback(code: string): Observable<OAuth2LoginResponse> {
    return this.http.post<OAuth2LoginResponse>(
      this.apiService.buildUrl('/auth/facebook/callback'),
      { code },
    );
  }

  /**
   * Redirect to Facebook OAuth2 authorization page
   */
  initiateFacebookLogin(clientId: string, redirectUri: string): void {
    const authUrl =
      'https://www.facebook.com/v18.0/dialog/oauth?' +
      `client_id=${encodeURIComponent(clientId)}&` +
      `redirect_uri=${encodeURIComponent(redirectUri)}&` +
      'response_type=code&' +
      'scope=public_profile,email';

    window.location.href = authUrl;
  }

  /**
   * Social login using provider access token (popup flows)
   * Backend should expose an endpoint to accept provider and accessToken
   */
  socialLogin(provider: string, accessToken: string) {
    return this.http.post<OAuth2LoginResponse>(
      this.apiService.buildUrl('/auth/social'),
      { provider, accessToken },
    );
  }
}
