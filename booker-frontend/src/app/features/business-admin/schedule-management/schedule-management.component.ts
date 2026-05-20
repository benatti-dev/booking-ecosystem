import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormArray, FormGroup } from '@angular/forms';
import { ScheduleService, ScheduleRuleDto } from '../../../core/schedule/schedule.service';

const DAY_NAMES: Record<number, string> = {
  0: 'Sun', 1: 'Mon', 2: 'Tue', 3: 'Wed', 4: 'Thu', 5: 'Fri', 6: 'Sat',
};

@Component({
  selector: 'app-schedule-management',
  templateUrl: './schedule-management.component.html',
  styleUrl: './schedule-management.component.scss',
  standalone: false,
})
export class ScheduleManagementComponent implements OnInit {
  @Input() employeeId!: number;

  readonly DAY_NAMES = DAY_NAMES;

  // 0=Sunday … 6=Saturday
  days = [1, 2, 3, 4, 5, 6, 0]; // Mon-Sun display order

  loading = true;
  saving = false;
  saved = false;

  form = this.fb.group({
    rules: this.fb.array(this.days.map(d => this.buildRuleGroup(d))),
  });

  get rulesArray(): FormArray { return this.form.get('rules') as FormArray; }

  constructor(
    private readonly fb: FormBuilder,
    private readonly scheduleSvc: ScheduleService,
  ) {}

  ngOnInit(): void {
    this.scheduleSvc.getSchedule(this.employeeId).subscribe({
      next: data => {
        this.loading = false;
        if (data.rules?.length) {
          this.days.forEach((day, idx) => {
            const existing = data.rules.find((r: ScheduleRuleDto) => r.dayOfWeek === day);
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
    const rules: ScheduleRuleDto[] = (this.rulesArray.value as ScheduleRuleDto[]).map(v => ({
      dayOfWeek: v.dayOfWeek,
      startTime: v.startTime + ':00',
      endTime: v.endTime + ':00',
      isWorkingDay: v.isWorkingDay,
      breaks: [],
    }));

    this.scheduleSvc.saveSchedule(this.employeeId, rules).subscribe({
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
