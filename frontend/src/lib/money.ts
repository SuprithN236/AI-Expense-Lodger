import type { Money } from '../api/types';

const CURRENCY_SYMBOL = '$';
const AMOUNT_PATTERN = /^\d{1,10}(\.\d{1,2})?$/;

/** True for a positive amount with at most two decimals, e.g. "12", "12.5", "12.50". */
export function isValidAmount(input: string): boolean {
  return AMOUNT_PATTERN.test(input.trim()) && toCents(input) > 0;
}

/** Parses a decimal string into integer cents using string arithmetic only (no floats). */
export function toCents(input: string): number {
  const [whole, fraction = ''] = input.trim().split('.');
  return Number(whole) * 100 + Number(fraction.padEnd(2, '0'));
}

export function fromCents(cents: number): Money {
  const sign = cents < 0 ? '-' : '';
  const abs = Math.abs(cents);
  return `${sign}${Math.floor(abs / 100)}.${String(abs % 100).padStart(2, '0')}`;
}

/** Formats a decimal string such as "-1234.5" as "-$1,234.50" without converting to a float. */
export function formatMoney(amount: Money): string {
  const negative = amount.trim().startsWith('-');
  const unsigned = amount.trim().replace(/^[-+]/, '');
  const [whole, fraction = ''] = unsigned.split('.');
  const grouped = whole.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return `${negative ? '-' : ''}${CURRENCY_SYMBOL}${grouped}.${fraction.padEnd(2, '0').slice(0, 2)}`;
}

export function signOf(amount: Money): -1 | 0 | 1 {
  const cents = toCents(amount.replace(/^[-+]/, ''));
  if (cents === 0) return 0;
  return amount.trim().startsWith('-') ? -1 : 1;
}

/** The same cent-exact equal split the backend performs, for previewing shares in the form. */
export function previewEqualShares(amount: string, participants: number): Money[] {
  if (participants <= 0 || !isValidAmount(amount)) return [];
  const cents = toCents(amount);
  const base = Math.floor(cents / participants);
  const remainder = cents % participants;
  return Array.from({ length: participants }, (_, i) => fromCents(base + (i < remainder ? 1 : 0)));
}
