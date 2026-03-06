import { ChangeDetectorRef, Component } from '@angular/core';
import { Router, NavigationEnd, RouterOutlet } from '@angular/router';
import {
  trigger,
  transition,
  style,
  query,
  group,
  animate,
} from '@angular/animations';
import { filter } from 'rxjs/operators';
import { ThemeService } from './services/theme.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  standalone: false,
  animations: [
    trigger('routeAnimations', [
      transition('* <=> *', [
        query(
          ':enter, :leave',
          style({
            position: 'relative',
            width: '100%',
            opacity: 0,
            transform: 'translateY(6px)',
          }),
          { optional: true },
        ),
        group([
          query(
            ':leave',
            [
              animate(
                '180ms ease',
                style({ opacity: 0, transform: 'translateY(-6px)' }),
              ),
            ],
            { optional: true },
          ),
          query(
            ':enter',
            [
              style({ opacity: 0, transform: 'translateY(12px)' }),
              animate(
                '240ms ease-out',
                style({ opacity: 1, transform: 'translateY(0)' }),
              ),
            ],
            { optional: true },
          ),
        ]),
      ]),
    ]),
  ],
})
export class AppComponent {
  isHomeRoute = false;
  title = 'lms-frontend';

  constructor(
    private router: Router,
    private themeService: ThemeService,
    private cdr: ChangeDetectorRef,
  ) {
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe(() => {
        this.updateFlag();
        // Flush state immediately so Angular's consistency check sees the settled
        // value — prevents NG0100 when outlet animation switches from 'root' to
        // the actual route animation key (e.g. 'forbidden') in dev mode.
        this.cdr.detectChanges();
      });
    this.updateFlag(); // set lần đầu khi load trang
  }

  prepareRoute(outlet: RouterOutlet) {
    // Guard against outlets that are not yet activated (prevents NG04012)
    if (!outlet || !outlet.isActivated) {
      return 'root';
    }

    return (
      outlet.activatedRouteData?.['animation'] ||
      outlet.activatedRoute?.snapshot?.url.join('/') ||
      'root'
    );
  }

  private updateFlag() {
    const url = this.router.url.split('?')[0];
    // chỉnh danh sách path được coi là Home nếu bạn dùng route khác
    this.isHomeRoute = url === '/' || url === '/home';
  }
}
