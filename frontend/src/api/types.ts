// Monetary values are decimal strings (e.g. "12.50"), exactly as the API sends them,
// so they never pass through binary floating point.
export type Money = string;

export interface User {
  id: number;
  email: string;
}

export interface AuthResponse {
  token: string;
  tokenType: 'Bearer';
  expiresInSeconds: number;
  user: User;
}

export interface Group {
  id: number;
  name: string;
  members: User[];
}

export interface Split {
  userId: number;
  email: string;
  owedAmount: Money;
}

export interface Transaction {
  id: number;
  groupId: number;
  description: string;
  totalAmount: Money;
  payerId: number;
  payerEmail: string;
  date: string;
  createdBy: number;
  createdAt: string;
  reversesTransactionId: number | null;
  reversedByTransactionId: number | null;
  splits: Split[];
}

export interface MemberBalance {
  userId: number;
  email: string;
  netBalance: Money;
}

export interface Settlement {
  fromUserId: number;
  fromEmail: string;
  toUserId: number;
  toEmail: string;
  amount: Money;
}

export interface BalanceSheet {
  groupId: number;
  totalSpent: Money;
  balances: MemberBalance[];
  settlements: Settlement[];
}

export interface LogExpensePayload {
  idempotencyKey: string;
  description: string;
  totalAmount: Money;
  payerId: number;
  date: string;
  participantIds: number[];
}

export interface ReceiptData {
  merchant: string | null;
  totalAmount: Money | null;
  items: string[];
  date: string | null;
}

/** RFC 9457 problem details returned by the backend on every error. */
export interface ProblemDetail {
  status: number;
  title?: string;
  detail?: string;
  existingTransactionId?: number;
}
