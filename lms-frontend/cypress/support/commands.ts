// cypress/support/commands.ts
// Custom Cypress commands for LMS E2E tests

declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Log in via the UI form.
       * @example cy.login('admin', 'admin123')
       */
      login(username: string, password: string): Chainable<void>;

      /**
       * Log in via the API (bypasses UI – much faster).
       * Stores the JWT token in localStorage so the app authenticates on next visit.
       * @example cy.loginApi('admin', 'admin123')
       */
      loginApi(username: string, password: string): Chainable<void>;

      /**
       * Navigate to a page and wait for the Angular app to finish rendering.
       */
      goTo(path: string): Chainable<void>;
    }
  }
}

// ── UI Login ─────────────────────────────────────────────────────────────────
Cypress.Commands.add('login', (username: string, password: string) => {
  cy.visit('/login');
  cy.get('input[name="username"]').clear().type(username);
  cy.get('input[name="password"]').clear().type(password);
  cy.get('button[type="submit"]').click();
  // Wait until we're redirected away from login
  cy.url({ timeout: 10_000 }).should('not.include', '/login');
});

// ── API Login (preferred – faster) ───────────────────────────────────────────
Cypress.Commands.add('loginApi', (username: string, password: string) => {
  cy.request({
    method: 'POST',
    url: 'http://localhost:8080/api/auth/login',
    body: { username, password },
    headers: { 'Content-Type': 'application/json' },
  }).then((resp) => {
    const token = resp.body.token ?? resp.body.jwtToken;
    expect(token, 'JWT token phải tồn tại trong response').to.be.a('string');
    window.localStorage.setItem('jwtToken', token);
    window.localStorage.setItem('userName', username);
    // Store roles if available
    if (resp.body.roles) {
      window.localStorage.setItem('userRole', JSON.stringify(resp.body.roles));
    }
  });
});

// ── Navigation helper ─────────────────────────────────────────────────────────
Cypress.Commands.add('goTo', (path: string) => {
  cy.visit(path);
  // Short pause to let Angular's change detection settle after navigation
  cy.get('app-root', { timeout: 8_000 }).should('exist');
});

export {};
