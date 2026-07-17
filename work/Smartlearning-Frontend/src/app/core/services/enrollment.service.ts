import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EnrollRequest, EnrollmentResponse } from '../models/models';

@Injectable({ providedIn: 'root' })
export class EnrollmentService {
  private readonly apiUrl = `${environment.apiUrl}/enrollments`;

  constructor(private http: HttpClient) {}

  enroll(request: EnrollRequest): Observable<EnrollmentResponse> {
    return this.http.post<EnrollmentResponse>(`${this.apiUrl}/enroll`, request);
  }

  unenroll(courseId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/unenroll/${courseId}`);
  }

  checkEnrollment(courseId: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/check/${courseId}`);
  }

  getMyCourses(): Observable<EnrollmentResponse[]> {
    return this.http.get<EnrollmentResponse[]>(`${this.apiUrl}/my-courses`);
  }

  getEnrollmentsByStudent(studentId: string): Observable<EnrollmentResponse[]> {
    return this.http.get<EnrollmentResponse[]>(`${this.apiUrl}/student/${studentId}`);
  }

  getEnrollmentsByCourse(courseId: string): Observable<EnrollmentResponse[]> {
    return this.http.get<EnrollmentResponse[]>(`${this.apiUrl}/course/${courseId}`);
  }

  removeEnrollment(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /** Downloads a CSV roster for a course - every enrolled student with their total score. */
  exportRosterCsv(courseId: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/course/${courseId}/export`, { responseType: 'blob' });
  }
}
