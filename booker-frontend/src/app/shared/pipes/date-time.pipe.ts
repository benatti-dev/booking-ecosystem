import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'dateTime', standalone: false })
export class DateTimePipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) return '';
    const d = new Date(value);
    if (isNaN(d.getTime())) return '';
    return d.toLocaleString('en-US', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}
