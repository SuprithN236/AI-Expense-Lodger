import { useState, type FormEvent } from 'react';
import { errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Spinner } from './Spinner';

type Mode = 'login' | 'signup';

export function AuthScreen() {
  const { login, signup } = useAuth();
  const [mode, setMode] = useState<Mode>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (isSubmitting) return;
    setIsSubmitting(true);
    setError(null);
    try {
      await (mode === 'login' ? login(email, password) : signup(email, password));
    } catch (err) {
      setError(errorMessage(err));
      setIsSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-sm rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
        <h1 className="text-xl font-semibold text-slate-900">AI Expense Ledger</h1>
        <p className="mt-1 text-sm text-slate-500">Shared expenses on an append-only ledger.</p>

        <div className="mt-6 grid grid-cols-2 rounded-lg bg-slate-100 p-1 text-sm font-medium">
          {(['login', 'signup'] as const).map((m) => (
            <button
              key={m}
              type="button"
              disabled={isSubmitting}
              onClick={() => {
                setMode(m);
                setError(null);
              }}
              className={`rounded-md py-1.5 transition ${
                mode === m ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-700'
              }`}
            >
              {m === 'login' ? 'Log in' : 'Sign up'}
            </button>
          ))}
        </div>

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          <fieldset disabled={isSubmitting} className="space-y-4">
            <label className="block">
              <span className="text-sm font-medium text-slate-700">Email</span>
              <input
                type="email"
                required
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="input mt-1"
              />
            </label>
            <label className="block">
              <span className="text-sm font-medium text-slate-700">Password</span>
              <input
                type="password"
                required
                minLength={mode === 'signup' ? 8 : undefined}
                maxLength={72}
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="input mt-1"
              />
              {mode === 'signup' && <span className="mt-1 block text-xs text-slate-500">At least 8 characters.</span>}
            </label>
          </fieldset>

          {error && <p className="rounded-md bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p>}

          <button type="submit" disabled={isSubmitting} className="btn-primary w-full">
            {isSubmitting && <Spinner />}
            {mode === 'login' ? 'Log in' : 'Create account'}
          </button>
        </form>
      </div>
    </div>
  );
}
