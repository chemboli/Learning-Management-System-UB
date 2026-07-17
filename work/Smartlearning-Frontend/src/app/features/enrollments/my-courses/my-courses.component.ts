import { Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { EnrollmentService } from '../../../core/services/enrollment.service';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmService } from '../../../shared/components/confirm-dialog/confirm.service';
import { EnrollmentResponse } from '../../../core/models/models';

@Component({
  selector: 'app-my-courses',
  standalone: true,
  imports: [RouterLink, DatePipe],
  templateUrl: './my-courses.component.html'
})
export class MyCoursesComponent implements OnInit {
  enrollments: EnrollmentResponse[] = [];
  loading = false;
  unenrollingId: string | null = null;

  constructor(
    private enrollmentService: EnrollmentService,
    private toast: ToastService,
    private confirmService: ConfirmService
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.enrollmentService.getMyCourses().subscribe({
      next: (res) => {
        this.enrollments = res;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  async unenroll(e: EnrollmentResponse) {
    const ok = await this.confirmService.ask(
      `Unenroll from ${e.courseTitle}? You'll lose access to course-restricted materials.`,
      'Unenroll from course',
      'Unenroll'
    );

    if (!ok) return;

    this.unenrollingId = e.courseId;
    this.enrollmentService.unenroll(e.courseId).subscribe({
      next: () => {
        this.enrollments = this.enrollments.filter((x) => x.id !== e.id);
        this.unenrollingId = null;
        this.toast.success(`Unenrolled from ${e.courseCode}.`);
      },
      error: () => (this.unenrollingId = null)
    });
  }
}
