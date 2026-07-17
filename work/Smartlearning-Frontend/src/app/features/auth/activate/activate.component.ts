import { Component, OnInit, inject } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { InvitationDetails } from '../../../core/models/models';

/**
 * Public activation page for admin-invited accounts.
 *
 * Flow:
 *  1. User arrives via the emailed link (/activate?code=XXXX) or types the code.
 *  2. We verify the code and load their identity (name, email) — read-only.
 *  3. They choose a password, we redeem the code, then send them to login.
 */
@Component({
  selector: 'app-activate',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './activate.component.html'
})
export class ActivateComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);

  /** 'code' → entering the invitation code; 'password' → identity loaded, choosing password. */
  step: 'code' | 'password' = 'code';

  verifying = false;
  activating = false;
  submittedCode = false;
  submittedPassword = false;
  showPassword = false;

  invitation: InvitationDetails | null = null;

  codeForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.minLength(6)]]
  });

  passwordForm = this.fb.nonNullable.group(
    {
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    },
    { validators: [matchPasswords] }
  );

  constructor(
    private auth: AuthService,
    private router: Router,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    // If the user clicked the email link, the code arrives as ?code=XXXX —
    // pre-fill and verify it immediately so they land straight on step 2.
    const codeFromLink = this.route.snapshot.queryParamMap.get('code');
    if (codeFromLink) {
      this.codeForm.patchValue({ code: codeFromLink });
      this.verifyCode();
    }
  }

  verifyCode() {
    this.submittedCode = true;

    if (this.codeForm.invalid) {
      return;
    }

    this.verifying = true;
    const code = this.codeForm.getRawValue().code;

    this.auth.getInvitation(code).subscribe({
      next: (invitation) => {
        this.verifying = false;
        this.invitation = invitation;
        this.step = 'password';
      },
      error: () => {
        // The error interceptor already shows the backend message as a toast.
        this.verifying = false;
        this.step = 'code';
      }
    });
  }

  changeCode() {
    this.step = 'code';
    this.invitation = null;
    this.submittedPassword = false;
    this.passwordForm.reset();
  }

  activate() {
    this.submittedPassword = true;

    if (this.passwordForm.invalid) {
      return;
    }

    this.activating = true;

    this.auth
      .activateAccount({
        invitationCode: this.codeForm.getRawValue().code.trim(),
        password: this.passwordForm.getRawValue().password
      })
      .subscribe({
        next: () => {
          this.activating = false;
          this.toast.success('Account activated! You can now sign in.');
          this.router.navigate(['/login']);
        },
        error: () => (this.activating = false)
      });
  }
}

/** Cross-field validator: password and confirmPassword must match. */
function matchPasswords(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { passwordMismatch: true } : null;
}
