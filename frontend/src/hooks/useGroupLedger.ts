import { useCallback, useEffect, useRef, useState } from 'react';
import { errorMessage } from '../api/client';
import { groupApi, ledgerApi } from '../api/endpoints';
import type { BalanceSheet, Group, Transaction } from '../api/types';

interface GroupLedgerState {
  group: Group | null;
  balances: BalanceSheet | null;
  transactions: Transaction[];
  loading: boolean;
  error: string | null;
}

const EMPTY: GroupLedgerState = { group: null, balances: null, transactions: [], loading: false, error: null };

/** Loads a group's members, derived balances and ledger rows together, ignoring stale responses. */
export function useGroupLedger(groupId: number | null) {
  const [state, setState] = useState<GroupLedgerState>(EMPTY);
  const requestSeq = useRef(0);

  const reload = useCallback(async () => {
    if (groupId === null) {
      setState(EMPTY);
      return;
    }
    const seq = ++requestSeq.current;
    setState((prev) => ({ ...prev, loading: true, error: null }));
    try {
      const [group, balances, transactions] = await Promise.all([
        groupApi.get(groupId),
        ledgerApi.balances(groupId),
        ledgerApi.transactions(groupId),
      ]);
      if (seq === requestSeq.current) {
        setState({ group, balances, transactions, loading: false, error: null });
      }
    } catch (error) {
      if (seq === requestSeq.current) {
        setState((prev) => ({ ...prev, loading: false, error: errorMessage(error) }));
      }
    }
  }, [groupId]);

  useEffect(() => {
    setState(EMPTY);
    void reload();
  }, [reload]);

  return { ...state, reload };
}
