import axios, { AxiosError } from 'axios';
import type { ProblemDetail } from './types';

/**
 * Shared HTTP client. The auth interceptors (Bearer token injection and 401 handling) are installed
 * by AuthContext, which owns the token.
 */
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  headers: { 'Content-Type': 'application/json' },
  timeout: 60_000,
});

export function problemOf(error: unknown): ProblemDetail | undefined {
  if (axios.isAxiosError(error)) {
    return (error as AxiosError<ProblemDetail>).response?.data ?? undefined;
  }
  return undefined;
}

export function statusOf(error: unknown): number | undefined {
  return axios.isAxiosError(error) ? error.response?.status : undefined;
}

/** A human-readable message for any API or network failure. */
export function errorMessage(error: unknown): string {
  const problem = problemOf(error);
  if (problem?.detail) return problem.detail;
  if (axios.isAxiosError(error)) {
    if (error.code === 'ECONNABORTED') return 'The request timed out. Please try again.';
    if (!error.response) return 'Cannot reach the server. Check your connection and try again.';
    return `Request failed with status ${error.response.status}`;
  }
  return error instanceof Error ? error.message : 'Something went wrong';
}
