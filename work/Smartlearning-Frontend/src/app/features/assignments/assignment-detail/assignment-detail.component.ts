import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AssignmentService } from '../../../core/services/assignment.service';
import { SubmissionService } from '../../../core/services/submission.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmService } from '../../../shared/components/confirm-dialog/confirm.service';
import { FilePreviewService } from '../../../core/services/file-preview.service';
import { ScoreChartComponent, ChartBar } from '../../../shared/components/score-chart/score-chart.component';
import { CodeEditorComponent } from '../../../shared/components/code-editor/code-editor.component';
import { AssignmentResponse, SubmissionResponse } from '../../../core/models/models';

@Component({
  selector: 'app-assignment-detail',
  standalone: true,
  imports: [RouterLink, DatePipe, ReactiveFormsModule, CommonModule, ScoreChartComponent, CodeEditorComponent],
  templateUrl: './assignment-detail.component.html'
})
export class AssignmentDetailComponent implements OnInit {
  private fb = inject(FormBuilder);

  assignment: AssignmentResponse | null = null;
  mySubmission: SubmissionResponse | null = null;
  submissions: SubmissionResponse[] = [];

  loading = false;
  mySubmissionLoading = false;
  submissionsLoading = false;
  uploading = false;
  submitted = false;
  selectedFile: File | null = null;
  unsubmitting = false;

  // Tracks in-flight "run" requests so the button can show a spinner per submission.
  runningSubmissionIds = new Set<string>();

  // Tracks in-flight "analyze for AI" requests, same pattern as run.
  analyzingSubmissionIds = new Set<string>();

  // Tracks which submissions currently have their source code panel expanded (lecturer view).
  codeOpenSubmissionIds = new Set<string>();

  // Tracks which submission's grading panel is expanded, and the in-progress form values.
  gradingSubmissionId: string | null = null;
  gradeForm = this.fb.nonNullable.group({
    score: [0],
    feedback: ['']
  });
  savingGrade = false;

  form = this.fb.nonNullable.group({
    comment: ['']
  });

