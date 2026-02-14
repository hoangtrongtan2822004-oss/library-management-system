import {
  Component,
  ElementRef,
  OnInit,
  OnDestroy,
  ViewChild,
} from '@angular/core';
import { UserAuthService } from '../services/user-auth.service';
import { ChatbotService } from '../services/chatbot.service';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

interface Message {
  author: 'user' | 'bot';
  text: string;
  timestamp?: Date;
  id?: string;
  feedback?: 'up' | 'down';
  feedbackSent?: boolean;
  bookCard?: BookCard; // For interactive book responses
}

interface BookCard {
  id: number;
  title: string;
  author: string;
  imageUrl?: string;
  available: boolean;
}

@Component({
  selector: 'app-chatbot',
  templateUrl: './chatbot.component.html',
  styleUrls: ['./chatbot.component.css'],
  standalone: false,
})
export class ChatbotComponent implements OnInit, OnDestroy {
  @ViewChild('chatBody') private chatBody!: ElementRef;

  isOpen = false;
  isLoading = false;
  currentMessage = '';
  messages: Message[] = [];
  conversationId: string | null = null;
  private hasInitializedChat = false;

  // TTS
  ttsEnabled = false;
  ttsSupported = typeof window !== 'undefined' && 'speechSynthesis' in window;
  private ttsVoice?: SpeechSynthesisVoice;
  private lastUtterance: SpeechSynthesisUtterance | null = null;

  quickReplies: string[] = [
    'Sách Toán lớp 6 còn không?',
    'Thư viện mở cửa lúc nào?',
    'Hướng dẫn mượn sách',
  ];

  // Voice input
  isListening = false;
  recognition: any = null;
  voiceSupported = false;

  private destroy$ = new Subject<void>();
  private readonly STORAGE_KEY = 'chatbot_history';
  private readonly CONVERSATION_KEY = 'chatbot_conversation_id';
  // timeouts for pulse/hint so we can clear them on destroy
  private pulseTimeout: any = null;
  private hintTimeout: any = null;

  constructor(
    private userAuthService: UserAuthService,
    private chatbotService: ChatbotService,
  ) {
    this.initializeVoiceRecognition();
  }

  // Dynamic getter so chatbot re-checks login state on each render/change detection
  get isUser(): boolean {
    return this.userAuthService.isLoggedIn();
  }

  ngOnInit(): void {
    // Initialize conversation only on first user login
    this.initializeChatIfNeeded();

    // Load TTS preference
    const savedTts = localStorage.getItem('chatbot_tts_enabled');
    this.ttsEnabled = savedTts ? savedTts === 'true' : false;
    if (this.ttsSupported && window.speechSynthesis.getVoices().length) {
      this.ttsVoice = window.speechSynthesis
        .getVoices()
        .find((v) => v.lang.startsWith('vi'));
    }

    // Global keyboard shortcuts: Esc to close, Ctrl/Cmd+K to open & focus, Ctrl/Cmd+Enter to send
    window.addEventListener('keydown', this.onGlobalKeyDown);

    // Schedule subtle UI hints for discoverability (pulse FAB, show shortcut hint)
    try {
      this.pulseTimeout = setTimeout(() => {
        if (!this.isOpen) this.pulseFab();
      }, 6500);

      this.hintTimeout = setTimeout(() => {
        this.showShortcutHintIfNeeded();
      }, 12000);
    } catch (e) {
      // ignore scheduling errors
    }
  }

