import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ScheduleBreakDto {
  startTime: string;
  endTime: string;
}

export interface ScheduleRuleDto {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  isWorkingDay: boolean;
  breaks: ScheduleBreakDto[];
}

export interface ScheduleResponse {
  rules: ScheduleRuleDto[];
}

@Injectable({ providedIn: 'root' })
export class ScheduleService {
  constructor(private readonly http: HttpClient) {}

  getSchedule(employeeId: number): Observable<ScheduleResponse> {
    return this.http.get<ScheduleResponse>(
      `${environment.apiUrl}/employees/${employeeId}/schedule`
    );
  }

  saveSchedule(employeeId: number, rules: ScheduleRuleDto[]): Observable<void> {
    return this.http.put<void>(
      `${environment.apiUrl}/employees/${employeeId}/schedule`,
      rules
    );
  }
}
