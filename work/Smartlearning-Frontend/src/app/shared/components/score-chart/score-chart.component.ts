import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface ChartBar {
  label: string;
  value: number;
  /** Optional secondary value (e.g. max possible) shown as a lighter reference bar behind the main one. */
  maxValue?: number;
}

/**
 * Minimal SVG bar chart, no charting library dependency. Built for simple
 * "score per item" displays (assignment scores, course averages) — not a
 * general-purpose charting component. Bars scale to the largest maxValue
 * (or value, if no maxValue given) across the data set.
 */
@Component({
  selector: 'app-score-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './score-chart.component.html',
  styleUrl: './score-chart.component.scss'
})
export class ScoreChartComponent {
  @Input() bars: ChartBar[] = [];
  @Input() valueSuffix = '';

  hoveredIndex = signal<number | null>(null);

  maxScale = computed(() => {
    const values = this.bars.flatMap((b) => [b.value, b.maxValue ?? 0]);
    const max = Math.max(1, ...values);
    return max;
  });

  barHeightPercent(value: number): number {
    return Math.max(2, (value / this.maxScale()) * 100);
  }

  setHover(index: number | null) {
    this.hoveredIndex.set(index);
  }
}
