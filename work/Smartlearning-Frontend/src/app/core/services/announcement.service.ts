import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AnnouncementResponse,
  CreateAnnouncementRequest,
  UpdateAnnouncementRequest
} from '../models/models';

@Injectable({ providedIn: 'root' })
export class AnnouncementService {
  private readonly apiUrl = `${environment.apiUrl}/announcements`;

  constructor(private http: HttpClient) {}

  create(request: CreateAnnouncementRequest): Observable<AnnouncementResponse> {
    return this.http.post<AnnouncementResponse>(this.apiUrl, request);
  }

  getSitewide(): Observable<AnnouncementResponse[]> {
    return this.http.get<AnnouncementResponse[]>(`${this.apiUrl}/sitewide`);
  }

  getForCourse(courseId: string): Observable<AnnouncementResponse[]> {
    return this.http.get<AnnouncementResponse[]>(`${this.apiUrl}/course/${courseId}`);
  }

  /** Personalized feed: sitewide + the user's own course announcements. */
  getFeed(): Observable<AnnouncementResponse[]> {
    return this.http.get<AnnouncementResponse[]>(`${this.apiUrl}/feed`);
  }

  update(id: string, request: UpdateAnnouncementRequest): Observable<AnnouncementResponse> {
    return this.http.put<AnnouncementResponse>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
