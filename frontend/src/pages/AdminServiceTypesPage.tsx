import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../api/admin';
import { getApiErrorMessage } from '../api/client';
import type { ServiceType } from '../types';

type FormState = { name: string; description: string; basePrice: string };
const EMPTY_FORM: FormState = { name: '', description: '', basePrice: '' };

function ServiceTypeForm({
  initial,
  submitLabel,
  onSubmit,
  onCancel,
  submitting,
  error,
}: {
  initial: FormState;
  submitLabel: string;
  onSubmit: (values: { name: string; description: string; basePrice: number }) => void;
  onCancel?: () => void;
  submitting: boolean;
  error: string | null;
}) {
  const [form, setForm] = useState(initial);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const basePrice = Number(form.basePrice);
    if (!form.name.trim() || Number.isNaN(basePrice) || basePrice < 0) return;
    onSubmit({ name: form.name.trim(), description: form.description.trim(), basePrice });
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-3 sm:flex-row sm:items-start">
      <input
        className="flex-1 rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
        placeholder="Name (e.g. Housekeeping)"
        value={form.name}
        onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
        required
      />
      <input
        className="flex-[2] rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
        placeholder="Short description"
        value={form.description}
        onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
      />
      <input
        type="number"
        min="0"
        step="1"
        className="w-32 rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
        placeholder="Base price (₹)"
        value={form.basePrice}
        onChange={(e) => setForm((f) => ({ ...f, basePrice: e.target.value }))}
        required
      />
      <div className="flex gap-2">
        <button
          type="submit"
          disabled={submitting}
          className="rounded-md bg-signal px-4 py-2 text-sm font-semibold text-signal-ink transition-opacity hover:opacity-90 disabled:opacity-50"
        >
          {submitting ? 'Saving…' : submitLabel}
        </button>
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            className="rounded-md border border-line px-4 py-2 text-sm font-medium text-ink-soft hover:text-ink"
          >
            Cancel
          </button>
        )}
      </div>
      {error && <p className="w-full text-sm text-rust">{error}</p>}
    </form>
  );
}

function ServiceTypeRow({ serviceType }: { serviceType: ServiceType }) {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);

  const updateMutation = useMutation({
    mutationFn: (params: { name: string; description: string; basePrice: number }) =>
      adminApi.updateServiceType(serviceType.serviceTypeId, params),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-service-types'] });
      queryClient.invalidateQueries({ queryKey: ['service-types'] }); // homepage's public listing
      setEditing(false);
    },
  });

  const toggleMutation = useMutation({
    mutationFn: (active: boolean) => adminApi.setServiceTypeActive(serviceType.serviceTypeId, active),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-service-types'] });
      queryClient.invalidateQueries({ queryKey: ['service-types'] });
    },
  });

  if (editing) {
    return (
      <div className="rounded-lg border border-line bg-paper-raised p-4">
        <ServiceTypeForm
          initial={{
            name: serviceType.name,
            description: serviceType.description,
            basePrice: String(serviceType.basePrice),
          }}
          submitLabel="Save"
          submitting={updateMutation.isPending}
          error={updateMutation.isError ? getApiErrorMessage(updateMutation.error) : null}
          onSubmit={(values) => updateMutation.mutate(values)}
          onCancel={() => setEditing(false)}
        />
      </div>
    );
  }

  return (
    <div className="flex items-center justify-between gap-4 rounded-lg border border-line bg-paper-raised px-4 py-3">
      <div className="min-w-0 flex-1">
        <p className="flex items-center gap-2 font-medium">
          {serviceType.name}
          {!serviceType.active && (
            <span className="rounded-full bg-line px-2 py-0.5 font-utility text-xs text-ink-soft">Inactive</span>
          )}
        </p>
        <p className="truncate text-sm text-ink-soft">{serviceType.description || 'No description'}</p>
      </div>
      <span className="font-utility text-sm text-ink-soft">₹{Math.round(serviceType.basePrice)}</span>
      <div className="flex shrink-0 gap-2">
        <button
          onClick={() => setEditing(true)}
          className="rounded-md px-3 py-1.5 text-xs font-medium text-steel underline underline-offset-2"
        >
          Edit
        </button>
        <button
          onClick={() => toggleMutation.mutate(!serviceType.active)}
          disabled={toggleMutation.isPending}
          className={`rounded-md px-3 py-1.5 text-xs font-medium transition-colors disabled:opacity-40 ${
            serviceType.active ? 'bg-rust/15 text-rust hover:bg-rust/25' : 'bg-moss/15 text-moss hover:bg-moss/25'
          }`}
        >
          {serviceType.active ? 'Deactivate' : 'Reactivate'}
        </button>
      </div>
    </div>
  );
}

export function AdminServiceTypesPage() {
  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: ['admin-service-types'], queryFn: adminApi.listAllServiceTypes });

  const createMutation = useMutation({
    mutationFn: (params: { name: string; description: string; basePrice: number }) =>
      adminApi.createServiceType(params),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-service-types'] });
      queryClient.invalidateQueries({ queryKey: ['service-types'] });
    },
  });

  return (
    <div className="max-w-3xl">
      <h1 className="font-display text-2xl font-semibold tracking-tight">Services</h1>
      <p className="mt-1 text-sm text-ink-soft">
        The trades FixitPro offers. Add a new one any time — customers will see it on the homepage
        immediately, no deployment needed.
      </p>

      <div className="mt-6 rounded-lg border border-dashed border-line bg-paper-raised p-4">
        <p className="mb-3 font-utility text-xs font-medium uppercase tracking-widest text-ink-soft">
          Add a service
        </p>
        <ServiceTypeForm
          initial={EMPTY_FORM}
          submitLabel="Add service"
          submitting={createMutation.isPending}
          error={createMutation.isError ? getApiErrorMessage(createMutation.error) : null}
          onSubmit={(values) => createMutation.mutate(values)}
        />
      </div>

      {query.isLoading && <p className="mt-6 text-sm text-ink-soft">Loading services…</p>}
      {query.isError && <p className="mt-6 text-sm text-rust">{getApiErrorMessage(query.error)}</p>}

      <div className="mt-6 flex flex-col gap-2">
        {query.data?.map((st) => (
          <ServiceTypeRow key={st.serviceTypeId} serviceType={st} />
        ))}
        {query.data?.length === 0 && <p className="text-sm text-ink-soft">No service types yet.</p>}
      </div>
    </div>
  );
}