  private assignmentId!: string;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private assignmentService: AssignmentService,
    private submissionService: SubmissionService,
    public auth: AuthService,
    private toast: ToastService,
    private confirmService: ConfirmService,
    public filePreview: FilePreviewService
  ) {}

  ngOnInit(): void {
    this.assignmentId = this.route.snapshot.paramMap.get('id')!;
    this.loadAssignment();

    if (this.auth.hasAnyRole('STUDENT')) {
      this.loadMySubmission();
    }

    if (this.auth.hasAnyRole('LECTURER', 'ADMIN', 'MASTER')) {
      this.loadSubmissions();
    }
  }

  loadAssignment() {
    this.loading = true;
    this.assignmentService.getAssignment(this.assignmentId).subscribe({
      next: (res) => {
        this.assignment = res;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  loadMySubmission() {
    this.mySubmissionLoading = true;
    this.submissionService.getMySubmissions(this.assignmentId).subscribe({
      next: (res) => {
        // A student has at most one submission now — the backend still returns
        // a list for backward compatibility, so just take the first entry.
        this.mySubmission = res.length > 0 ? res[0] : null;
        this.mySubmissionLoading = false;
      },
      error: () => (this.mySubmissionLoading = false)
    });
  }

  loadSubmissions() {
    this.submissionsLoading = true;
    this.submissionService.getAssignmentSubmissions(this.assignmentId).subscribe({
      next: (res) => {
        this.submissions = res;
        this.submissionsLoading = false;
      },
      error: () => (this.submissionsLoading = false)
    });
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
  }

  // ---- In-browser code editor (alternative to file upload, programming assignments only) ----

  submissionMode: 'upload' | 'editor' = 'upload';
  editorCode = '';

  onEditorChange(code: string) {
    this.editorCode = code;
  }

  private fileExtensionFor(language: string | undefined): string {
    switch (language) {
      case 'PYTHON': return 'py';
      case 'CPP': return 'cpp';
      case 'C': return 'c';
      case 'JAVA':
      default: return 'java';
    }
  }

  /** Builds a real File object from the editor's text, so it submits through the exact same path as a file upload. */
  private buildFileFromEditor(): File {
    const ext = this.fileExtensionFor(this.assignment?.programmingLanguage);
    const filename = ext === 'java' ? 'Main.java' : `main.${ext}`;
    const blob = new Blob([this.editorCode], { type: 'text/plain' });
    return new File([blob], filename, { type: 'text/plain' });
  }

  submitWork() {
    this.submitted = true;

    const usingEditor = this.submissionMode === 'editor';
    const fileToSubmit = usingEditor
      ? (this.editorCode.trim() ? this.buildFileFromEditor() : null)
      : this.selectedFile;

    if (!fileToSubmit || this.mySubmission) {
      return;
    }

    this.uploading = true;
    const value = this.form.getRawValue();

    this.submissionService
      .submit(fileToSubmit, {
        assignmentId: this.assignmentId,
        comment: value.comment || undefined
      })
      .subscribe({
        next: () => {
          this.uploading = false;
          this.submitted = false;
          this.selectedFile = null;
          this.editorCode = '';
          this.form.reset();
          this.toast.success('Submission uploaded.');
          this.loadMySubmission();
          this.loadAssignment();
        },
        error: () => (this.uploading = false)
      });
  }

  async unsubmit() {
    if (!this.mySubmission) return;

    const ok = await this.confirmService.ask(
      'Withdraw your submission? You will be able to submit again afterwards, but the current file will be removed.',
      'Unsubmit',
      'Unsubmit'
    );

    if (!ok) return;

    this.unsubmitting = true;

    this.submissionService.unsubmit(this.assignmentId).subscribe({
      next: () => {
        this.unsubmitting = false;
        this.mySubmission = null;
        this.toast.success('Submission withdrawn. You can submit again.');
        this.loadAssignment();
      },
      error: () => (this.unsubmitting = false)
    });
  }

  markReviewed(s: SubmissionResponse) {
    this.submissionService.markReviewed(s.id).subscribe({
      next: (updated) => {
        this.submissions = this.submissions.map((x) => (x.id === updated.id ? updated : x));
        this.toast.success(`Marked ${s.studentName}'s submission as reviewed.`);
      }
    });
  }

  // ---- Preview --------------------------------------------------------

  preview(s: SubmissionResponse) {
    this.filePreview.open({
      fileName: s.fileName,
      url: s.downloadUrl,
      contentType: s.contentType
    });
  }

  // ---- Code view (lecturer only) ----------------------------------------

  toggleCode(s: SubmissionResponse) {
    if (this.codeOpenSubmissionIds.has(s.id)) {
      this.codeOpenSubmissionIds.delete(s.id);
    } else {
      this.codeOpenSubmissionIds.add(s.id);
    }
  }

  isCodeOpen(s: SubmissionResponse): boolean {
    return this.codeOpenSubmissionIds.has(s.id);
  }

  // ---- Automated run (lecturer only — students cannot trigger this) -----

  runSubmission(s: SubmissionResponse) {
    this.runningSubmissionIds.add(s.id);

    this.submissionService.runSubmission(s.id).subscribe({
      next: (updated) => {
        this.runningSubmissionIds.delete(s.id);
        this.applyUpdatedSubmission(updated);
        const passed = updated.lastRunResult?.filter((r) => r.passed).length ?? 0;
        const total = updated.lastRunResult?.length ?? 0;
        this.toast.success(`Run complete: ${passed}/${total} test cases passed.`);
      },
      error: () => this.runningSubmissionIds.delete(s.id)
    });
  }

  isRunning(s: SubmissionResponse): boolean {
    return this.runningSubmissionIds.has(s.id);
  }

  runningAll = false;

  /** Runs every submission for this assignment in one batch, then reloads the list to show results. */
  runAll() {
    this.runningAll = true;

    this.submissionService.runAllForAssignment(this.assignmentId).subscribe({
      next: (result) => {
        this.runningAll = false;
        let message = `Ran ${result.ranCount} of ${result.totalSubmissions} submissions.`;
        if (result.skippedCount > 0) {
          message += ` ${result.skippedCount} skipped (no readable code).`;
        }
        if (result.failedCount > 0) {
          message += ` ${result.failedCount} failed to run.`;
        }
        this.toast.success(message);
        this.loadSubmissions();
      },
      error: () => (this.runningAll = false)
    });
  }

  // ---- AI-likelihood analysis (lecturer triage signal only) -------------

  analyzeForAi(s: SubmissionResponse) {
    this.analyzingSubmissionIds.add(s.id);

    this.submissionService.analyzeForAi(s.id).subscribe({
      next: (updated) => {
        this.analyzingSubmissionIds.delete(s.id);
        this.applyUpdatedSubmission(updated);
        this.toast.success(`AI-likelihood estimate: ${updated.aiLikelihoodPercent}%.`);
      },
      error: () => this.analyzingSubmissionIds.delete(s.id)
    });
  }

  isAnalyzing(s: SubmissionResponse): boolean {
    return this.analyzingSubmissionIds.has(s.id);
  }

  aiLikelihoodClass(percent: number | undefined): string {
    if (percent === undefined || percent === null) return 'secondary';
    if (percent >= 70) return 'danger';
    if (percent >= 40) return 'warning';
    return 'success';
  }

  /** One bar per graded submission, showing score against the assignment's max. */
  scoreChartBars(): ChartBar[] {
    return this.submissions
      .filter((s) => s.score !== null && s.score !== undefined)
      .map((s) => ({
        label: s.studentName,
        value: s.score as number,
        maxValue: s.maxScore
      }));
  }

  // ---- Manual grading ---------------------------------------------------

  startGrading(s: SubmissionResponse) {
    this.gradingSubmissionId = s.id;
    this.gradeForm.setValue({
      score: s.score ?? 0,
      feedback: s.feedback ?? ''
    });
  }

  cancelGrading() {
    this.gradingSubmissionId = null;
  }

  saveGrade(s: SubmissionResponse) {
    const value = this.gradeForm.getRawValue();
    const max = this.assignment?.maxScore ?? 100;

    if (value.score < 0 || value.score > max) {
      this.toast.error(`Score must be between 0 and ${max}.`);
      return;
    }

    this.savingGrade = true;

    this.submissionService
      .gradeSubmission(s.id, { score: value.score, feedback: value.feedback })
      .subscribe({
        next: (updated) => {
          this.savingGrade = false;
          this.gradingSubmissionId = null;
          this.applyUpdatedSubmission(updated);
          this.toast.success(`Grade saved for ${s.studentName}.`);
        },
        error: () => (this.savingGrade = false)
      });
  }

  /** Patches one submission wherever it's currently held. */
  private applyUpdatedSubmission(updated: SubmissionResponse) {
    if (this.mySubmission?.id === updated.id) {
      this.mySubmission = updated;
    }
    this.submissions = this.submissions.map((x) => (x.id === updated.id ? updated : x));
  }

  async deleteAssignment() {
    const ok = await this.confirmService.ask(
      `Delete "${this.assignment?.title}"? Student submissions will remain in storage but the assignment will no longer be visible.`,
      'Delete assignment',
      'Delete'
    );

    if (!ok) return;

    this.assignmentService.deleteAssignment(this.assignmentId).subscribe({
      next: () => {
        this.toast.success('Assignment deleted.');
        this.router.navigate(['/courses', this.assignment?.courseId]);
      }
    });
  }

  // ---- CSV export ---------------------------------------------------------

  exportingCsv = false;

  downloadScoresCsv() {
    this.exportingCsv = true;

    this.submissionService.exportScoresCsv(this.assignmentId).subscribe({
      next: (blob) => {
        this.exportingCsv = false;
        const safeTitle = (this.assignment?.title ?? 'assignment').replace(/[^a-z0-9]+/gi, '-').toLowerCase();
        this.triggerDownload(blob, `scores-${safeTitle}.csv`);
      },
      error: () => {
        this.exportingCsv = false;
        this.toast.error('Could not generate the CSV export.');
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
