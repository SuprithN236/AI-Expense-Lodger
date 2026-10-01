import { useState } from 'react';
import { errorMessage, statusOf } from '../api/client';
import { ledgerApi } from '../api/endpoints';
import type { Transaction } from '../api/types';
import { newIdempotencyKey } from '../lib/ids';
import { formatMoney } from '../lib/money';
import { Spinner } from './Spinner';

interface LedgerTableProps {
  groupId: number;
  transactions: Transaction[];
  onChanged: () => void;
}

export function LedgerTable({ groupId, transactions, onChanged }: LedgerTableProps) {
  const [reversingId, setReversingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reverse = async (transaction: Transaction) => {
    if (reversingId !== null) return;
    const confirmed = window.confirm(
      `Reverse "${transaction.description}" (${formatMoney(transaction.totalAmount)})?\n\n` +
        'The original entry stays in the ledger; a cancelling entry is added.',
    );
    if (!confirmed) return;

    setReversingId(transaction.id);
    setError(null);
    try {
      await ledgerApi.reverse(groupId, transaction.id, newIdempotencyKey());
      onChanged();
    } catch (err) {
      if (statusOf(err) === 409) onChanged();
      setError(errorMessage(err));
    } finally {
      setReversingId(null);
    }
  };

  return (
    <section className="card">
      <div className="flex items-baseline justify-between">
        <h2 className="card-title">Ledger</h2>
        <span className="text-xs text-slate-500">Append-only · {transactions.length} entries</span>
      </div>
      {error && <p className="mt-3 rounded-md bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p>}

      {transactions.length === 0 ? (
        <p className="mt-4 text-sm text-slate-500">No expenses yet. Log the first one on the left.</p>
      ) : (
        <div className="mt-4 max-h-[28rem] overflow-auto rounded-xl border border-slate-200">
          <table className="min-w-full text-sm">
            <thead className="sticky top-0 bg-slate-50 text-left text-xs font-medium uppercase tracking-wide text-slate-500">
              <tr>
                <th className="px-3 py-2">#</th>
                <th className="px-3 py-2">Date</th>
                <th className="px-3 py-2">Description</th>
                <th className="px-3 py-2">Paid by</th>
                <th className="px-3 py-2">Split</th>
                <th className="px-3 py-2 text-right">Amount</th>
                <th className="px-3 py-2" aria-label="Actions" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {transactions.map((t) => {
                const isReversal = t.reversesTransactionId !== null;
                const isReversed = t.reversedByTransactionId !== null;
                return (
                  <tr key={t.id} className={isReversal ? 'bg-amber-50/50' : undefined}>
                    <td className="px-3 py-2 tabular-nums text-slate-400">{t.id}</td>
                    <td className="whitespace-nowrap px-3 py-2 tabular-nums text-slate-600">{t.date}</td>
                    <td className="px-3 py-2">
                      <span className={isReversed ? 'text-slate-400 line-through' : 'text-slate-800'}>
                        {t.description}
                      </span>
                      {isReversal && (
                        <span className="ml-2 rounded bg-amber-100 px-1.5 py-0.5 text-xs text-amber-800">
                          reverses #{t.reversesTransactionId}
                        </span>
                      )}
                      {isReversed && (
                        <span className="ml-2 rounded bg-slate-100 px-1.5 py-0.5 text-xs text-slate-600">
                          reversed by #{t.reversedByTransactionId}
                        </span>
                      )}
                    </td>
                    <td className="max-w-[10rem] truncate px-3 py-2 text-slate-600" title={t.payerEmail}>
                      {t.payerEmail}
                    </td>
                    <td className="px-3 py-2 text-xs text-slate-500">
                      <span
                        title={t.splits.map((s) => `${s.email}: ${formatMoney(s.owedAmount)}`).join('\n')}
                        className="cursor-help underline decoration-dotted"
                      >
                        {t.splits.length} {t.splits.length === 1 ? 'person' : 'people'}
                      </span>
                    </td>
                    <td
                      className={`whitespace-nowrap px-3 py-2 text-right font-medium tabular-nums ${
                        isReversal ? 'text-amber-700' : 'text-slate-900'
                      }`}
                    >
                      {formatMoney(t.totalAmount)}
                    </td>
                    <td className="px-3 py-2 text-right">
                      {!isReversal && !isReversed && (
                        <button
                          type="button"
                          disabled={reversingId !== null}
                          onClick={() => void reverse(t)}
                          className="inline-flex items-center gap-1 text-xs font-medium text-slate-500 hover:text-rose-600 disabled:opacity-40"
                        >
                          {reversingId === t.id && <Spinner className="h-3 w-3" />}
                          Reverse
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