  // Arrow-bound handler so `this` remains correct and can be removed by reference
  private onGlobalKeyDown = (e: KeyboardEvent): void => {
    try {
      // Esc - close chat if open
      if (e.key === 'Escape') {
        if (this.isOpen) {
          this.isOpen = false;
          e.preventDefault();
        }
        return;
      }

      // Ctrl/Cmd + K - open chat and focus input
      const isModK = (e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k';
      if (isModK) {
        this.isOpen = true;
        setTimeout(() => {
          const input: HTMLInputElement | null =
            document.querySelector('.chat-footer input');
          if (input) {
            input.focus();
            input.select();
          }
        }, 50);
        e.preventDefault();
        return;
      }

      // Ctrl/Cmd + Enter - send message when chat open and input focused
      const isModEnter = (e.ctrlKey || e.metaKey) && e.key === 'Enter';
      if (isModEnter && this.isOpen) {
        // If there's a message, submit it
        if (this.currentMessage && this.currentMessage.trim().length > 0) {
          this.sendMessage();
        }
        e.preventDefault();
      }
    } catch (err) {
      // swallow errors from global handler to avoid breaking page
      console.error('Chat global key handler error', err);
    }
  };

  private initializeChatIfNeeded(): void {
    if (!this.hasInitializedChat && this.isUser) {
      this.hasInitializedChat = true;

      // Try to load existing chat from localStorage
      const savedHistory = this.loadChatHistory();
      const savedConversationId = localStorage.getItem(this.CONVERSATION_KEY);

      if (savedHistory && savedHistory.length > 0 && savedConversationId) {
        this.messages = savedHistory;
        this.conversationId = savedConversationId;
      } else {
        // Start fresh conversation
        this.conversationId = this.generateUUID();
        this.messages.push({
          author: 'bot',
          text: 'Hello! How can I help you find a book today? You can ask about books, authors, genres, or borrowing information.',
          timestamp: new Date(),
        });
        this.saveChatHistory();
      }
    }
  }

  private initializeVoiceRecognition(): void {
    const SpeechRecognition =
      (window as any).SpeechRecognition ||
      (window as any).webkitSpeechRecognition;

    if (SpeechRecognition) {
      this.voiceSupported = true;
      this.recognition = new SpeechRecognition();
      this.recognition.continuous = false;
      this.recognition.interimResults = false;
      this.recognition.lang = 'vi-VN'; // Vietnamese

      this.recognition.onresult = (event: any) => {
        const transcript = event.results[0][0].transcript;
        this.currentMessage = transcript;
        this.isListening = false;
      };

      this.recognition.onerror = (event: any) => {
        console.error('Speech recognition error:', event.error);
        this.isListening = false;
      };

      this.recognition.onend = () => {
        this.isListening = false;
        if (this.currentMessage && this.currentMessage.trim().length > 0) {
          this.sendMessage();
        }
      };
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();

    // Clean up voice recognition
    if (this.recognition) {
      this.recognition.stop();
    }

    // Stop any ongoing speech
    if (this.ttsSupported) {
      window.speechSynthesis.cancel();
    }

    // Remove global keyboard listener
    try {
      window.removeEventListener('keydown', this.onGlobalKeyDown);
    } catch (err) {}

    // Clear any hint/pulse timers
    try {
      if (this.pulseTimeout) {
        clearTimeout(this.pulseTimeout);
        this.pulseTimeout = null;
      }
      if (this.hintTimeout) {
        clearTimeout(this.hintTimeout);
        this.hintTimeout = null;
      }
    } catch (e) {}
  }

  toggleChat(): void {
    this.isOpen = !this.isOpen;
    // If opening the chat, clear any pending discovery hints
    if (this.isOpen) {
      try {
        if (this.pulseTimeout) {
          clearTimeout(this.pulseTimeout);
          this.pulseTimeout = null;
        }
        if (this.hintTimeout) {
          clearTimeout(this.hintTimeout);
          this.hintTimeout = null;
        }
      } catch (e) {}
    }
  }

  replayLastAnswer(): void {
    if (!this.ttsSupported) return;
    const lastBot = [...this.messages]
      .reverse()
      .find((m) => m.author === 'bot');
    if (lastBot?.text) {
      this.speakBotMessage(lastBot.text);
    }
  }

  toggleVoiceInput(): void {
    if (!this.voiceSupported) {
      alert('Trình duyệt của bạn không hỗ trợ nhập liệu bằng giọng nói.');
      return;
    }

    if (this.isListening) {
      this.recognition.stop();
      this.isListening = false;
    } else {
      this.recognition.start();
      this.isListening = true;
    }
  }

  sendMessage(): void {
    const userMessage = this.currentMessage.trim();
    if (!userMessage || this.isLoading || !this.conversationId) return;

    this.messages.push({
      author: 'user',
      text: userMessage,
      timestamp: new Date(),
      id: this.generateUUID(),
    });
    this.currentMessage = '';
    this.isLoading = true;
    this.saveChatHistory();
    this.scrollToBottom();

    this.chatbotService
      .ask(userMessage, this.conversationId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          let botText = '';

          // Handle different response formats
          if (response.error) {
            botText = response.error;
          } else if (response.answer) {
            // Format: {"answer":"...", "status":"ok", "conversationId":"..."}
            botText = response.answer;
          } else if (response.candidates && response.candidates[0]) {
            botText = response.candidates[0].content.parts[0].text;
          } else {
            botText = 'Sorry, I could not process that response.';
          }

          // Try to parse book card from response
          const bookCard = this.parseBookCard(botText);

          const botMessage: Message = {
            author: 'bot',
            text: botText,
            timestamp: new Date(),
            bookCard: bookCard || undefined,
            id: this.generateUUID(),
          };

          this.messages.push(botMessage);

          // Dynamic quick replies if provided
          if (Array.isArray((response as any).suggestions)) {
            this.quickReplies = (response as any).suggestions
              .map((s: any) => (typeof s === 'string' ? s : s?.label))
              .filter((s: string) => !!s);
          }

          this.speakBotMessage(botText);

          this.isLoading = false;
          this.saveChatHistory();
          this.scrollToBottom();
        },
        error: (err: HttpErrorResponse) => {
          const errorMsg = this.extractErrorMessage(err);
          this.messages.push({
            author: 'bot',
            text: errorMsg,
            timestamp: new Date(),
            id: this.generateUUID(),
          });
          this.isLoading = false;
          this.saveChatHistory();
          this.scrollToBottom();
        },
      });
  }

