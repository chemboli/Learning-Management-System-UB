import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateNoteRequest, NoteResponse, UpdateNoteRequest } from '../models/models';

@Injectable({ providedIn: 'root' })
export class NoteService {
  private readonly apiUrl = `${environment.apiUrl}/notes`;

  constructor(private http: HttpClient) {}

  upload(file: File, request: CreateNoteRequest): Observable<NoteResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append(
      'request',
      new Blob([JSON.stringify(request)], { type: 'application/json' })
    );

    return this.http.post<NoteResponse>(`${this.apiUrl}/upload`, formData);
  }

  getCourseNotes(courseId: string): Observable<NoteResponse[]> {
    return this.http.get<NoteResponse[]>(`${this.apiUrl}/course/${courseId}`);
  }

  getByCourseCode(courseCode: string): Observable<NoteResponse[]> {
    return this.http.get<NoteResponse[]>(`${this.apiUrl}/course/code/${courseCode}`);
  }

  getNote(id: string): Observable<NoteResponse> {
    return this.http.get<NoteResponse>(`${this.apiUrl}/${id}`);
  }

  updateNote(id: string, request: UpdateNoteRequest): Observable<NoteResponse> {
    return this.http.put<NoteResponse>(`${this.apiUrl}/${id}`, request);
  }

  deleteNote(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
