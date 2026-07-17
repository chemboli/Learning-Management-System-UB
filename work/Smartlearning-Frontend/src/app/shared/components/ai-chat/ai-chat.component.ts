import { Component, ElementRef, OnInit, ViewChild, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService, ChatTurn } from '../../../core/services/chat.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-ai-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-chat.component.html',
  styleUrl: './ai-chat.component.scss'
})
export class AiChatComponent implements OnInit {
  @ViewChild('messagesEnd') messagesEnd?: ElementRef<HTMLDivElement>;

  open = signal(false);
  available = false;
  messages = signal<ChatTurn[]>([]);
  draft = '';
  sending = false;

  constructor(private chatService: ChatService, public auth: AuthService) {}

  ngOnInit(): void {
    this.chatService.status().subscribe({
      next: (res) => (this.available = res.configured),
      error: () => (this.available = false)
    });
  }

  toggle() {
    this.open.update((v) => !v);
  }

  send() {
    const text = this.draft.trim();
    if (!text || this.sending) return;

    const history = this.messages();
    this.messages.set([...history, { role: 'user', content: text }]);
    this.draft = '';
    this.sending = true;
    this.scrollToEndSoon();

    this.chatService.send({ message: text, history }).subscribe({
      next: (res) => {
        this.sending = false;
        this.messages.update((list) => [...list, { role: 'assistant', content: res.reply }]);
        this.scrollToEndSoon();
      },
      error: () => {
        this.sending = false;
        this.messages.update((list) => [
          ...list,
          {
            role: 'assistant',
            content: "Sorry, I couldn't reach the AI assistant just now. Please try again in a moment."
          }
        ]);
        this.scrollToEndSoon();
      }
    });
  }

  onKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  private scrollToEndSoon() {
    setTimeout(() => {
      this.messagesEnd?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'end' });
    }, 50);
  }
}
