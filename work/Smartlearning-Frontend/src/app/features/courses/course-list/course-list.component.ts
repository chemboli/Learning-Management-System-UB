import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CourseService } from '../../../core/services/course.service';
import { AuthService } from '../../../core/services/auth.service';
import { CourseResponse } from '../../../core/models/models';

@Component({
  selector: 'app-course-list',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './course-list.component.html'
})
export class CourseListComponent implements OnInit {
  courses = signal<CourseResponse[]>([]);
  loading = false;
  searchTerm = '';

  constructor(private courseService: CourseService, public auth: AuthService) {}

  ngOnInit(): void {
    this.loading = true;
    this.courseService.getAllCourses().subscribe({
      next: (res) => {
        this.courses.set(res);
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  filteredCourses() {
    const term = this.searchTerm.trim().toLowerCase();

    if (!term) {
      return this.courses();
    }

    return this.courses().filter(
      (c) =>
        c.title.toLowerCase().includes(term) ||
        c.courseCode.toLowerCase().includes(term) ||
        c.lecturerName?.toLowerCase().includes(term)
    );
  }
}
