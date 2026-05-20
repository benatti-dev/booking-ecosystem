import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'distance', standalone: false })
export class DistancePipe implements PipeTransform {
  transform(metres: number | null | undefined): string {
    if (metres == null) return '';
    return metres < 1000
      ? `${Math.round(metres)} m`
      : `${(metres / 1000).toFixed(1)} km`;
  }
}
