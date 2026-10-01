import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from 'react';
import { errorMessage, problemOf, statusOf } from '../api/client';
import { aiApi, ledgerApi } from '../api/endpoints';
import type { Group } from '../api/types';
import { newIdempotencyKey, todayIso } from '../lib/ids';
import { formatMoney, isValidAmount, previewEqualShares } from '../lib/money';
import { Spinner } from './Spinner';

interface ExpenseFormProps {
  group: Group;
  currentUserId: number;
  onLogged: () => void;
}

type Notice = { kind: 'success' | 'error' | 'info'; text: string } | null;

export function ExpenseForm({ group, currentUserId, onLogged }: ExpenseFormProps) {
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState(todayIso);
  const [payerId, setPayerId] = useState(currentUserId);
  const [participantIds, setParticipantIds] = useState<Set<number>>(() => new Set(group.members.map((m) => m.id)));
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isScanning, setIsScanning] = useState(false);
  const [notice, setNotice] = useState<Notice>(null);
  const [receiptItems, setReceiptItems] = useState<string[]>([]);

  // One key per logical submission. A retry of the *same* form contents reuses it so the server can
  // reject the duplicate; any edit means a new submission and therefore a new key.
  const idempotencyKey = useRef(newIdempotencyKey());
  // Synchronous guard: state updates are async, a ref flips in the same tick as the click.
  const inFlight = useRef(false);
  const fileInput = useRef<HTMLInputElement>(null);

  const memberKey = group.members.map((m) => m.id).join(',');
  useEffect(() => {
    // Group (or its membership) changed: default to splitting between everyone.
    setParticipantIds(new Set(group.members.map((m) => m.id)));
    setPayerId(group.members.some((m) => m.id === currentUserId) ? currentUserId : group.members[0]?.id ?? currentUserId);
    idempotencyKey.current = newIdempotencyKey();
  }, [group.id, memberKey, currentUserId]); // memberKey stands in for group.members

  const edited = <T,>(setter: (value: T) => void) => (value: T) => {
    idempotencyKey.current = newIdempotencyKey();
    setNotice(null);
    setter(value);
  };

  const toggleParticipant = edited((userId: number) => {
    setParticipantIds((prev) => {
      const next = new Set(prev);
      if (next.has(userId)) next.delete(userId);
      else next.add(userId);
      return next;
    });
  });

  const resetForm = () => {
    setDescription('');
    setAmount('');
    setDate(todayIso());
    setReceiptItems([]);
    idempotencyKey.current = newIdempotencyKey();
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (inFlight.current) return;
    inFlight.current = true;
    setIsSubmitting(true);
    setNotice(null);

    try {
      if (!description.trim()) throw new Error('Enter a description.');
      if (!isValidAmount(amount)) throw new Error('Enter a positive amount with at most two decimals.');
      if (participantIds.size === 0) throw new Error('Select at least one member to split with.');

      await ledgerApi.logExpense(group.id, {
        idempotencyKey: idempotencyKey.current,
        description: description.trim(),
        totalAmount: amount.trim(),
        payerId,
        date,
        participantIds: [...participantIds],
      });
      resetForm();
      setNotice({ kind: 'success', text: 'Expense logged.' });
      onLogged();
    } catch (err) {
      if (statusOf(err) === 409 && problemOf(err)?.existingTransactionId) {
        // An earlier attempt with this exact submission already succeeded (e.g. the response was lost).
        resetForm();
        setNotice({ kind: 'info', text: 'This expense was already recorded.' });
        onLogged();
      } else {
        setNotice({ kind: 'error', text: errorMessage(err) });
      }
    } finally {
      inFlight.current = false;
      setIsSubmitting(false);
    }
  };

  const handleReceipt = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    setIsScanning(true);
    setNotice(null);
    try {
      const receipt = await aiApi.scanReceipt(file);
      idempotencyKey.current = newIdempotencyKey();
      if (receipt.merchant) setDescription(receipt.merchant.slice(0, 255));
      if (receipt.totalAmount) setAmount(receipt.totalAmount);
      if (receipt.date) setDate(receipt.date);
      setReceiptItems(receipt.items);
      setNotice(
        receipt.totalAmount
          ? { kind: 'info', text: 'Receipt scanned. Review the details, then log the expense.' }
          : { kind: 'error', text: 'Could not read a total from that image. Please enter it manually.' },
      );
    } catch (err) {
      setNotice({ kind: 'error', text: errorMessage(err) });
    } finally {
      setIsScanning(false);
    }
  };

  const shares = previewEqualShares(amount, participantIds.size);
  const busy = isSubmitting || isScanning;

  return (
    <form onSubmit={handleSubmit} className="card space-y-4" aria-busy={isSubmitting}>
      <div className="flex items-center justify-between">
        <h2 className="card-title">Log an expense</h2>
        <button
          type="button"
          disabled={busy}
          onClick={() => fileInput.current?.click()}
          className="btn-secondary px-2.5 py-1 text-xs"
        >
          {isScanning && <Spinner className="h-3 w-3" />}
          {isScanning ? 'Reading receipt…' : 'Scan receipt'}
        </button>
        <input
          ref={fileInput}
          type="file"
          accept="image/jpeg,image/png,image/gif,image/webp"
          className="hidden"
          onChange={handleReceipt}
        />
      </div>

      <fieldset disabled={busy} className="space-y-4">
        <label className="block">
          <span className="text-sm font-medium text-slate-700">Description</span>
          <input
            className="input mt-1"
            value={description}
            maxLength={255}
            placeholder="e.g. Weekly groceries at Trader Joe's"
            onChange={(e) => edited(setDescription)(e.target.value)}
          />
        </label>

        <div className="grid grid-cols-2 gap-3">
          <label className="block">
            <span className="text-sm font-medium text-slate-700">Amount</span>
            <input
              className="input mt-1"
              inputMode="decimal"
              value={amount}
              placeholder="0.00"
              onChange={(e) => edited(setAmount)(e.target.value)}
            />
          </label>
          <label className="block">
            <span className="text-sm font-medium text-slate-700">Date</span>
            <input
              type="date"
              className="input mt-1"
              value={date}
              required
              onChange={(e) => edited(setDate)(e.target.value)}
            />
          </label>
        </div>

        <label className="block">
          <span className="text-sm font-medium text-slate-700">Paid by</span>
          <select
            className="input mt-1"
            value={payerId}
            onChange={(e) => edited(setPayerId)(Number(e.target.value))}
          >
            {group.members.map((member) => (
              <option key={member.id} value={member.id}>
                {member.id === currentUserId ? `${member.email} (you)` : member.email}
              </option>
            ))}
          </select>
        </label>

        <div>
          <span className="text-sm font-medium text-slate-700">Split equally between</span>
          <div className="mt-2 space-y-1.5">
            {group.members.map((member) => {
              const index = [...participantIds].sort((a, b) => a - b).indexOf(member.id);
              return (
                <label key={member.id} className="flex items-center justify-between gap-2 text-sm text-slate-700">
                  <span className="flex min-w-0 items-center gap-2">
                    <input
                      type="checkbox"
                      className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
                      checked={participantIds.has(member.id)}
                      onChange={() => toggleParticipant(member.id)}
                    />
                    <span className="truncate">{member.email}</span>
                  </span>
                  {index >= 0 && shares[index] && (
                    <span className="shrink-0 tabular-nums text-slate-500">{formatMoney(shares[index])}</span>
                  )}
                </label>
              );
            })}
          </div>
        </div>
      </fieldset>

      {receiptItems.length > 0 && (
        <p className="text-xs text-slate-500">
          Items on receipt: {receiptItems.slice(0, 8).join(', ')}
          {receiptItems.length > 8 && ` and ${receiptItems.length - 8} more`}
        </p>
      )}

      {notice && (
        <p
          role="status"
          className={`rounded-md px-3 py-2 text-sm ${
            notice.kind === 'success'
              ? 'bg-emerald-50 text-emerald-700'
              : notice.kind === 'info'
                ? 'bg-indigo-50 text-indigo-700'
                : 'bg-rose-50 text-rose-700'
          }`}
        >
          {notice.text}
        </p>
      )}

      <button type="submit" disabled={isSubmitting || isScanning} className="btn-primary w-full">
        {isSubmitting && <Spinner />}
        {isSubmitting ? 'Logging…' : 'Log Expense'}
      </button>
    </form>
  );
}
