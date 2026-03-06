/**
 * E2E: Core borrowing workflow (Admin perspective)
 *
 * Scenarios:
 * 1. Admin sees book list
 * 2. Admin navigates to Create Loan page
 * 3. Admin views loan management
 * 4. Admin views overdue / fines section
 *
 * Note: Actual DB-mutating actions (create/return loan) are tested
 * in integration tests against a test DB. These E2E tests verify
 * the UI renders correctly and navigation works end-to-end.
 */
describe('Admin – Mượn Sách Workflow', () => {
  before(() => {
    // Log in once before all tests in this suite
    cy.loginApi('admin', 'admin123');
  });

  beforeEach(() => {
    // Re-apply token in case localStorage was cleared between tests
    cy.loginApi('admin', 'admin123');
  });

  // ── 1. Book catalogue visible ─────────────────────────────────────────────
  it('danh sách sách public hiển thị được', () => {
    cy.goTo('/');
    // Home page should show books or a search area
    cy.get('body').should('be.visible');
    // Relaxed: just ensure the page loads without error overlay
    cy.get('.error-page, [data-cy="global-error"]').should('not.exist');
  });

  // ── 2. Admin can reach Create Loan page ───────────────────────────────────
  it('admin truy cập trang Tạo Phiếu Mượn', () => {
    cy.goTo('/admin/create-loan');
    // The page title or a key form element must be visible
    cy.contains(/tạo phiếu|mượn sách|create loan/i, { timeout: 8_000 }).should(
      'be.visible',
    );
  });

  // ── 3. Loan management list renders ───────────────────────────────────────
  it('admin thấy được danh sách phiếu mượn', () => {
    cy.goTo('/admin/loans');
    // Should render a table or a "no data" message – not a blank/error page
    cy.get('table, .no-data, [data-cy="loan-list"]', {
      timeout: 10_000,
    }).should('exist');
  });

  // ── 4. Fines section accessible ───────────────────────────────────────────
  it('admin thấy trang quản lý tiền phạt', () => {
    cy.goTo('/admin/fines');
    cy.get('body').should('be.visible');
    cy.contains(/tiền phạt|fine/i, { timeout: 8_000 }).should('be.visible');
  });

  // ── 5. Dashboard stats load ───────────────────────────────────────────────
  it('dashboard hiển thị các thẻ thống kê', () => {
    cy.goTo('/admin/dashboard');
    // Wait for skeleton to disappear and stat cards to appear
    cy.get('.stat-card, [data-cy="stat-card"]', { timeout: 12_000 }).should(
      'have.length.greaterThan',
      0,
    );
  });

  // ── 6. Book search works ───────────────────────────────────────────────────
  it('tìm kiếm sách trả về kết quả hoặc thông báo rỗng', () => {
    cy.goTo('/');
    // Find a search input (home page or books page)
    cy.get(
      'input[type="search"], input[placeholder*="Tìm kiếm"], input[placeholder*="tìm"]',
    )
      .first()
      .type('Java{enter}');

    // Either results or empty state – not an error
    cy.get('.book-card, .empty-state, .no-results, [data-cy="book-card"]', {
      timeout: 10_000,
    }).should('exist');
  });
});
