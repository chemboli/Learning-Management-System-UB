import { Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CourseService } from '../../../core/services/course.service';
import { EnrollmentService } from '../../../core/services/enrollment.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmService } from '../../../shared/components/confirm-dialog/confirm.service';
import { CourseResponse, EnrollmentResponse } from '../../../core/models/models';

@Component({
  selector: 'app-manage-enrollments',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './manage-enrollments.component.html'
})
export class ManageEnrollmentsComponent implements OnInit {
  courses: CourseResponse[] = [];
  enrollments: EnrollmentResponse[] = [];
  selectedCourseId = '';
  loading = false;
  exportingCsv = false;

  constructor(
    private courseService: CourseService,
    private enrollmentService: EnrollmentService,
    public auth: AuthService,
    private toast: ToastService,
    private confirmService: ConfirmService
  ) {}

  ngOnInit(): void {
    // Lecturers only ever manage their own courses; admins/master can pick any course.
    const courses$ = this.auth.hasAnyRole('LECTURER')
      ? this.courseService.getMyCourses()
      : this.courseService.getAllCourses();

    courses$.subscribe({
      next: (res) => (this.courses = res)
    });
  }

  onCourseChange() {
    if (!this.selectedCourseId) return;

    this.loading = true;
    this.enrollmentService.getEnrollmentsByCourse(this.selectedCourseId).subscribe({
      next: (res) => {
        this.enrollments = res;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  async removeEnrollment(e: EnrollmentResponse) {
    const ok = await this.confirmService.ask(
      `Remove ${e.studentName} from ${e.courseTitle}?`,
      'Remove enrollment',
      'Remove'
    );

    if (!ok) return;

    this.enrollmentService.removeEnrollment(e.id).subscribe({
      next: () => {
        this.enrollments = this.enrollments.filter((x) => x.id !== e.id);
        this.toast.success('Enrollment removed.');
      }
    });
  }

  exportRoster() {
    if (!this.selectedCourseId) return;

    this.exportingCsv = true;

    this.enrollmentService.exportRosterCsv(this.selectedCourseId).subscribe({
      next: (blob) => {
        this.exportingCsv = false;
        const course = this.courses.find((c) => c.id === this.selectedCourseId);
        const safeName = (course?.courseCode ?? 'course').replace(/[^a-z0-9]+/gi, '-').toLowerCase();
        this.triggerDownload(blob, `roster-${safeName}.csv`);
      },
      error: () => {
        this.exportingCsv = false;
        this.toast.error('Could not generate the roster export.');
      }
    });
  }

  private triggerDownload(blob: Blob, filename: string) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }
}
