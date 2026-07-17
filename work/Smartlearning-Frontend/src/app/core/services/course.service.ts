import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CourseResponse, CreateCourseRequest, UpdateCourseRequest } from '../models/models';

@Injectable({ providedIn: 'root' })
export class CourseService {
  private readonly apiUrl = `${environment.apiUrl}/courses`;

  constructor(private http: HttpClient) {}

  getAllCourses(): Observable<CourseResponse[]> {
    return this.http.get<CourseResponse[]>(this.apiUrl);
  }

  getCourse(id: string): Observable<CourseResponse> {
    return this.http.get<CourseResponse>(`${this.apiUrl}/${id}`);
  }

  getCourseByCode(courseCode: string): Observable<CourseResponse> {
    return this.http.get<CourseResponse>(`${this.apiUrl}/code/${courseCode}`);
  }

  getCoursesByLecturer(lecturerId: string): Observable<CourseResponse[]> {
    return this.http.get<CourseResponse[]>(`${this.apiUrl}/lecturer/${lecturerId}`);
  }

  /** Courses taught by the currently authenticated lecturer. */
  getMyCourses(): Observable<CourseResponse[]> {
    return this.http.get<CourseResponse[]>(`${this.apiUrl}/mine`);
  }

  createCourse(request: CreateCourseRequest): Observable<CourseResponse> {
    return this.http.post<CourseResponse>(this.apiUrl, request);
  }

  updateCourse(id: string, request: UpdateCourseRequest): Observable<CourseResponse> {
    return this.http.put<CourseResponse>(`${this.apiUrl}/${id}`, request);
  }

  deleteCourse(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
