import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ActivateAccountRequest,
  AuthResponse,
  InvitationDetails,
  LoginRequest,
  RegisterRequest,
  Role
} from '../models/models';

const TOKEN_KEY = 'sl_token';
const EMAIL_KEY = 'sl_email';
const ROLE_KEY = 'sl_role';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  private tokenSig = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  private emailSig = signal<string | null>(localStorage.getItem(EMAIL_KEY));
  private roleSig = signal<Role | null>(localStorage.getItem(ROLE_KEY) as Role | null);

  email = computed(() => this.emailSig());
  role = computed(() => this.roleSig());
  isLoggedIn = computed(() => !!this.tokenSig() && this.isTokenStructurallyValid());

  constructor(private http: HttpClient) {}

  private isTokenStructurallyValid(): boolean {
    const token = this.tokenSig();
    if (!token) return false;

    try {
      const payload = token.split('.')[1];
      const json = decodeURIComponent(
        atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      const decoded = JSON.parse(json);

      if (decoded.exp && Date.now() >= decoded.exp * 1000) {
        return false;
      }

      return true;
    } catch {
      return false;
    }
  }

  getToken(): string | null {
    return this.tokenSig();
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap((res) => this.setSession(res))
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, request).pipe(
      tap((res) => this.setSession(res))
    );
  }

  /** Public: loads the read-only identity behind an invitation code. */
  getInvitation(code: string): Observable<InvitationDetails> {
    return this.http.get<InvitationDetails>(
      `${this.apiUrl}/invitation/${encodeURIComponent(code.trim())}`
    );
  }

  /** Public: redeems an invitation code with the user's chosen password. */
  activateAccount(request: ActivateAccountRequest): Observable<string> {
    return this.http.post(`${this.apiUrl}/activate`, request, { responseType: 'text' });
  }

  private setSession(res: AuthResponse) {
    if (res?.token) {
      localStorage.setItem(TOKEN_KEY, res.token);
      this.tokenSig.set(res.token);
    }

    if (res?.email) {
      localStorage.setItem(EMAIL_KEY, res.email);
      this.emailSig.set(res.email);
    }

    if (res?.role) {
      const normalized = res.role.replace(/^ROLE_/, '') as Role;
      localStorage.setItem(ROLE_KEY, normalized);
      this.roleSig.set(normalized);
    }
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(EMAIL_KEY);
    localStorage.removeItem(ROLE_KEY);
    this.tokenSig.set(null);
    this.emailSig.set(null);
    this.roleSig.set(null);
  }

  /** Call after fetching /users/me, in case the role changed server-side. */
  refreshIdentity(email: string, role: Role) {
    const normalized = role.replace(/^ROLE_/, '') as Role;
    localStorage.setItem(EMAIL_KEY, email);
    localStorage.setItem(ROLE_KEY, normalized);
    this.emailSig.set(email);
    this.roleSig.set(normalized);
  }

  hasAnyRole(...roles: Role[]): boolean {
    const current = this.roleSig();
    return !!current && roles.includes(current);
  }
}
