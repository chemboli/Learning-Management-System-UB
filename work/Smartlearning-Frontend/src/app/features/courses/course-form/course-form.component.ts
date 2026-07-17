import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CourseService } from '../../../core/services/course.service';
import { UserService } from '../../../core/services/user.service';
import { ToastService } from '../../../core/services/toast.service';
import { UserResponse } from '../../../core/models/models';

@Component({
  selector: 'app-course-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './course-form.component.html'
})
export class CourseFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  isEdit = false;
  loading = false;
  saving = false;
  submitted = false;
  lecturers: UserResponse[] = [];

  private courseId: string | null = null;

  form = this.fb.nonNullable.group({
    courseCode: ['', Validators.required],
    title: ['', Validators.required],
    description: [''],
    creditHours: [3, [Validators.required, Validators.min(1)]],
    lecturerId: ['', Validators.required],
    active: [true]
  });

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private courseService: CourseService,
    private userService: UserService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.courseId = this.route.snapshot.paramMap.get('id');
    this.isEdit = !!this.courseId;

    this.userService.getLecturers().subscribe({
      next: (res) => (this.lecturers = res)
    });

    if (this.isEdit) {
      this.loading = true;
      this.courseService.getCourse(this.courseId!).subscribe({
        next: (course) => {
          this.form.patchValue({
            courseCode: course.courseCode,
            title: course.title,
            description: course.description,
            creditHours: course.creditHours,
            lecturerId: course.lecturerId,
            active: course.active
          });
          this.form.controls.courseCode.disable();
          this.loading = false;
        },
        error: () => (this.loading = false)
      });
    }
  }

  submit() {
    this.submitted = true;

    if (this.form.invalid) {
      return;
    }

    this.saving = true;
    const value = this.form.getRawValue();

    if (this.isEdit) {
      this.courseService
        .updateCourse(this.courseId!, {
          title: value.title,
          description: value.description,
          creditHours: value.creditHours,
          lecturerId: value.lecturerId,
          active: value.active
        })
        .subscribe({
          next: (res) => {
            this.saving = false;
            this.toast.success('Course updated.');
            this.router.navigate(['/courses', res.id]);
          },
          error: () => (this.saving = false)
        });
    } else {
      this.courseService
        .createCourse({
          courseCode: value.courseCode,
          title: value.title,
          description: value.description,
          creditHours: value.creditHours,
          lecturerId: value.lecturerId
        })
        .subscribe({
          next: (res) => {
            this.saving = false;
            this.toast.success('Course created.');
            this.router.navigate(['/courses', res.id]);
          },
          error: () => (this.saving = false)
        });
    }
  }
}
