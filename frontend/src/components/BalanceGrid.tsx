import type { BalanceSheet } from '../api/types';
import { formatMoney, signOf } from '../lib/money';

interface BalanceGridProps {
  sheet: BalanceSheet;
  currentUserId: number;
}

function label(email: string, userId: number, currentUserId: number) {
  return userId === currentUserId ? 'You' : email;
}

export function BalanceGrid({ sheet, currentUserId }: BalanceGridProps) {
  return (
    <section className="card">
      <div className="flex items-baseline justify-between">
        <h2 className="card-title">Balances</h2>
        <span className="text-sm text-slate-500">
          Total spent <span className="font-semibold tabular-nums text-slate-900">{formatMoney(sheet.totalSpent)}</span>
        </span>
      </div>

      <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
        {sheet.balances.map((balance) => {
          const sign = signOf(balance.netBalance);
          const tone =
            sign > 0
              ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
              : sign < 0
                ? 'border-rose-200 bg-rose-50 text-rose-700'
                : 'border-slate-200 bg-slate-50 text-slate-500';
          return (
            <div key={balance.userId} className={`rounded-xl border p-3 ${tone}`}>
              <p className="truncate text-sm font-medium text-slate-700" title={balance.email}>
                {label(balance.email, balance.userId, currentUserId)}
              </p>
              <p className="mt-1 text-lg font-semibold tabular-nums">{formatMoney(balance.netBalance)}</p>
              <p className="text-xs">{sign > 0 ? 'is owed' : sign < 0 ? 'owes' : 'settled up'}</p>
            </div>
          );
        })}
      </div>

      <h3 className="mt-5 text-sm font-medium text-slate-700">Who owes who</h3>
      {sheet.settlements.length === 0 ? (
        <p className="mt-2 text-sm text-slate-500">Everyone is settled up.</p>
      ) : (
        <ul className="mt-2 divide-y divide-slate-100 rounded-xl border border-slate-200">
          {sheet.settlements.map((s) => (
            <li key={`${s.fromUserId}-${s.toUserId}`} className="flex items-center justify-between gap-3 px-3 py-2 text-sm">
              <span className="min-w-0 truncate text-slate-700">
                <span className="font-medium">{label(s.fromEmail, s.fromUserId, currentUserId)}</span>
                {s.fromUserId === currentUserId ? ' owe ' : ' owes '}
                <span className="font-medium">{label(s.toEmail, s.toUserId, currentUserId)}</span>
              </span>
              <span className="shrink-0 font-semibold tabular-nums text-slate-900">{formatMoney(s.amount)}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
