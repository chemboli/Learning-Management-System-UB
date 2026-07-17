import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AssignmentResponse, CreateAssignmentRequest, UpdateAssignmentRequest } from '../models/models';

@Injectable({ providedIn: 'root' })
export class AssignmentService {
  private readonly apiUrl = `${environment.apiUrl}/assignments`;

  constructor(private http: HttpClient) {}

  createAssignment(request: CreateAssignmentRequest): Observable<AssignmentResponse> {
    return this.http.post<AssignmentResponse>(this.apiUrl, request);
  }

  getCourseAssignments(courseId: string): Observable<AssignmentResponse[]> {
    return this.http.get<AssignmentResponse[]>(`${this.apiUrl}/course/${courseId}`);
  }

  getAssignment(id: string): Observable<AssignmentResponse> {
    return this.http.get<AssignmentResponse>(`${this.apiUrl}/${id}`);
  }

  updateAssignment(id: string, request: UpdateAssignmentRequest): Observable<AssignmentResponse> {
    return this.http.put<AssignmentResponse>(`${this.apiUrl}/${id}`, request);
  }

  deleteAssignment(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
