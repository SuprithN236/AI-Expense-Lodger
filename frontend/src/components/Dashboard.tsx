import { useCallback, useEffect, useState } from 'react';
import { errorMessage } from '../api/client';
import { groupApi } from '../api/endpoints';
import type { Group } from '../api/types';
import { useAuth } from '../context/AuthContext';
import { useGroupLedger } from '../hooks/useGroupLedger';
import { AiAssistant } from './AiAssistant';
import { BalanceGrid } from './BalanceGrid';
import { ExpenseForm } from './ExpenseForm';
import { GroupToolbar } from './GroupToolbar';
import { LedgerTable } from './LedgerTable';
import { Spinner } from './Spinner';

const SELECTED_GROUP_KEY = 'ai-expense-ledger.selectedGroup';

function readSelectedGroup(): number | null {
  try {
    const stored = localStorage.getItem(SELECTED_GROUP_KEY);
    return stored ? Number(stored) : null;
  } catch {
    return null;
  }
}

function rememberSelectedGroup(groupId: number) {
  try {
    localStorage.setItem(SELECTED_GROUP_KEY, String(groupId));
  } catch {
    // Non-essential convenience; ignore storage failures.
  }
}

export function Dashboard() {
  const { user, logout } = useAuth();
  const [groups, setGroups] = useState<Group[] | null>(null);
  const [groupsError, setGroupsError] = useState<string | null>(null);
  const [selectedGroupId, setSelectedGroupId] = useState<number | null>(readSelectedGroup);
  const ledger = useGroupLedger(selectedGroupId);

  const loadGroups = useCallback(async () => {
    try {
      const loaded = await groupApi.list();
      setGroups(loaded);
      setGroupsError(null);
      setSelectedGroupId((current) =>
        current !== null && loaded.some((g) => g.id === current) ? current : loaded[0]?.id ?? null,
      );
    } catch (err) {
      setGroupsError(errorMessage(err));
    }
  }, []);

  useEffect(() => {
    void loadGroups();
  }, [loadGroups]);

  const selectGroup = (groupId: number) => {
    rememberSelectedGroup(groupId);
    setSelectedGroupId(groupId);
  };

  const handleGroupCreated = (group: Group) => {
    setGroups((prev) => [...(prev ?? []), group].sort((a, b) => a.name.localeCompare(b.name)));
    selectGroup(group.id);
  };

  const handleMemberAdded = () => {
    void loadGroups();
    void ledger.reload();
  };

  if (!user) return null;

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4 px-4 py-4 sm:px-6">
          <div>
            <h1 className="text-lg font-semibold text-slate-900">AI Expense Ledger</h1>
            <p className="text-xs text-slate-500">Balances are derived from an append-only ledger.</p>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <span className="hidden text-slate-600 sm:inline">{user.email}</span>
            <button type="button" className="btn-secondary" onClick={logout}>
              Log out
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl space-y-6 px-4 py-6 sm:px-6">
        {groupsError && <p className="rounded-md bg-rose-50 px-3 py-2 text-sm text-rose-700">{groupsError}</p>}

        {groups === null ? (
          !groupsError && (
            <div className="flex items-center gap-2 text-sm text-slate-500">
              <Spinner /> Loading groups…
            </div>
          )
        ) : (
          <>
            <GroupToolbar
              groups={groups}
              selectedGroupId={selectedGroupId}
              onSelect={selectGroup}
              onGroupCreated={handleGroupCreated}
              onMemberAdded={handleMemberAdded}
            />

            {groups.length === 0 && (
              <div className="card text-sm text-slate-600">
                Create your first group to start logging shared expenses. Members must sign up before they can be
                added.
              </div>
            )}

            {ledger.error && (
              <p className="rounded-md bg-rose-50 px-3 py-2 text-sm text-rose-700">{ledger.error}</p>
            )}

            {ledger.group && ledger.balances && (
              <div className="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]">
                <div className="space-y-6">
                  <ExpenseForm group={ledger.group} currentUserId={user.id} onLogged={() => void ledger.reload()} />
                  <AiAssistant groupId={ledger.group.id} />
                </div>
                <div className="space-y-6">
                  <BalanceGrid sheet={ledger.balances} currentUserId={user.id} />
                  <LedgerTable
                    groupId={ledger.group.id}
                    transactions={ledger.transactions}
                    onChanged={() => void ledger.reload()}
                  />
                </div>
              </div>
            )}

            {ledger.loading && !ledger.group && (
              <div className="flex items-center gap-2 text-sm text-slate-500">
                <Spinner /> Loading ledger…
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
}
