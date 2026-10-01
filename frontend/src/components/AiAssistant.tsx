import { useEffect, useRef, useState, type FormEvent } from 'react';
import { errorMessage } from '../api/client';
import { aiApi } from '../api/endpoints';
import { Spinner } from './Spinner';

interface Message {
  id: number;
  role: 'user' | 'assistant' | 'error';
  text: string;
}

const PROMPT_LABEL = 'Ask the AI Assistant about spending history...';
const EXAMPLES = ['How much did we spend on groceries last week?', 'Who owes the most right now?'];

export function AiAssistant({ groupId }: { groupId: number }) {
  const [question, setQuestion] = useState('');
  const [messages, setMessages] = useState<Message[]>([]);
  const [isAsking, setIsAsking] = useState(false);
  const nextId = useRef(0);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setMessages([]);
  }, [groupId]);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, isAsking]);

  const append = (role: Message['role'], text: string) =>
    setMessages((prev) => [...prev, { id: nextId.current++, role, text }]);

  const ask = async (text: string) => {
    const queryText = text.trim();
    if (!queryText || isAsking) return;
    setIsAsking(true);
    setQuestion('');
    append('user', queryText);
    try {
      append('assistant', await aiApi.query(groupId, queryText));
    } catch (err) {
      append('error', errorMessage(err));
    } finally {
      setIsAsking(false);
    }
  };

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    void ask(question);
  };

  return (
    <section className="card flex flex-col">
      <h2 className="card-title">AI Assistant</h2>

      <div ref={scrollRef} className="mt-3 max-h-72 min-h-[6rem] space-y-2 overflow-y-auto">
        {messages.length === 0 && !isAsking && (
          <div className="space-y-2 text-sm text-slate-500">
            <p>The assistant reads this group's ledger through read-only tools. It cannot change any entries.</p>
            <div className="flex flex-wrap gap-2">
              {EXAMPLES.map((example) => (
                <button
                  key={example}
                  type="button"
                  onClick={() => void ask(example)}
                  className="rounded-full border border-slate-200 px-3 py-1 text-xs text-slate-600 hover:bg-slate-50"
                >
                  {example}
                </button>
              ))}
            </div>
          </div>
        )}
        {messages.map((message) => (
          <div
            key={message.id}
            className={`whitespace-pre-wrap rounded-xl px-3 py-2 text-sm ${
              message.role === 'user'
                ? 'ml-8 bg-indigo-600 text-white'
                : message.role === 'assistant'
                  ? 'mr-8 bg-slate-100 text-slate-800'
                  : 'mr-8 bg-rose-50 text-rose-700'
            }`}
          >
            {message.text}
          </div>
        ))}
        {isAsking && (
          <div className="mr-8 flex items-center gap-2 rounded-xl bg-slate-100 px-3 py-2 text-sm text-slate-500">
            <Spinner /> Checking the ledger…
          </div>
        )}
      </div>

      <form onSubmit={handleSubmit} className="mt-3 flex gap-2">
        <label htmlFor="ai-question" className="sr-only">
          {PROMPT_LABEL}
        </label>
        <input
          id="ai-question"
          className="input"
          placeholder={PROMPT_LABEL}
          value={question}
          maxLength={500}
          disabled={isAsking}
          onChange={(e) => setQuestion(e.target.value)}
        />
        <button type="submit" disabled={isAsking || !question.trim()} className="btn-primary shrink-0">
          {isAsking ? <Spinner /> : 'Ask'}
        </button>
      </form>
    </section>
  );
}
