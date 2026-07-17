import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { GenerateAssignmentRequest, GeneratedAssignmentResponse } from '../models/models';

@Injectable({ providedIn: 'root' })
export class AiService {
  private readonly apiUrl = `${environment.apiUrl}/ai`;

  constructor(private http: HttpClient) {}

  status(): Observable<{ configured: boolean }> {
    return this.http.get<{ configured: boolean }>(`${this.apiUrl}/status`);
  }

  generateAssignment(request: GenerateAssignmentRequest): Observable<GeneratedAssignmentResponse> {
    return this.http.post<GeneratedAssignmentResponse>(`${this.apiUrl}/generate-assignment`, request);
  }
}
