import { Component, OnInit, inject } from '@angular/core';
import { Location } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NoteService } from '../../../core/services/note.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-note-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './note-form.component.html'
})
export class NoteFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  loading = false;
  saving = false;
  submitted = false;
  private noteId!: string;

  form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    description: [''],
    noteType: ['LECTURE_NOTE'],
    weekNumber: [null as number | null],
    published: [true]
  });

  constructor(
    private route: ActivatedRoute,
    private location: Location,
    private noteService: NoteService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.noteId = this.route.snapshot.paramMap.get('id')!;
    this.loading = true;

    this.noteService.getNote(this.noteId).subscribe({
      next: (note) => {
        this.form.patchValue({
          title: note.title,
          description: note.description,
          noteType: note.noteType,
          weekNumber: note.weekNumber ?? null,
          published: true
        });
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  submit() {
    this.submitted = true;

    if (this.form.invalid) {
      return;
    }

    this.saving = true;
    const value = this.form.getRawValue();

    this.noteService
      .updateNote(this.noteId, {
        title: value.title,
        description: value.description,
        noteType: value.noteType as any,
        weekNumber: value.weekNumber ?? undefined,
        published: value.published
      })
      .subscribe({
        next: () => {
          this.saving = false;
          this.toast.success('Note updated.');
          this.location.back();
        },
        error: () => (this.saving = false)
      });
  }

  goBack() {
    this.location.back();
  }
}
