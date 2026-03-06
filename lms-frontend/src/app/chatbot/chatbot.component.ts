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
  bookCards?: BookCard[]; // All books mentioned in the message (carousel)
  recommendations?: RecommendationCard[];
  isStreaming?: boolean;
  pinned?: boolean; // user can bookmark important bot messages
}

interface BookCard {
  id: number;
  title: string;
  author: string;
  imageUrl?: string;
  available: boolean;
}

interface RecommendationCard {
  id?: number | null;
  title: string;
  reason: string;
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
  isExpanded = false;
  isLoading = false;
  isLoadingRecommendations = false;
  isLoadingInsight = false;
  currentMessage = '';
  messages: Message[] = [];
  conversationId: string | null = null;
  private hasInitializedChat = false;
  unreadCount = 0;
  copiedMessageId: string | null = null;

  // Conversation history sidebar
  showHistory = false;
  isLoadingHistory = false;
  conversationList: string[] = [];
  conversationTitles: Record<string, string> = {};
  private readonly TITLES_KEY = 'chatbot_conv_titles';

  // History search filter
  historySearchQuery = '';

  // Conversation rename state
  renamingConvId: string | null = null;

  // Scroll-to-bottom button
  showScrollDown = false;

  // Rate-limit countdown (seconds remaining until requests are allowed again)
  rateLimitCountdown = 0;
  private countdownInterval: any = null;

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
  voicePreviewActive = false; // true while transcript is staged but not yet sent
  recognition: any = null;
  voiceSupported = false;

  private destroy$ = new Subject<void>();
  private readonly STORAGE_KEY = 'chatbot_history';
  private readonly CONVERSATION_KEY = 'chatbot_conversation_id';
  private readonly PINNED_KEY = 'chatbot_pinned';
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

  /** Filtered conversation list based on search query */
  get filteredConversationList(): string[] {
    if (!this.historySearchQuery.trim()) return this.conversationList;
    const q = this.historySearchQuery.trim().toLowerCase();
    return this.conversationList.filter((id) => {
      const title = (
        this.conversationTitles[id] || id.substring(0, 8)
      ).toLowerCase();
      return title.includes(q);
    });
  }

  ngOnInit(): void {
    // Initialize conversation only on first user login
    this.initializeChatIfNeeded();

    // Load conversation titles
    try {
      const savedTitles = localStorage.getItem(this.TITLES_KEY);
      if (savedTitles) this.conversationTitles = JSON.parse(savedTitles);
    } catch (e) {
      /* ignore corrupt data */
    }

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
          text: 'Xin chào! Mình là **Trợ lý Thư viện AI** 📚\n\nBạn có thể hỏi mình về:\n- Tìm sách theo tên, tác giả, thể loại\n- Thông tin mượn/trả sách\n- Đánh giá và nội dung sách\n\n💡 Nhấn nút **✨** ở dưới để nhận **gợi ý sách AI** cá nhân hóa dành riêng cho bạn!',
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
        // Show preview — user confirms by pressing Send (Enter or button)
        if (this.currentMessage && this.currentMessage.trim().length > 0) {
          this.voicePreviewActive = true;
        }
      };
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();

    // Close any active SSE stream
    if (this.streamController) {
      this.streamController.abort();
      this.streamController = null;
    }

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

