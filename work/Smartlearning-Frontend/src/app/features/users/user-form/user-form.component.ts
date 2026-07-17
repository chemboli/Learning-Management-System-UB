import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UserService } from '../../../core/services/user.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './user-form.component.html'
})
export class UserFormComponent {
  private fb = inject(FormBuilder);
  submitted = false;
  saving = false;

  form = this.fb.nonNullable.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    matricule: [''],
    role: ['STUDENT', Validators.required]
  });

  constructor(private userService: UserService, private router: Router, private toast: ToastService) {}

  submit() {
    this.submitted = true;

    if (this.form.invalid) {
      return;
    }

    this.saving = true;
    const value = this.form.getRawValue();

    this.userService
      .createUserByAdmin({
        firstName: value.firstName,
        lastName: value.lastName,
        email: value.email,
        matricule: value.matricule || undefined,
        role: value.role as any
      })
      .subscribe({
        next: (user) => {
          this.saving = false;
          this.toast.success(`Invitation sent to ${user.email}.`);
          this.router.navigate(['/users']);
        },
        error: () => (this.saving = false)
      });
  }
}
