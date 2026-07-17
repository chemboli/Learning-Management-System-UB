import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  BatchRunResponse,
  CreateSubmissionRequest,
  GradeSubmissionRequest,
  SubmissionResponse
} from '../models/models';

@Injectable({ providedIn: 'root' })
export class SubmissionService {
  private readonly apiUrl = `${environment.apiUrl}/submissions`;

  constructor(private http: HttpClient) {}

  submit(file: File, request: CreateSubmissionRequest): Observable<SubmissionResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append(
      'request',
      new Blob([JSON.stringify(request)], { type: 'application/json' })
    );

    return this.http.post<SubmissionResponse>(`${this.apiUrl}/submit`, formData);
  }

  getAssignmentSubmissions(assignmentId: string): Observable<SubmissionResponse[]> {
    return this.http.get<SubmissionResponse[]>(`${this.apiUrl}/assignment/${assignmentId}`);
  }

  getMySubmissions(assignmentId: string): Observable<SubmissionResponse[]> {
    return this.http.get<SubmissionResponse[]>(`${this.apiUrl}/assignment/${assignmentId}/mine`);
  }

  /** Withdraws the current student's own submission so they can submit again. */
  unsubmit(assignmentId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/assignment/${assignmentId}/mine`);
  }

  markReviewed(id: string): Observable<SubmissionResponse> {
    return this.http.put<SubmissionResponse>(`${this.apiUrl}/${id}/review`, {});
  }

  /** Lecturer manually enters/edits a score and feedback. */
  gradeSubmission(id: string, request: GradeSubmissionRequest): Observable<SubmissionResponse> {
    return this.http.put<SubmissionResponse>(`${this.apiUrl}/${id}/grade`, request);
  }

  /** Runs the submission's code against the assignment's test cases (programming assignments). */
  runSubmission(id: string): Observable<SubmissionResponse> {
    return this.http.post<SubmissionResponse>(`${this.apiUrl}/${id}/run`, {});
  }

  /** Runs every submission for a programming assignment in one batch. */
  runAllForAssignment(assignmentId: string): Observable<BatchRunResponse> {
    return this.http.post<BatchRunResponse>(`${this.apiUrl}/assignment/${assignmentId}/run-all`, {});
  }

  /** Estimates how likely a submission's text content is AI-generated — a triage signal, not a verdict. */
  analyzeForAi(id: string): Observable<SubmissionResponse> {
    return this.http.post<SubmissionResponse>(`${this.apiUrl}/${id}/analyze-ai`, {});
  }

  /** Downloads a CSV gradebook for an assignment — one row per enrolled student. */
  exportScoresCsv(assignmentId: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/assignment/${assignmentId}/export`, { responseType: 'blob' });
  }
}
