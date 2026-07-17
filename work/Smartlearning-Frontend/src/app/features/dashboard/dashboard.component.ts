import { Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { EnrollmentService } from '../../core/services/enrollment.service';
import { AnnouncementService } from '../../core/services/announcement.service';
import { AnnouncementResponse, EnrollmentResponse } from '../../core/models/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, DatePipe],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  myCourses: EnrollmentResponse[] = [];
  announcements: AnnouncementResponse[] = [];
  loading = false;
  announcementsLoading = false;

  constructor(
    public auth: AuthService,
    private enrollmentService: EnrollmentService,
    private announcementService: AnnouncementService
  ) {}

  ngOnInit(): void {
    if (this.auth.hasAnyRole('STUDENT')) {
      this.loading = true;
      this.enrollmentService.getMyCourses().subscribe({
        next: (res) => {
          this.myCourses = res;
          this.loading = false;
        },
        error: () => (this.loading = false)
      });
    }

    this.announcementsLoading = true;
    this.announcementService.getFeed().subscribe({
      next: (res) => {
        this.announcements = res.slice(0, 5);
        this.announcementsLoading = false;
      },
      error: () => (this.announcementsLoading = false)
    });
  }
}