    // Clear rate-limit countdown
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
      this.countdownInterval = null;
    }

    // Clear pending timeouts
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

  toggleExpand(): void {
    this.isExpanded = !this.isExpanded;
  }

  toggleChat(): void {
    this.isOpen = !this.isOpen;
    if (!this.isOpen) this.isExpanded = false;
    // If opening the chat, clear unread badge and pending hints
    if (this.isOpen) {
      this.unreadCount = 0;
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

  // Whether to use streaming (SSE) or synchronous mode
  useStreaming = true;
  private streamController: AbortController | null = null;

  sendMessage(): void {
    const userMessage = this.currentMessage.trim();
    if (
      !userMessage ||
      this.isLoading ||
      this.rateLimitCountdown > 0 ||
      !this.conversationId
    )
      return;

    this.messages.push({
      author: 'user',
      text: userMessage,
      timestamp: new Date(),
      id: this.generateUUID(),
    });
    this.currentMessage = '';
    this.voicePreviewActive = false;
    this.isLoading = true;

    // Auto-title: capture the first user message in each conversation
    const userMsgs = this.messages.filter((m) => m.author === 'user');
    if (userMsgs.length === 1 && this.conversationId) {
      const title =
        userMessage.length > 42
          ? userMessage.substring(0, 42) + '…'
          : userMessage;
      this.conversationTitles[this.conversationId] = title;
      try {
        localStorage.setItem(
          this.TITLES_KEY,
          JSON.stringify(this.conversationTitles),
        );
      } catch (e) {}
    }

    this.saveChatHistory();
    this.scrollToBottom();

    if (this.useStreaming) {
      this.sendMessageStreaming(userMessage);
    } else {
      this.sendMessageSync(userMessage);
    }
  }

  private sendMessageStreaming(userMessage: string): void {
    // Create bot message placeholder for streaming text
    const botMessage: Message = {
      author: 'bot',
      text: '',
      timestamp: new Date(),
      id: this.generateUUID(),
    };
    this.messages.push(botMessage);
    this.scrollToBottom();

    // Cancel any previous stream
    if (this.streamController) {
      this.streamController.abort();
    }

    let endHandled = false;

    this.streamController = this.chatbotService.streamChat(
      userMessage,
      this.conversationId!,
      {
        onStart: (data) => {
          if (data.conversationId) {
            this.conversationId = data.conversationId;
          }
        },
        onToken: (token) => {
          botMessage.text += token;
          botMessage.isStreaming = true;
          this.scrollToBottom();
        },
        onEnd: () => {
          if (endHandled) return;
          endHandled = true;
          this.streamController = null;
          botMessage.isStreaming = false;

          // Parse ALL book cards from complete response, then strip markup
          const cards = this.parseBookCards(botMessage.text);
          botMessage.bookCards = cards.length > 0 ? cards : undefined;
          botMessage.text = this.stripBookCards(botMessage.text);

          this.incrementUnreadIfClosed();
          this.speakBotMessage(botMessage.text);
          this.isLoading = false;
          this.saveChatHistory();
          this.scrollToBottom();
        },
        onError: (err) => {
          if (endHandled) return;
          endHandled = true;
          this.streamController = null;

          console.warn(
            '[Chatbot] Streaming failed, falling back to sync:',
            err?.message,
          );

          // ❌ Streaming failed → fall back to sync mode automatically
          // Remove the empty bot placeholder
          const idx = this.messages.indexOf(botMessage);
          if (idx !== -1) this.messages.splice(idx, 1);

          this.sendMessageSync(userMessage);
        },
      },
    );
  }

  private sendMessageSync(userMessage: string): void {
    this.chatbotService
      .ask(userMessage, this.conversationId!)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          let botText = '';

          // Handle different response formats
          if (response.error) {
            botText = response.error;
          } else if (response.answer) {
            botText = response.answer;
          } else if (response.candidates && response.candidates[0]) {
            botText = response.candidates[0].content.parts[0].text;
          } else {
            botText = 'Sorry, I could not process that response.';
          }

          // Parse ALL book cards from response, then strip markup
          const bookCards = this.parseBookCards(botText);
          botText = this.stripBookCards(botText);

          const botMessage: Message = {
            author: 'bot',
            text: botText,
            timestamp: new Date(),
            bookCards: bookCards.length > 0 ? bookCards : undefined,
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
          this.incrementUnreadIfClosed();
          this.isLoading = false;
          this.saveChatHistory();
          this.scrollToBottom();
        },
        error: (err: HttpErrorResponse) => {
          // Start countdown timer when rate-limited
          if (err.status === 429) {
            const retryAfter = parseInt(
              err.headers?.get('Retry-After') || '30',
              10,
            );
            this.startRateLimitCountdown(isNaN(retryAfter) ? 30 : retryAfter);
          }
          const errorMsg = this.extractErrorMessage(err);
          this.messages.push({
            author: 'bot',
            text: errorMsg,
            timestamp: new Date(),
            id: this.generateUUID(),
          });
          this.incrementUnreadIfClosed();
          this.isLoading = false;
          this.saveChatHistory();
          this.scrollToBottom();
        },
      });
  }

  private startRateLimitCountdown(seconds: number): void {
    if (this.countdownInterval) clearInterval(this.countdownInterval);
    this.rateLimitCountdown = seconds;
    this.countdownInterval = setInterval(() => {
      this.rateLimitCountdown--;
      if (this.rateLimitCountdown <= 0) {
        clearInterval(this.countdownInterval);
        this.countdownInterval = null;
        this.rateLimitCountdown = 0;
      }
    }, 1000);
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
      text: '🗑️ Đã xóa cuộc trò chuyện. Bắt đầu cuộc hội thoại mới — mình có thể giúp gì cho bạn?',
      timestamp: new Date(),
    });
    this.saveChatHistory();
  }

  toggleHistory(): void {
    this.showHistory = !this.showHistory;
    if (this.showHistory) {
      this.loadConversationList();
      this.historySearchQuery = ''; // reset search on open
    }
  }

  startNewConversation(): void {
    this.clearChat();
    this.showHistory = false;
  }

  // === Conversation rename ===
  startRename(convId: string, event: MouseEvent): void {
    event.stopPropagation();
    this.renamingConvId = convId;
  }

  confirmRename(convId: string, value: string): void {
    const title = value.trim();
    this.renamingConvId = null;
    if (!title) return;

    this.conversationTitles[convId] = title;
    try {
      localStorage.setItem(
        this.TITLES_KEY,
        JSON.stringify(this.conversationTitles),
      );
    } catch (e) {
      /* ignore */
    }

    // Persist to backend
    this.chatbotService
      .renameConversation(convId, title)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        error: (err) =>
          console.error('Failed to persist conversation rename:', err),
      });
  }

  cancelRename(): void {
    this.renamingConvId = null;
  }

  // === Scroll-to-bottom button ===
  onChatScroll(): void {
    const el = this.chatBody?.nativeElement;
    if (!el) return;
    this.showScrollDown =
      el.scrollHeight - el.scrollTop - el.clientHeight > 160;
  }

  scrollDown(): void {
    this.scrollToBottom();
  }

  // === Regenerate last bot response ===
  regenerateLastResponse(): void {
    if (this.isLoading) return;
    const lastUserMsg = [...this.messages]
      .reverse()
      .find((m) => m.author === 'user');
    if (!lastUserMsg) return;

    // Remove the last bot message in the array
    const lastBotIdx = this.messages.reduce(
      (acc, m, i) => (m.author === 'bot' ? i : acc),
      -1,
    );
    if (lastBotIdx !== -1) {
      this.messages.splice(lastBotIdx, 1);
    }

    this.isLoading = true;
    this.saveChatHistory();
    this.scrollToBottom();
    if (this.useStreaming) {
      this.sendMessageStreaming(lastUserMsg.text);
    } else {
      this.sendMessageSync(lastUserMsg.text);
    }
  }

  // === Pin / Bookmark messages ===
  togglePin(message: Message): void {
    message.pinned = !message.pinned;
    this.saveChatHistory();
  }

  deleteConversation(convId: string, event: MouseEvent): void {
    event.stopPropagation();
    if (!confirm('Xóa cuộc hội thoại này? Hành động không thể hoàn tác.'))
      return;

    this.chatbotService
      .deleteConversation(convId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.conversationList = this.conversationList.filter(
            (c) => c !== convId,
          );
          delete this.conversationTitles[convId];
          try {
            localStorage.setItem(
              this.TITLES_KEY,
              JSON.stringify(this.conversationTitles),
            );
          } catch (e) {
            /* ignore */
          }
          // If deleted the active conversation, start a fresh one
          if (this.conversationId === convId) {
            this.clearChat();
          }
        },
        error: (err) => {
          console.error('Failed to delete conversation:', err);
        },
      });
  }

  private loadConversationList(): void {
    this.isLoadingHistory = true;
    // Refresh in-memory titles from localStorage
    try {
      const savedTitles = localStorage.getItem(this.TITLES_KEY);
      if (savedTitles) this.conversationTitles = JSON.parse(savedTitles);
    } catch (e) {
      /* ignore */
    }

    this.chatbotService
      .getUserConversations()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          this.conversationList = res.conversations || [];
          // Merge server-side titles — server wins over local placeholders
          const serverTitles: Record<string, string> = res.titles || {};
          Object.entries(serverTitles).forEach(([id, t]) => {
            if (t) this.conversationTitles[id] = t as string;
          });
          try {
            localStorage.setItem(
              this.TITLES_KEY,
              JSON.stringify(this.conversationTitles),
            );
          } catch (e) {
            /* ignore */
          }
          this.isLoadingHistory = false;
        },
        error: () => {
          this.conversationList = [];
          this.isLoadingHistory = false;
        },
      });
  }

  loadConversation(convId: string): void {
    if (convId === this.conversationId) {
      this.showHistory = false;
      return;
    }
    this.isLoadingHistory = true;
    this.chatbotService
      .getConversationHistory(convId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          const history: any[] = res.history || [];
          this.messages = [];
          history.forEach((msg: any) => {
            // Each ChatMessage has userMessage + botResponse
            if (msg.userMessage) {
              this.messages.push({
                author: 'user',
                text: msg.userMessage,
                timestamp: msg.createdAt ? new Date(msg.createdAt) : undefined,
                id: this.generateUUID(),
              });
            }
            if (msg.botResponse) {
              // Extract text from Gemini JSON if needed
              let botText = msg.botResponse;
              try {
                const parsed = JSON.parse(msg.botResponse);
                if (parsed?.candidates?.[0]?.content?.parts?.[0]?.text) {
                  botText = parsed.candidates[0].content.parts[0].text;
                }
              } catch (e) {
                // already plain text
              }
              const cards = this.parseBookCards(botText);
              botText = this.stripBookCards(botText);
              this.messages.push({
                author: 'bot',
                text: botText,
                timestamp: msg.createdAt ? new Date(msg.createdAt) : undefined,
                id: this.generateUUID(),
                bookCards: cards.length > 0 ? cards : undefined,
              });
            }
          });
          this.conversationId = convId;
          localStorage.setItem(this.CONVERSATION_KEY, convId);
          this.saveChatHistory();
          this.isLoadingHistory = false;
          this.showHistory = false;
          this.scrollToBottom();
        },
        error: () => {
          this.isLoadingHistory = false;
        },
      });
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    if (!file.type.startsWith('image/')) {
      this.messages.push({
        author: 'bot',
        text: 'Vui lòng chọn file ảnh (JPEG, PNG, v.v.).',
        timestamp: new Date(),
        id: this.generateUUID(),
      });
      return;
    }

    // Show user what they uploaded
    this.messages.push({
      author: 'user',
      text: `📸 Tải ảnh bìa sách: ${file.name}`,
      timestamp: new Date(),
      id: this.generateUUID(),
    });
    this.isLoading = true;
    this.scrollToBottom();

    this.chatbotService
      .extractBookInfo(file)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          const info = res.bookInfo;
          let botText = '📖 **Thông tin sách từ ảnh:**\n\n';
          if (info.title) botText += `**Tên sách:** ${info.title}\n`;
          if (info.authors && info.authors.length > 0) {
            botText += `**Tác giả:** ${info.authors.join(', ')}\n`;
          }
          if (info.isbn) botText += `**ISBN:** ${info.isbn}\n`;
          if (info.description) botText += `**Mô tả:** ${info.description}\n`;

          botText += '\n_Bạn có muốn tìm sách này trong thư viện không?_';

          // Set the title as a suggestion for easy follow-up
          if (info.title) {
            this.quickReplies = [
              `Tìm sách "${info.title}"`,
              `Sách của ${info.authors?.[0] || 'tác giả này'}`,
              'Hướng dẫn mượn sách',
            ];
          }

          this.messages.push({
            author: 'bot',
            text: botText,
            timestamp: new Date(),
            id: this.generateUUID(),
          });
          this.isLoading = false;
          this.saveChatHistory();
          this.scrollToBottom();
        },
        error: (err: HttpErrorResponse) => {
          const detail: string =
            err.error?.message ||
            err.error?.error ||
            (typeof err.error === 'string' ? err.error : null) ||
            'Không thể đọc thông tin từ ảnh. Vui lòng thử ảnh khác hoặc nhập tên sách trực tiếp.';
          this.messages.push({
            author: 'bot',
            text: `❌ ${detail}`,
            timestamp: new Date(),
            id: this.generateUUID(),
          });
          this.isLoading = false;
          this.saveChatHistory();
          this.scrollToBottom();
        },
      });

    // Reset input so user can upload same file again
    input.value = '';
  }

  borrowBook(bookId: number): void {
    // Navigate to borrow page with book ID
    window.location.href = `/borrow-book?bookId=${bookId}`;
  }

  viewBookDetails(bookId: number): void {
    // Navigate to book details page
    window.location.href = `/book-details/${bookId}`;
  }

  /**
   * Parse ALL BOOK_CARD entries from a bot response.
   * Format: BOOK_CARD:{id:123,title:'...',author:'...',available:true}
   */
  private parseBookCards(text: string): BookCard[] {
    const regex = /BOOK_CARD:\{([^}]+)\}/g;
    let match;
    const cards: BookCard[] = [];

    while ((match = regex.exec(text)) !== null) {
      try {
        const d = match[1];
        const id = d.match(/id:(\d+)/)?.[1];
        const title = d.match(/title:'([^']+)'/)?.[1];
        const author = d.match(/author:'([^']+)'/)?.[1];
        const available = d.match(/available:(true|false)/)?.[1] === 'true';
        if (id && title && author) {
          cards.push({ id: parseInt(id), title, author, available });
        }
      } catch (e) {
        console.error('Failed to parse book card:', e);
      }
    }
    return cards;
  }

  /**
   * Remove all BOOK_CARD:{...} markup from text so it isn't shown to the user.
   */
  private stripBookCards(text: string): string {
    return text.replace(/\s*BOOK_CARD:\{[^}]+\}/g, '').trim();
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
      const retryAfter = parseInt(err.headers?.get('Retry-After') || '0', 10);
      const waitMsg =
        retryAfter > 0
          ? ` Vui lòng đợi ${retryAfter} giây.`
          : ' Vui lòng thử lại sau.';
      return `⏳ Quá nhiều yêu cầu.${waitMsg}`;
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
        this.showScrollDown = false;
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

  // === AI Recommendations ===
  getAiRecommendations(): void {
    if (this.isLoadingRecommendations || this.isLoading) return;

    this.messages.push({
      author: 'user',
      text: '✨ Gợi ý sách cho tôi',
      timestamp: new Date(),
      id: this.generateUUID(),
    });
    this.isLoadingRecommendations = true;
    this.scrollToBottom();

    this.chatbotService
      .getRecommendations()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          let recs: RecommendationCard[] = [];
          try {
            if (res.recommendations) {
              recs =
                typeof res.recommendations === 'string'
                  ? JSON.parse(res.recommendations)
                  : res.recommendations;
            }
          } catch (e) {
            console.error('Failed to parse recommendations', e);
          }

          this.messages.push({
            author: 'bot',
            text:
              recs.length > 0
                ? '✨ **Gợi ý sách dành riêng cho bạn:**'
                : 'Xin lỗi, mình chưa có đủ dữ liệu để gợi ý sách cho bạn. Hãy mượn thêm sách để mình hiểu sở thích của bạn hơn nhé! 📚',
            timestamp: new Date(),
            id: this.generateUUID(),
            recommendations: recs.length > 0 ? recs : undefined,
          });
          this.incrementUnreadIfClosed();
          this.isLoadingRecommendations = false;
          this.saveChatHistory();
          this.scrollToBottom();
        },
        error: (_err: HttpErrorResponse) => {
          this.messages.push({
            author: 'bot',
            text: '❌ Không thể tải gợi ý sách lúc này. Vui lòng thử lại sau.',
            timestamp: new Date(),
            id: this.generateUUID(),
          });
          this.incrementUnreadIfClosed();
          this.isLoadingRecommendations = false;
          this.saveChatHistory();
          this.scrollToBottom();
        },
      });
  }

  // === Reading Insight ===
  getReadingInsight(): void {
    if (this.isLoadingInsight || this.isLoading) return;

    this.messages.push({
      author: 'user',
      text: '📊 Phân tích thói quen đọc sách của tôi',
      timestamp: new Date(),
      id: this.generateUUID(),
    });
    this.isLoadingInsight = true;
    this.scrollToBottom();

    this.chatbotService
      .getReadingInsight()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          const insight = res?.insight || 'Không thể tạo nhận xét lúc này.';
          const stats = res?.stats;
          let statsText = '';
          if (stats?.totalBooks) {
            statsText = `\n\n📚 **Tổng sách đã mượn:** ${stats.totalBooks}`;
            if (stats.topCategories)
              statsText += `\n🏷️ **Thể loại yêu thích:** ${stats.topCategories}`;
            if (stats.topAuthors)
              statsText += `\n✍️ **Tác giả đọc nhiều:** ${stats.topAuthors}`;
          }
          this.messages.push({
            author: 'bot',
            text: `📊 **Nhận xét về thói quen đọc sách của bạn:**\n\n${insight}${statsText}`,
            timestamp: new Date(),
            id: this.generateUUID(),
          });
          this.incrementUnreadIfClosed();
          this.isLoadingInsight = false;
          this.saveChatHistory();
          this.scrollToBottom();
        },
        error: () => {
          this.messages.push({
            author: 'bot',
            text: '❌ Không thể phân tích thói quen đọc sách lúc này. Vui lòng thử lại sau.',
            timestamp: new Date(),
            id: this.generateUUID(),
          });
          this.incrementUnreadIfClosed();
          this.isLoadingInsight = false;
          this.saveChatHistory();
          this.scrollToBottom();
        },
      });
  }

  // === Export chat ===
  exportChat(): void {
    if (this.messages.length === 0) return;
    const lines: string[] = [
      '=== Lịch sử trò chuyện - Thư viện THCS Phương Tú ===',
      `Thời gian xuất: ${new Date().toLocaleString('vi-VN')}`,
      '',
    ];
    for (const msg of this.messages) {
      const time = msg.timestamp ? msg.timestamp.toLocaleString('vi-VN') : '';
      const author = msg.author === 'user' ? 'Bạn' : 'Trợ lý AI';
      const text = msg.text
        .replace(/\*\*/g, '')
        .replace(/\*/g, '')
        .replace(/^#+\s/gm, '')
        .trim();
      lines.push(`[${time}] ${author}:`);
      lines.push(text);
      lines.push('');
    }
    const blob = new Blob([lines.join('\n')], {
      type: 'text/plain;charset=utf-8',
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `chat-${new Date().toISOString().slice(0, 10)}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  }

  // === Copy message ===
  copyMessage(text: string, messageId?: string): void {
    try {
      const cleaned = text
        .replace(/\*\*/g, '')
        .replace(/\*/g, '')
        .replace(/^#+\s/gm, '')
        .trim();
      navigator.clipboard.writeText(cleaned).then(() => {
        this.copiedMessageId = messageId || null;
        setTimeout(() => {
          if (this.copiedMessageId === (messageId || null))
            this.copiedMessageId = null;
        }, 2000);
      });
    } catch (e) {
      console.error('Copy failed', e);
    }
  }

  private incrementUnreadIfClosed(): void {
    if (!this.isOpen) {
      this.unreadCount++;
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
