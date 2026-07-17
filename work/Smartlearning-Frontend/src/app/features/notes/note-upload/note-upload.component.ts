import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CourseService } from '../../../core/services/course.service';
import { NoteService } from '../../../core/services/note.service';
import { ToastService } from '../../../core/services/toast.service';
import { CourseResponse } from '../../../core/models/models';

@Component({
  selector: 'app-note-upload',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './note-upload.component.html'
})
export class NoteUploadComponent implements OnInit {
  private fb = inject(FormBuilder);
  courses: CourseResponse[] = [];
  selectedFile: File | null = null;
  submitted = false;
  uploading = false;

  form = this.fb.nonNullable.group({
    courseId: ['', Validators.required],
    title: ['', Validators.required],
    description: [''],
    noteType: ['LECTURE_NOTE', Validators.required],
    weekNumber: [null as number | null],
    published: [true]
  });

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private courseService: CourseService,
    private noteService: NoteService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.courseService.getAllCourses().subscribe({
      next: (res) => (this.courses = res)
    });

    const courseId = this.route.snapshot.queryParamMap.get('courseId');
    if (courseId) {
      this.form.patchValue({ courseId });
    }
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
  }

  submit() {
    this.submitted = true;

    if (this.form.invalid || !this.selectedFile) {
      return;
    }

    this.uploading = true;
    const value = this.form.getRawValue();

    this.noteService
      .upload(this.selectedFile, {
        courseId: value.courseId,
        title: value.title,
        description: value.description,
        noteType: value.noteType as any,
        weekNumber: value.weekNumber ?? undefined,
        published: value.published
      })
      .subscribe({
        next: (res) => {
          this.uploading = false;
          this.toast.success('Note uploaded successfully.');
          this.router.navigate(['/courses', value.courseId]);
        },
        error: () => (this.uploading = false)
      });
  }
}
