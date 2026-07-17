import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AdminCreateUserRequest,
  ChangePasswordRequest,
  UpdateUserRequest,
  UserResponse
} from '../models/models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly apiUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  getMe(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/me`);
  }

  getAllUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(this.apiUrl);
  }

  getUser(id: string): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/${id}`);
  }

  getStudents(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.apiUrl}/students`);
  }

  getLecturers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.apiUrl}/lecturers`);
  }

  createUserByAdmin(request: AdminCreateUserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.apiUrl}/admin/create`, request);
  }

  /** Re-sends the invitation email for a still-pending account. */
  resendInvitation(id: string): Observable<string> {
    return this.http.post(`${this.apiUrl}/${id}/resend-invitation`, null, {
      responseType: 'text'
    });
  }

  updateUser(email: string, request: UpdateUserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.apiUrl}/${email}`, request);
  }

  changePassword(email: string, request: ChangePasswordRequest): Observable<string> {
    return this.http.put(`${this.apiUrl}/change-password/${email}`, request, {
      responseType: 'text'
    });
  }

  deleteUser(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /** Downloads a CSV of users, optionally filtered to a single role. Pass null/undefined for everyone. */
  exportUsersCsv(role?: string | null): Observable<Blob> {
    const url = role ? `${this.apiUrl}/export?role=${role}` : `${this.apiUrl}/export`;
    return this.http.get(url, { responseType: 'blob' });
  }
}
