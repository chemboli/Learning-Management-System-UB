import { Component, OnInit, inject } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AssignmentService } from '../../../core/services/assignment.service';
import { CourseService } from '../../../core/services/course.service';
import { AiService } from '../../../core/services/ai.service';
import { ToastService } from '../../../core/services/toast.service';
import { AssignmentType, CourseResponse, ProgrammingLanguage, TestCaseDto } from '../../../core/models/models';

@Component({
  selector: 'app-assignment-form',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, RouterLink],
  templateUrl: './assignment-form.component.html'
})
export class AssignmentFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private aiService = inject(AiService);

  isEdit = false;
  loading = false;
  saving = false;
  submitted = false;
  courses: CourseResponse[] = [];

  // ---- AI generation ----
  aiAvailable = false;
  aiPrompt = '';
  generating = false;
  lastGenerationDisclaimer: string | null = null;

  private assignmentId: string | null = null;
  private courseId: string | null = null;

  form = this.fb.nonNullable.group({
    courseId: ['', Validators.required],
    title: ['', Validators.required],
    description: [''],
    dueDate: ['', Validators.required],
    published: [true],
    assignmentType: ['REGULAR' as AssignmentType, Validators.required],
    programmingLanguage: ['JAVA' as ProgrammingLanguage],
    maxScore: [100, [Validators.required, Validators.min(1)]],
    latePenaltyPercent: [0, [Validators.min(0), Validators.max(100)]],
    testCases: this.fb.array<FormGroup>([])
  });

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private assignmentService: AssignmentService,
    private courseService: CourseService,
    private toast: ToastService
  ) {}

  get isProgramming(): boolean {
    return this.form.controls.assignmentType.value === 'PROGRAMMING';
  }

  get testCases(): FormArray<FormGroup> {
    return this.form.controls.testCases;
  }

  ngOnInit(): void {
    this.aiService.status().subscribe({
      next: (res) => (this.aiAvailable = res.configured),
      error: () => (this.aiAvailable = false)
    });

    this.assignmentId = this.route.snapshot.paramMap.get('id');
    this.isEdit = !!this.assignmentId;

    if (this.isEdit) {
      this.loading = true;
      this.assignmentService.getAssignment(this.assignmentId!).subscribe({
        next: (a) => {
          this.courseId = a.courseId;
          this.form.patchValue({
            courseId: a.courseId,
            title: a.title,
            description: a.description,
            dueDate: this.toLocalInput(a.dueDate),
            published: a.published,
            assignmentType: a.assignmentType,
            programmingLanguage: a.programmingLanguage ?? 'JAVA',
            maxScore: a.maxScore,
            latePenaltyPercent: a.latePenaltyPercent ?? 0
          });
          this.form.controls.courseId.disable();

          (a.testCases ?? []).forEach((tc) => this.addTestCase(tc));

          this.loading = false;
        },
        error: () => (this.loading = false)
      });
    } else {
      this.courseService.getAllCourses().subscribe({
        next: (res) => (this.courses = res)
      });

      const courseIdParam = this.route.snapshot.queryParamMap.get('courseId');
      if (courseIdParam) {
        this.courseId = courseIdParam;
        this.form.patchValue({ courseId: courseIdParam });
      }
    }
  }

  addTestCase(existing?: TestCaseDto) {
    const group = this.fb.nonNullable.group({
      label: [existing?.label ?? ''],
      input: [existing?.input ?? ''],
      expectedOutput: [existing?.expectedOutput ?? '', Validators.required],
      weight: [existing?.weight ?? 1, [Validators.required, Validators.min(0.1)]],
      hidden: [existing?.hidden ?? false]
    });
    this.testCases.push(group);
  }

  removeTestCase(index: number) {
    this.testCases.removeAt(index);
  }

  generateWithAi() {
    if (!this.aiPrompt.trim()) {
      this.toast.error('Describe what was covered so the AI has something to work from.');
      return;
    }

    this.generating = true;
    const value = this.form.getRawValue();

    this.aiService
      .generateAssignment({
        prompt: this.aiPrompt.trim(),
        assignmentType: value.assignmentType,
        programmingLanguage: this.isProgramming ? value.programmingLanguage : undefined,
        testCaseCount: 4
      })
      .subscribe({
        next: (draft) => {
          this.generating = false;
          this.lastGenerationDisclaimer = draft.disclaimer;

          this.form.patchValue({
            title: draft.title,
            description: draft.description,
            maxScore: draft.suggestedMaxScore ?? this.form.controls.maxScore.value
          });

          if (this.isProgramming) {
            // Replace any existing test cases with the generated ones — the lecturer
            // can still edit, add, or remove before saving.
            while (this.testCases.length) {
              this.testCases.removeAt(0);
            }
            (draft.testCases ?? []).forEach((tc) => this.addTestCase(tc));
          }

          this.toast.success('Draft generated. Review everything before publishing.');
        },
        error: () => (this.generating = false)
      });
  }

  private toLocalInput(iso: string): string {
    // Convert an ISO datetime to the value <input type="datetime-local"> expects.
    const d = new Date(iso);
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  submit() {
    this.submitted = true;

    if (this.form.invalid) {
      return;
    }

    if (this.isProgramming && this.testCases.length === 0) {
      this.toast.error('Add at least one test case for a programming assignment.');
      return;
    }

    this.saving = true;
    const value = this.form.getRawValue();
    const dueDateIso = new Date(value.dueDate).toISOString();

    const testCasesPayload: TestCaseDto[] | undefined = this.isProgramming
      ? value.testCases.map((tc, i) => ({
          sequence: i + 1,
          label: tc['label'] || undefined,
          input: tc['input'],
          expectedOutput: tc['expectedOutput'],
          weight: tc['weight'],
          hidden: tc['hidden']
        }))
      : undefined;

    if (this.isEdit) {
      this.assignmentService
        .updateAssignment(this.assignmentId!, {
          title: value.title,
          description: value.description,
          dueDate: dueDateIso,
          published: value.published,
          assignmentType: value.assignmentType,
          programmingLanguage: this.isProgramming ? value.programmingLanguage : undefined,
          maxScore: value.maxScore,
          latePenaltyPercent: value.latePenaltyPercent,
          testCases: testCasesPayload
        })
        .subscribe({
          next: (res) => {
            this.saving = false;
            this.toast.success('Assignment updated.');
            this.router.navigate(['/courses', res.courseId]);
          },
          error: () => (this.saving = false)
        });
    } else {
      this.assignmentService
        .createAssignment({
          courseId: value.courseId,
          title: value.title,
          description: value.description,
          dueDate: dueDateIso,
          published: value.published,
          assignmentType: value.assignmentType,
          programmingLanguage: this.isProgramming ? value.programmingLanguage : undefined,
          maxScore: value.maxScore,
          latePenaltyPercent: value.latePenaltyPercent,
          testCases: testCasesPayload
        })
        .subscribe({
          next: (res) => {
            this.saving = false;
            this.toast.success('Assignment created.');
            this.router.navigate(['/courses', res.courseId]);
          },
          error: () => (this.saving = false)
        });
    }
  }
}
