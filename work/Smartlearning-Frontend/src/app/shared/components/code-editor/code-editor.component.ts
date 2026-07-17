import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { basicSetup } from 'codemirror';
import { EditorView, keymap } from '@codemirror/view';
import { EditorState, Compartment } from '@codemirror/state';
import { indentWithTab } from '@codemirror/commands';
import { java } from '@codemirror/lang-java';
import { python } from '@codemirror/lang-python';
import { cpp } from '@codemirror/lang-cpp';
import { LanguageSupport } from '@codemirror/language';

export type EditorLanguage = 'JAVA' | 'PYTHON' | 'C' | 'CPP';

/**
 * Thin Angular wrapper around CodeMirror 6. Used for in-browser submission of
 * programming assignments — students write code here instead of uploading a
 * file. The resulting text is submitted through the exact same backend
 * endpoint as a file upload (see assignment-detail component), just built
 * into a Blob/File client-side instead of coming from a file picker.
 */
@Component({
  selector: 'app-code-editor',
  standalone: true,
  template: `<div #host class="code-editor-host"></div>`,
  styleUrl: './code-editor.component.scss'
})
export class CodeEditorComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('host', { static: true }) host!: ElementRef<HTMLDivElement>;

  @Input() language: EditorLanguage = 'JAVA';
  @Input() initialValue = '';
  @Output() valueChange = new EventEmitter<string>();

  private view?: EditorView;
  private languageCompartment = new Compartment();

  ngAfterViewInit(): void {
    const state = EditorState.create({
      doc: this.initialValue,
      extensions: [
        basicSetup,
        keymap.of([indentWithTab]),
        this.languageCompartment.of(this.languageExtension()),
        EditorView.updateListener.of((update) => {
          if (update.docChanged) {
            this.valueChange.emit(update.state.doc.toString());
          }
        }),
        EditorView.theme({
          '&': { fontSize: '0.88rem' },
          '.cm-content': { fontFamily: "'JetBrains Mono', 'Fira Code', Consolas, monospace" },
          '.cm-scroller': { overflow: 'auto' }
        })
      ]
    });

    this.view = new EditorView({
      state,
      parent: this.host.nativeElement
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Language can change after the view is created (e.g. assignment data
    // loads after the editor mounts) — reconfigure in place rather than
    // tearing down and rebuilding the whole editor.
    if (changes['language'] && this.view && !changes['language'].firstChange) {
      this.view.dispatch({
        effects: this.languageCompartment.reconfigure(this.languageExtension())
      });
    }
  }

  ngOnDestroy(): void {
    this.view?.destroy();
  }

  getValue(): string {
    return this.view?.state.doc.toString() ?? '';
  }

  setValue(value: string) {
    if (!this.view) return;
    this.view.dispatch({
      changes: { from: 0, to: this.view.state.doc.length, insert: value }
    });
  }

  private languageExtension(): LanguageSupport {
    switch (this.language) {
      case 'PYTHON':
        return python();
      case 'C':
      case 'CPP':
        return cpp();
      case 'JAVA':
      default:
        return java();
    }
  }
}
