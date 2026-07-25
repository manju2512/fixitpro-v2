import { useEffect, useRef, useState } from 'react';
import { chatApiClient, getApiErrorMessage } from '../api/client';

type ChatMessage = { role: 'user' | 'assistant'; content: string };

type ChatResponse = { reply: string; messages: ChatMessage[] };

/** Floating chat launcher + panel, wired to ai-chat-service (POST /chat/message). */
export function ChatWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [draft, setDraft] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight });
  }, [messages, isOpen]);

  async function sendMessage() {
    const content = draft.trim();
    if (!content || isSending) return;

    const nextMessages: ChatMessage[] = [...messages, { role: 'user', content }];
    setMessages(nextMessages);
    setDraft('');
    setError(null);
    setIsSending(true);

    try {
      const { data } = await chatApiClient.post<ChatResponse>('/chat/message', {
        messages: nextMessages,
      });
      setMessages(data.messages ?? [...nextMessages, { role: 'assistant', content: data.reply }]);
    } catch (err) {
      setError(getApiErrorMessage(err));
      // Roll back the optimistic user message so a retry doesn't duplicate it.
      setMessages(messages);
      setDraft(content);
    } finally {
      setIsSending(false);
    }
  }

  function handleKeyDown(event: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      sendMessage();
    }
  }

  return (
    <div className="fixed bottom-5 right-5 z-50 flex flex-col items-end">
      {isOpen && (
        <div className="mb-3 flex h-96 w-80 flex-col overflow-hidden rounded-lg border border-line bg-paper-raised shadow-lg">
          <div className="flex items-center justify-between border-b border-line px-4 py-3">
            <span className="font-display text-sm font-semibold text-ink">FixitPro Assistant</span>
            <button
              onClick={() => setIsOpen(false)}
              aria-label="Close chat"
              className="rounded-md px-2 py-1 text-ink-soft transition-colors hover:bg-ink/5 hover:text-ink"
            >
              ✕
            </button>
          </div>

          <div ref={scrollRef} className="flex-1 space-y-2 overflow-y-auto px-4 py-3">
            {messages.length === 0 && (
              <p className="font-utility text-xs text-ink-soft">
                Ask about booking a service, tracking a job, or anything else.
              </p>
            )}
            {messages.map((message, index) => (
              <div
                key={index}
                className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}
              >
                <p
                  className={`max-w-[85%] rounded-lg px-3 py-2 text-sm ${
                    message.role === 'user'
                      ? 'bg-signal text-signal-ink'
                      : 'bg-ink/5 text-ink'
                  }`}
                >
                  {message.content}
                </p>
              </div>
            ))}
            {isSending && (
              <p className="font-utility text-xs text-ink-soft">Thinking…</p>
            )}
          </div>

          {error && (
            <p className="border-t border-line px-4 py-2 font-utility text-xs text-rust">{error}</p>
          )}

          <div className="flex items-end gap-2 border-t border-line px-3 py-2">
            <textarea
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              onKeyDown={handleKeyDown}
              rows={1}
              placeholder="Type a message…"
              className="flex-1 resize-none rounded-md border border-line bg-paper px-2 py-1.5 text-sm text-ink outline-none focus:border-steel"
            />
            <button
              onClick={sendMessage}
              disabled={isSending || !draft.trim()}
              className="rounded-md bg-signal px-3 py-1.5 text-sm font-medium text-signal-ink transition-opacity hover:opacity-90 disabled:opacity-40"
            >
              Send
            </button>
          </div>
        </div>
      )}

      <button
        onClick={() => setIsOpen((open) => !open)}
        aria-label={isOpen ? 'Close chat' : 'Open chat'}
        className="flex h-12 w-12 items-center justify-center rounded-full bg-signal text-signal-ink shadow-lg transition-opacity hover:opacity-90"
      >
        {isOpen ? '✕' : '💬'}
      </button>
    </div>
  );
}
