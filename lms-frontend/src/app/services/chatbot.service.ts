import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { ApiService } from './api.service';
import { UserAuthService } from './user-auth.service';

@Injectable({
  providedIn: 'root',
})
export class ChatbotService {
  private apiUrl: string;

  constructor(
    private http: HttpClient,
    private apiService: ApiService,
    private userAuthService: UserAuthService,
  ) {
    // Use centralized API service for URL management
    this.apiUrl = this.apiService.buildUrl('/user/chat');
  }

  /**
   * Send message with conversation support (RAG)
   */
  ask(prompt: string, conversationId?: string): Observable<any> {
    const payload = {
      prompt: prompt,
      conversationId: conversationId || undefined,
    };

    return this.http.post<any>(this.apiUrl, payload);
  }

  /**
   * Get conversation history
   */
  getConversationHistory(conversationId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/history/${conversationId}`);
  }

  /**
   * Get all conversations for user
   */
  getUserConversations(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/conversations`);
  }

  /**
   * Send feedback for a bot message
   */
  sendFeedback(payload: {
    conversationId: string | null;
    messageId?: string;
    helpful: boolean;
  }): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/feedback`, payload);
  }

  /**
   * Stream chat response via fetch + ReadableStream (SSE).
   * Uses Authorization header instead of query param token — more secure & reliable.
   * Returns an AbortController so the caller can cancel the stream.
   */
  streamChat(
    prompt: string,
    conversationId: string | undefined,
    callbacks: {
      onStart?: (data: any) => void;
      onToken?: (token: string) => void;
      onEnd?: () => void;
      onError?: (err: any) => void;
    },
  ): AbortController {
    const controller = new AbortController();
    const token = this.userAuthService.getToken();

    let url = `${this.apiUrl}/stream?prompt=${encodeURIComponent(prompt)}`;
    if (conversationId) {
      url += `&conversationId=${encodeURIComponent(conversationId)}`;
    }

    const headers: Record<string, string> = {
      Accept: 'text/event-stream',
    };
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    fetch(url, { method: 'GET', headers, signal: controller.signal })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const reader = response.body?.getReader();
        if (!reader) {
          throw new Error('ReadableStream not supported');
        }

        const decoder = new TextDecoder();
        let buffer = '';
        let currentEvent = ''; // tracks the event type from "event:" lines

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });

          // SSE spec: events are separated by double newlines
          const blocks = buffer.split('\n\n');
          buffer = blocks.pop() || ''; // keep incomplete last block

          for (const block of blocks) {
            if (!block.trim()) continue;

            let eventName = '';
            let eventData = '';

            for (const line of block.split('\n')) {
              if (line.startsWith('event:')) {
                eventName = line.substring(6).trim();
              } else if (line.startsWith('data:')) {
                eventData = line.substring(5);
              }
            }

            if (!eventName && !eventData) continue;

            switch (eventName) {
              case 'start':
                try {
                  const json = JSON.parse(eventData);
                  callbacks.onStart?.(json);
                } catch {
                  /* ignore */
                }
                break;

              case 'message':
                callbacks.onToken?.(eventData);
                break;

              case 'end':
                callbacks.onEnd?.();
                return; // done

              case 'error':
                throw new Error(eventData || 'Server error');

              default:
                // Fallback: treat unknown event data as token
                if (eventData) {
                  callbacks.onToken?.(eventData);
                }
                break;
            }
          }
        }

        // Stream ended naturally
        callbacks.onEnd?.();
      })
      .catch((err) => {
        if (err.name === 'AbortError') return; // cancelled by user
        console.error('[Chatbot SSE] Stream error:', err);
        callbacks.onError?.(err);
      });

    return controller;
  }

  /**
   * Upload book cover image for OCR extraction
   */
  extractBookInfo(image: File): Observable<any> {
    const formData = new FormData();
    formData.append('image', image);
    return this.http.post<any>(`${this.apiUrl}/ocr`, formData);
  }

  /**
   * Get AI-powered book recommendations based on user's borrowing history
   */
  getRecommendations(): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/recommend-books`, {});
  }

  /**
   * Get AI reading insight: personalized analysis of user's reading habits
   */
  getReadingInsight(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/reading-insight`);
  }

  /**
   * Delete a conversation by ID
   */
  deleteConversation(conversationId: string): Observable<any> {
    return this.http.delete<any>(
      `${this.apiUrl}/conversations/${conversationId}`,
    );
  }

  /**
   * Rename a conversation (persist title to backend)
   */
  renameConversation(conversationId: string, title: string): Observable<any> {
    return this.http.patch<any>(
      `${this.apiUrl}/conversations/${conversationId}/rename`,
      { title },
    );
  }
}