  fillMessage(text: string): void {
    this.currentMessage = text;
    // Gửi ngay sau khi gán để học sinh không phải bấm thêm
    this.sendMessage();
  }

  clearChat(): void {
    this.messages = [];
    this.conversationId = this.generateUUID();
    this.messages.push({
      author: 'bot',
      text: 'Chat cleared. Starting a new conversation. How can I help you?',
      timestamp: new Date(),
    });
    this.saveChatHistory();
  }

  borrowBook(bookId: number): void {
    // Navigate to borrow page with book ID
    window.location.href = `/borrow-book?bookId=${bookId}`;
  }

  viewBookDetails(bookId: number): void {
    // Navigate to book details page
    window.location.href = `/book-details/${bookId}`;
  }

  private parseBookCard(text: string): BookCard | null {
    // Try to extract book information from bot response
    // Format: "BOOK_CARD:{id:123,title:'Clean Code',author:'Robert Martin',available:true}"
    const bookCardMatch = text.match(/BOOK_CARD:\{([^}]+)\}/);
    if (!bookCardMatch) return null;

    try {
      const cardData = bookCardMatch[1];
      const id = cardData.match(/id:(\d+)/)?.[1];
      const title = cardData.match(/title:'([^']+)'/)?.[1];
      const author = cardData.match(/author:'([^']+)'/)?.[1];
      const available =
        cardData.match(/available:(true|false)/)?.[1] === 'true';

      if (id && title && author) {
        return {
          id: parseInt(id),
          title,
          author,
          available,
        };
      }
    } catch (e) {
      console.error('Failed to parse book card:', e);
    }
    return null;
  }

  private saveChatHistory(): void {
    try {
      // Save messages (limit to last 50 to avoid storage issues)
      const messagesToSave = this.messages.slice(-50);
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(messagesToSave));

      // Save conversation ID
      if (this.conversationId) {
        localStorage.setItem(this.CONVERSATION_KEY, this.conversationId);
      }
    } catch (e) {
      console.error('Failed to save chat history:', e);
    }
  }

  private loadChatHistory(): Message[] | null {
    try {
      const saved = localStorage.getItem(this.STORAGE_KEY);
      if (saved) {
        const messages = JSON.parse(saved);
        // Convert timestamp strings back to Date objects
        return messages.map((msg: any) => ({
          ...msg,
          timestamp: msg.timestamp ? new Date(msg.timestamp) : undefined,
        }));
      }
    } catch (e) {
      console.error('Failed to load chat history:', e);
    }
    return null;
  }

  private extractErrorMessage(err: HttpErrorResponse): string {
    if (err.error?.error) {
      return err.error.error;
    }
    if (err.error?.message) {
      return err.error.message;
    }
    if (err.status === 401) {
      return 'Session expired. Please log in again.';
    }
    if (err.status === 403) {
      return 'You do not have permission to use the chat feature.';
    }
    if (err.status === 429) {
      return 'Too many requests. Please wait a moment and try again.';
    }
    if (err.status >= 500) {
      return 'Server error. Please try again later.';
    }
    return 'Sorry, I encountered an error. Please try again.';
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      try {
        this.chatBody.nativeElement.scrollTop =
          this.chatBody.nativeElement.scrollHeight;
      } catch (err) {}
    }, 10);
  }

  private generateUUID(): string {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(
      /[xy]/g,
      function (c) {
        const r = (Math.random() * 16) | 0;
        const v = c === 'x' ? r : (r & 0x3) | 0x8;
        return v.toString(16);
      },
    );
  }

  // --- UI discoverability helpers ---
  private clearHintAndPulseTimers(): void {
    try {
      if (this.pulseTimeout) {
        clearTimeout(this.pulseTimeout);
        this.pulseTimeout = null;
      }
      if (this.hintTimeout) {
        clearTimeout(this.hintTimeout);
        this.hintTimeout = null;
      }
    } catch (e) {}
  }

  private pulseFab(): void {
    try {
      const fab = document.querySelector('.chat-fab') as HTMLElement | null;
      if (!fab) return;
      // Add a temporary class that should be styled in CSS for a gentle pulse
      fab.classList.add('pulse');
      // Remove after animation completes (2s)
      setTimeout(() => fab.classList.remove('pulse'), 2000);
    } catch (e) {
      // swallow DOM errors
    }
  }

  private showShortcutHintIfNeeded(): void {
    try {
      // Don't show more than once per user
      if (localStorage.getItem('chatbot_shortcut_hint_shown')) return;

      const fab = document.querySelector('.chat-fab') as HTMLElement | null;
      if (!fab) return;

      const hint = document.createElement('div');
      hint.className = 'chatbot-shortcut-hint';
      hint.setAttribute('role', 'status');
      hint.setAttribute('aria-live', 'polite');
      hint.textContent = 'Nhấn Ctrl/Cmd+K để mở chat nhanh';
      // Basic inline styling to ensure visibility without changing global CSS files
      hint.style.position = 'fixed';
      hint.style.right = '20px';
      hint.style.bottom = '84px';
      hint.style.background = 'rgba(17,24,39,0.9)';
      hint.style.color = 'white';
      hint.style.padding = '8px 12px';
      hint.style.borderRadius = '8px';
      hint.style.zIndex = '9999';
      hint.style.fontSize = '0.95rem';
      hint.style.boxShadow = '0 6px 20px rgba(0,0,0,0.2)';

      document.body.appendChild(hint);

      // Fade out and remove after 4.5s
      setTimeout(() => {
        try {
          hint.style.transition = 'opacity 0.6s ease';
          hint.style.opacity = '0';
        } catch (e) {}
      }, 3500);
      setTimeout(() => hint.remove(), 4200);

      localStorage.setItem('chatbot_shortcut_hint_shown', 'true');
    } catch (e) {
      // ignore
    }
  }

  // === Feedback ===
  sendFeedback(message: Message, isPositive: boolean): void {
    if (!message || message.feedbackSent || message.author !== 'bot') return;
    const payload = {
      conversationId: this.conversationId,
      messageId: message.id,
      helpful: isPositive,
    };

    this.chatbotService
      .sendFeedback(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          message.feedback = isPositive ? 'up' : 'down';
          message.feedbackSent = true;
          this.saveChatHistory();
        },
        error: () => {
          message.feedback = isPositive ? 'up' : 'down';
          message.feedbackSent = true;
          this.saveChatHistory();
        },
      });
  }

  // === Text-to-Speech ===
  toggleTts(): void {
    this.ttsEnabled = !this.ttsEnabled;
    localStorage.setItem('chatbot_tts_enabled', String(this.ttsEnabled));
    if (!this.ttsEnabled && this.ttsSupported) {
      window.speechSynthesis.cancel();
    }
  }

  private speakBotMessage(text: string): void {
    if (!this.ttsSupported || !this.ttsEnabled || !text) return;
    try {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = 'vi-VN';
      if (this.ttsVoice) {
        utterance.voice = this.ttsVoice;
      }
      this.lastUtterance = utterance;
      window.speechSynthesis.speak(utterance);
    } catch (err) {
      console.error('TTS speak error', err);
    }
  }
}
