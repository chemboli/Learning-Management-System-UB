import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './profile.component.html'
})
export class ProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  loading = false;
  savingProfile = false;
  savingPassword = false;
  passwordSubmitted = false;
  email = '';

  profileForm = this.fb.nonNullable.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    matricule: ['']
  });

  passwordForm = this.fb.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(6)]]
  });

  constructor(
    private userService: UserService,
    public auth: AuthService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.userService.getMe().subscribe({
      next: (user) => {
        this.email = user.email;
        this.profileForm.patchValue({
          firstName: user.firstName,
          lastName: user.lastName,
          matricule: user.matricule ?? ''
        });
        this.auth.refreshIdentity(user.email, user.role);
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  saveProfile() {
    if (this.profileForm.invalid) {
      return;
    }

    this.savingProfile = true;
    const value = this.profileForm.getRawValue();

    this.userService
      .updateUser(this.email, {
        firstName: value.firstName,
        lastName: value.lastName,
        matricule: value.matricule || undefined
      })
      .subscribe({
        next: () => {
          this.savingProfile = false;
          this.toast.success('Profile updated.');
        },
        error: () => (this.savingProfile = false)
      });
  }

  changePassword() {
    this.passwordSubmitted = true;

    if (this.passwordForm.invalid) {
      return;
    }

    this.savingPassword = true;
    const value = this.passwordForm.getRawValue();

    this.userService.changePassword(this.email, value).subscribe({
      next: () => {
        this.savingPassword = false;
        this.passwordSubmitted = false;
        this.passwordForm.reset();
        this.toast.success('Password updated.');
      },
      error: () => (this.savingPassword = false)
    });
  }
}
