// cypress/support/e2e.ts
// This file is automatically included before every Cypress test.

import './commands';

// Global error handling: suppress Angular zone errors that don't affect UX
Cypress.on('uncaught:exception', (err) => {
  // Angular zone.js sometimes throws for 3rd-party lib errors – safe to ignore
  if (
    err.message.includes('ResizeObserver') ||
    err.message.includes('Non-Error promise rejection')
  ) {
    return false;
  }
  return true;
});
