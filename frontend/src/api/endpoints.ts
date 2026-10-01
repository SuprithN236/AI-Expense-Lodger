import { api } from './client';
import type {
  AuthResponse,
  BalanceSheet,
  Group,
  LogExpensePayload,
  ReceiptData,
  Transaction,
} from './types';

export const authApi = {
  login: (email: string, password: string) =>
    api.post<AuthResponse>('/api/v1/auth/login', { email, password }).then((r) => r.data),
  signup: (email: string, password: string) =>
    api.post<AuthResponse>('/api/v1/auth/signup', { email, password }).then((r) => r.data),
};

export const groupApi = {
  list: () => api.get<Group[]>('/api/v1/groups').then((r) => r.data),
  get: (groupId: number) => api.get<Group>(`/api/v1/groups/${groupId}`).then((r) => r.data),
  create: (name: string) => api.post<Group>('/api/v1/groups', { name }).then((r) => r.data),
  addMember: (groupId: number, email: string) =>
    api.post<Group>(`/api/v1/groups/${groupId}/members`, { email }).then((r) => r.data),
};

export const ledgerApi = {
  logExpense: (groupId: number, payload: LogExpensePayload) =>
    api.post<Transaction>(`/api/v1/groups/${groupId}/expenses`, payload).then((r) => r.data),
  transactions: (groupId: number, limit = 200) =>
    api
      .get<Transaction[]>(`/api/v1/groups/${groupId}/transactions`, { params: { limit } })
      .then((r) => r.data),
  balances: (groupId: number) =>
    api.get<BalanceSheet>(`/api/v1/groups/${groupId}/balances`).then((r) => r.data),
  reverse: (groupId: number, transactionId: number, idempotencyKey: string) =>
    api
      .post<Transaction>(`/api/v1/groups/${groupId}/transactions/${transactionId}/reversal`, {
        idempotencyKey,
      })
      .then((r) => r.data),
};

export const aiApi = {
  query: (groupId: number, queryText: string) =>
    api.post<{ answer: string }>('/api/v1/ai/query', { groupId, queryText }).then((r) => r.data.answer),
  scanReceipt: (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api
      .post<ReceiptData>('/api/v1/ai/receipt', form, { headers: { 'Content-Type': 'multipart/form-data' } })
      .then((r) => r.data);
  },
};
