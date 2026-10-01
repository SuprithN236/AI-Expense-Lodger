import { useState, type FormEvent } from 'react';
import { errorMessage } from '../api/client';
import { groupApi } from '../api/endpoints';
import type { Group } from '../api/types';
import { Spinner } from './Spinner';

interface GroupToolbarProps {
  groups: Group[];
  selectedGroupId: number | null;
  onSelect: (groupId: number) => void;
  onGroupCreated: (group: Group) => void;
  onMemberAdded: () => void;
}

type Panel = 'none' | 'create' | 'invite';

export function GroupToolbar({ groups, selectedGroupId, onSelect, onGroupCreated, onMemberAdded }: GroupToolbarProps) {
  const [panel, setPanel] = useState<Panel>(groups.length === 0 ? 'create' : 'none');
  const [value, setValue] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const open = (next: Panel) => {
    setPanel((current) => (current === next ? 'none' : next));
    setValue('');
    setError(null);
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (isSubmitting || !value.trim()) return;
    setIsSubmitting(true);
    setError(null);
    try {
      if (panel === 'create') {
        onGroupCreated(await groupApi.create(value.trim()));
      } else if (panel === 'invite' && selectedGroupId !== null) {
        await groupApi.addMember(selectedGroupId, value.trim());
        onMemberAdded();
      }
      setValue('');
      setPanel('none');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-center gap-2">
        {groups.length > 0 && (
          <select
            aria-label="Group"
            className="input w-auto min-w-[12rem]"
            value={selectedGroupId ?? ''}
            onChange={(e) => onSelect(Number(e.target.value))}
          >
            {groups.map((g) => (
              <option key={g.id} value={g.id}>
                {g.name} ({g.members.length})
              </option>
            ))}
          </select>
        )}
        <button type="button" className="btn-secondary" onClick={() => open('create')}>
          New group
        </button>
        {selectedGroupId !== null && (
          <button type="button" className="btn-secondary" onClick={() => open('invite')}>
            Add member
          </button>
        )}
      </div>

      {panel !== 'none' && (
        <form onSubmit={handleSubmit} className="flex flex-wrap items-center gap-2">
          <input
            autoFocus
            className="input w-72 max-w-full"
            type={panel === 'invite' ? 'email' : 'text'}
            maxLength={panel === 'create' ? 120 : 255}
            placeholder={panel === 'create' ? 'Group name, e.g. Apartment 4B' : "Member's registered email"}
            value={value}
            disabled={isSubmitting}
            onChange={(e) => setValue(e.target.value)}
          />
          <button type="submit" className="btn-primary" disabled={isSubmitting || !value.trim()}>
            {isSubmitting && <Spinner />}
            {panel === 'create' ? 'Create' : 'Add'}
          </button>
          {error && <p className="w-full text-sm text-rose-600">{error}</p>}
        </form>
      )}
    </div>
  );
}
