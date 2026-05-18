import { Component, Input, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormArray, FormGroup } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

interface ScheduleBreakDto { startTime: string; endTime: string; }
interface ScheduleRuleDto {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  isWorkingDay: boolean;
  breaks: ScheduleBreakDto[];
}

const DAY_NAMES: Record<number, string> = {
  0: 'Sun', 1: 'Mon', 2: 'Tue', 3: 'Wed', 4: 'Thu', 5: 'Fri', 6: 'Sat'
};

@Component({
  selector: 'app-schedule-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './schedule-management.component.html',
  styleUrl: './schedule-management.component.scss',
})
export class ScheduleManagementComponent implements OnInit {
  @Input() employeeId!: number;

  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  readonly DAY_NAMES = DAY_NAMES;

  // 0=Sunday … 6=Saturday
  days = [1, 2, 3, 4, 5, 6, 0]; // Mon-Sun display order

  loading = true;
  saving = false;
  saved = false;

  form = this.fb.group({
    rules: this.fb.array(this.days.map(d => this.buildRuleGroup(d)))
  });

  get rulesArray(): FormArray { return this.form.get('rules') as FormArray; }

  ngOnInit(): void {
    this.http.get<any>(`${environment.apiUrl}/employees/${this.employeeId}/schedule`)
      .subscribe({
        next: data => {
          this.loading = false;
          if (data.rules?.length) {
            this.days.forEach((day, idx) => {
              const existing = data.rules.find((r: any) => r.dayOfWeek === day);
              if (existing) {
                this.rulesArray.at(idx).patchValue({
                  dayOfWeek: day,
                  isWorkingDay: existing.isWorkingDay,
                  startTime: existing.startTime?.substring(0, 5) ?? '09:00',
                  endTime: existing.endTime?.substring(0, 5) ?? '18:00',
                });
              }
            });
          }
        },
        error: () => { this.loading = false; }
      });
  }

  save(): void {
    this.saving = true;
    this.saved = false;
    const rules: ScheduleRuleDto[] = this.rulesArray.value.map((v: any) => ({
      dayOfWeek: v.dayOfWeek,
      startTime: v.startTime + ':00',
      endTime: v.endTime + ':00',
      isWorkingDay: v.isWorkingDay,
      breaks: [],
    }));

    this.http.put<any>(`${environment.apiUrl}/employees/${this.employeeId}/schedule`, rules)
      .subscribe({
        next: () => { this.saving = false; this.saved = true; },
        error: () => { this.saving = false; }
      });
  }

  private buildRuleGroup(dayOfWeek: number): FormGroup {
    return this.fb.group({
      dayOfWeek: [dayOfWeek],
      isWorkingDay: [dayOfWeek >= 1 && dayOfWeek <= 5], // Mon-Fri working by default
      startTime: ['09:00'],
      endTime: ['18:00'],
    });
  }
}
