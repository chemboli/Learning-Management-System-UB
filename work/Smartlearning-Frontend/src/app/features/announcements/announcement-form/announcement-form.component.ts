import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AnnouncementService } from '../../../core/services/announcement.service';
import { CourseService } from '../../../core/services/course.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { CourseResponse } from '../../../core/models/models';

@Component({
  selector: 'app-announcement-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './announcement-form.component.html'
})
export class AnnouncementFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);

  courses: CourseResponse[] = [];
  isEditMode = false;
  announcementId: string | null = null;
  submitted = false;
  saving = false;

  form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    body: ['', Validators.required],
    priority: ['NORMAL', Validators.required],
    courseId: [''], // empty string = sitewide
    published: [true],
    expiresAt: ['']
  });

  constructor(
    private announcementService: AnnouncementService,
    private courseService: CourseService,
    public auth: AuthService,
    private router: Router,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    // Lecturers can only post to their own courses, so don't bother loading
    // every course for them — they'll pick from the list scoped server-side anyway.
    this.courseService.getAllCourses().subscribe({
      next: (res) => (this.courses = res)
    });

    this.announcementId = this.route.snapshot.paramMap.get('id');

    if (this.announcementId) {
      this.isEditMode = true;
      // There's no single-announcement GET endpoint; reuse the feed to find it.
      this.announcementService.getFeed().subscribe({
        next: (list) => {
          const a = list.find((x) => x.id === this.announcementId);
          if (a) {
            this.form.patchValue({
              title: a.title,
              body: a.body,
              priority: a.priority,
              courseId: a.courseId ?? '',
              published: a.published,
              expiresAt: a.expiresAt ? a.expiresAt.substring(0, 16) : ''
            });
          }
        }
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

    if (this.isEditMode && this.announcementId) {
      this.announcementService
        .update(this.announcementId, {
          title: value.title,
          body: value.body,
          priority: value.priority as any,
          published: value.published,
          expiresAt: value.expiresAt || undefined
        })
        .subscribe({
          next: () => {
            this.saving = false;
            this.toast.success('Announcement updated.');
            this.router.navigate(['/announcements']);
          },
          error: () => (this.saving = false)
        });
    } else {
      this.announcementService
        .create({
          title: value.title,
          body: value.body,
          priority: value.priority as any,
          courseId: value.courseId || undefined,
          published: value.published,
          expiresAt: value.expiresAt || undefined
        })
        .subscribe({
          next: () => {
            this.saving = false;
            this.toast.success('Announcement posted.');
            this.router.navigate(['/announcements']);
          },
          error: () => (this.saving = false)
        });
    }
  }
}
