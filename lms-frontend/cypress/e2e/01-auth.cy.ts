/**
 * E2E: Authentication flows
 *
 * Scenarios:
 * 1. Successful admin login → redirect to home/dashboard
 * 2. Wrong password → error message shown
 * 3. Unauthenticated access to protected admin route → redirect to login
 * 4. Remember-me: token persists across page reload
 * 5. Logout clears session
 */
describe('Authentication', () => {
  beforeEach(() => {
    cy.clearLocalStorage();
    cy.clearCookies();
  });

  // ── 1. Successful login ──────────────────────────────────────────────────
  it('admin đăng nhập thành công và được redirect', () => {
    cy.visit('/login');

    cy.get('h1').should('contain', 'Đăng nhập');

    cy.get('input[name="username"]').type('admin');
    cy.get('input[name="password"]').type('admin123');
    cy.get('button[type="submit"]').click();

    // Should end up somewhere that is NOT /login or /forbidden
    cy.url({ timeout: 10_000 }).should('not.include', '/login');
    cy.url().should('not.include', '/forbidden');
  });

  // ── 2. Wrong password ────────────────────────────────────────────────────
  it('hiển thị lỗi khi đăng nhập sai mật khẩu', () => {
    cy.visit('/login');

    cy.get('input[name="username"]').type('admin');
    cy.get('input[name="password"]').type('wrong_password_xyz');
    cy.get('button[type="submit"]').click();

    // Error alert should appear
    cy.get('.alert-danger', { timeout: 6_000 }).should('be.visible');
    // Should still be on login page
    cy.url().should('include', '/login');
  });

  // ── 3. Guard redirects unauthenticated user ──────────────────────────────
  it('truy cập /admin/dashboard khi chưa login → redirect về /login', () => {
    cy.visit('/admin/dashboard');
    cy.url({ timeout: 8_000 }).should('include', '/login');
  });

  // ── 4. Admin dashboard accessible after login ────────────────────────────
  it('admin có thể vào Dashboard sau khi đăng nhập', () => {
    cy.loginApi('admin', 'admin123');
    cy.goTo('/admin/dashboard');

    // Dashboard should render some metrics
    cy.contains('Dashboard', { timeout: 8_000 }).should('be.visible');
  });

  // ── 5. Logout clears session ─────────────────────────────────────────────
  it('đăng xuất xóa token và chuyển về trang login', () => {
    cy.loginApi('admin', 'admin123');
    cy.goTo('/');

    // Click logout in header
    cy.get('[data-cy="logout-btn"], a[href="/logout"], .logout-btn')
      .first()
      .click({ force: true });

    cy.url({ timeout: 6_000 }).should(
      'satisfy',
      (url: string) =>
        url.includes('/login') ||
        url.includes('/logout') ||
        url === `${Cypress.config('baseUrl')}/`,
    );

    // Token should be gone
    cy.window().then((win) => {
      expect(win.localStorage.getItem('jwtToken')).to.be.null;
    });
  });
});
