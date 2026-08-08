import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, of } from 'rxjs';
import { SystemStatus } from '../models/system-status.model';

@Injectable({ providedIn: 'root' })
export class SystemStatusService {
  private readonly http = inject(HttpClient);

  getStatus(): Observable<SystemStatus> {
    return this.http.get<SystemStatus>('/api/system/status').pipe(
      catchError(() => of<SystemStatus>({ status: 'DOWN', postgres: 'DOWN', redis: 'DOWN' })),
    );
  }
}
