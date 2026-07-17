import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CourseService } from '../../../core/services/course.service';
import { NoteService } from '../../../core/services/note.service';
import { EnrollmentService } from '../../../core/services/enrollment.service';
import { AssignmentService } from '../../../core/services/assignment.service';
import { AnnouncementService } from '../../../core/services/announcement.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmService } from '../../../shared/components/confirm-dialog/confirm.service';
import { FilePreviewService } from '../../../core/services/file-preview.service';
import { ScoreChartComponent, ChartBar } from '../../../shared/components/score-chart/score-chart.component';
import {
  AnnouncementResponse,
  AssignmentResponse,
  CourseResponse,
  NoteResponse,
  NoteType
} from '../../../core/models/models';

const NOTE_TYPE_LABELS: Record<NoteType, string> = {
  LECTURE_NOTE: 'Lecture note',
  LAB: 'Lab',
  ASSIGNMENT: 'Assignment',
  TUTORIAL: 'Tutorial',
  PAST_EXAM: 'Past exam'
};

@Component({
  selector: 'app-course-detail',
  standalone: true,
  imports: [RouterLink, DatePipe, ScoreChartComponent],
  templateUrl: './course-detail.component.html'
})
export class CourseDetailComponent implements OnInit {
  course: CourseResponse | null = null;
  notes: NoteResponse[] = [];
  assignments: AssignmentResponse[] = [];
  announcements: AnnouncementResponse[] = [];
  loading = false;
  notesLoading = false;
  assignmentsLoading = false;
  announcementsLoading = false;
  enrollLoading = false;
  enrolled = false;

  filePreview = inject(FilePreviewService);

  private courseId!: string;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private courseService: CourseService,
    private noteService: NoteService,
    private enrollmentService: EnrollmentService,
    private assignmentService: AssignmentService,
    private announcementService: AnnouncementService,
    public auth: AuthService,
    private toast: ToastService,
    private confirmService: ConfirmService
  ) {}

  ngOnInit(): void {
    this.courseId = this.route.snapshot.paramMap.get('id')!;
    this.loadCourse();
    this.loadNotes();
    this.loadAssignments();
    this.loadAnnouncements();

    if (this.auth.hasAnyRole('STUDENT')) {
      this.enrollmentService.checkEnrollment(this.courseId).subscribe({
        next: (res) => (this.enrolled = res),
        error: () => {}
      });
    }
  }

  loadAnnouncements() {
    this.announcementsLoading = true;
    this.announcementService.getForCourse(this.courseId).subscribe({
      next: (res) => {
        this.announcements = res;
        this.announcementsLoading = false;
      },
      error: () => (this.announcementsLoading = false)
    });
  }

  previewNote(note: NoteResponse) {
    this.filePreview.open({
      fileName: note.fileName || note.title,
      url: note.downloadUrl,
      contentType: note.contentType
    });
  }

  /** Whether there's at least one assignment with a graded submission, worth charting. */
  hasChartableAssignments(): boolean {
    return this.assignments.some((a) => a.averageScore !== undefined && (a.submissionCount ?? 0) > 0);
  }

  courseScoreChartBars(): ChartBar[] {
    return this.assignments
      .filter((a) => (a.submissionCount ?? 0) > 0)
      .map((a) => ({
        label: a.title,
        value: a.averageScore ?? 0,
        maxValue: a.maxScore
      }));
  }

  loadAssignments() {
    this.assignmentsLoading = true;
    this.assignmentService.getCourseAssignments(this.courseId).subscribe({
      next: (res) => {
        this.assignments = res;
        this.assignmentsLoading = false;
      },
      error: () => (this.assignmentsLoading = false)
    });
  }

  loadCourse() {
    this.loading = true;
    this.courseService.getCourse(this.courseId).subscribe({
      next: (res) => {
        this.course = res;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  loadNotes() {
    this.notesLoading = true;
    this.noteService.getCourseNotes(this.courseId).subscribe({
      next: (res) => {
        this.notes = res;
        this.notesLoading = false;
      },
      error: () => (this.notesLoading = false)
    });
  }

  noteTypeLabel(type: NoteType): string {
    return NOTE_TYPE_LABELS[type] ?? type;
  }

  enroll() {
    this.enrollLoading = true;
    this.enrollmentService.enroll({ courseId: this.courseId }).subscribe({
      next: () => {
        this.enrolled = true;
        this.enrollLoading = false;
        this.toast.success(`Enrolled in ${this.course?.courseCode}.`);
      },
      error: () => (this.enrollLoading = false)
    });
  }

  async unenroll() {
    const ok = await this.confirmService.ask(
      `Unenroll from ${this.course?.title}? You'll lose access to course-restricted materials.`,
      'Unenroll from course',
      'Unenroll'
    );

    if (!ok) return;

    this.enrollLoading = true;
    this.enrollmentService.unenroll(this.courseId).subscribe({
      next: () => {
        this.enrolled = false;
        this.enrollLoading = false;
        this.toast.success(`Unenrolled from ${this.course?.courseCode}.`);
      },
      error: () => (this.enrollLoading = false)
    });
  }

  async deleteCourse() {
    const ok = await this.confirmService.ask(
      `Delete "${this.course?.title}"? This cannot be undone.`,
      'Delete course',
      'Delete'
    );

    if (!ok) return;

    this.courseService.deleteCourse(this.courseId).subscribe({
      next: () => {
        this.toast.success('Course deleted.');
        this.router.navigate(['/courses']);
      }
    });
  }

  async deleteNote(noteId: string) {
    const ok = await this.confirmService.ask('Delete this note? This cannot be undone.', 'Delete note', 'Delete');

    if (!ok) return;

    this.noteService.deleteNote(noteId).subscribe({
      next: () => {
        this.notes = this.notes.filter((n) => n.id !== noteId);
        this.toast.success('Note deleted.');
      }
    });
  }
}
