import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../api/admin';
import { catalogApi } from '../api/catalog';
import { getApiErrorMessage } from '../api/client';
import type { ServiceType, Technician } from '../types';

function CreateTechnicianForm({ serviceTypes, onDone }: { serviceTypes: ServiceType[]; onDone: () => void }) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState({
    username: '',
    email: '',
    password: '',
    phone: '',
    serviceTypeId: serviceTypes[0]?.serviceTypeId ?? 0,
    bio: '',
    yearsExperience: 0,
  });

  const mutation = useMutation({
    mutationFn: () => adminApi.createTechnician(form),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-technicians'] });
      onDone();
    },
  });

  function update<K extends keyof typeof form>(key: K, value: (typeof form)[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        mutation.mutate();
      }}
      className="mt-4 flex flex-col gap-3 rounded-lg border border-line bg-paper-raised p-4"
    >
      <div className="grid grid-cols-2 gap-3">
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Username</span>
          <input
            required
            value={form.username}
            onChange={(e) => update('username', e.target.value)}
            className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
          />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Email</span>
          <input
            type="email"
            required
            value={form.email}
            onChange={(e) => update('email', e.target.value)}
            className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
          />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Temporary password</span>
          <input
            type="password"
            required
            minLength={8}
            value={form.password}
            onChange={(e) => update('password', e.target.value)}
            className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
          />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Phone</span>
          <input
            value={form.phone}
            onChange={(e) => update('phone', e.target.value)}
            className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
          />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Service type</span>
          <select
            value={form.serviceTypeId}
            onChange={(e) => update('serviceTypeId', Number(e.target.value))}
            className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
          >
            {serviceTypes.map((st) => (
              <option key={st.serviceTypeId} value={st.serviceTypeId}>
                {st.name}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Years of experience</span>
          <input
            type="number"
            min={0}
            value={form.yearsExperience}
            onChange={(e) => update('yearsExperience', Number(e.target.value))}
            className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
          />
        </label>
      </div>
      <label className="flex flex-col gap-1">
        <span className="text-sm font-medium">Bio</span>
        <textarea
          value={form.bio}
          onChange={(e) => update('bio', e.target.value)}
          rows={2}
          className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
        />
      </label>

      {mutation.isError && <p className="text-sm text-rust">{getApiErrorMessage(mutation.error)}</p>}

      <div className="flex gap-3">
        <button
          type="submit"
          disabled={mutation.isPending}
          className="rounded-md bg-signal px-4 py-2 text-sm font-semibold text-signal-ink transition-opacity hover:opacity-90 disabled:opacity-50"
        >
          {mutation.isPending ? 'Creating…' : 'Create technician'}
        </button>
        <button type="button" onClick={onDone} className="text-sm text-ink-soft underline underline-offset-2">
          Cancel
        </button>
      </div>
    </form>
  );
}

function TechnicianRow({ technician, serviceTypes }: { technician: Technician; serviceTypes: ServiceType[] }) {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [edit, setEdit] = useState({
    serviceTypeId: technician.serviceTypeId,
    bio: technician.bio,
    yearsExperience: technician.yearsExperience,
  });

  const updateMutation = useMutation({
    mutationFn: () => adminApi.updateTechnician(technician.technicianId, edit),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-technicians'] });
      setEditing(false);
    },
  });

  const availabilityMutation = useMutation({
    mutationFn: (available: boolean) => adminApi.setTechnicianAvailability(technician.technicianId, available),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-technicians'] }),
  });

  return (
    <div className="rounded-lg border border-line bg-paper-raised p-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="font-medium">
            {technician.name} <span className="font-utility text-xs text-ink-soft">· {technician.serviceType}</span>
          </p>
          <p className="mt-0.5 text-sm text-ink-soft">
            {technician.yearsExperience} yrs ·{' '}
            {technician.ratingCount > 0
              ? `★ ${technician.ratingAvg.toFixed(1)} (${technician.ratingCount})`
              : 'No reviews yet'}
          </p>
          {!editing && technician.bio && <p className="mt-1 text-sm text-ink-soft">{technician.bio}</p>}
        </div>
        <button
          onClick={() => availabilityMutation.mutate(!technician.available)}
          disabled={availabilityMutation.isPending}
          className={`shrink-0 rounded-md px-3 py-1.5 text-xs font-medium transition-colors disabled:opacity-50 ${
            technician.available ? 'bg-moss/15 text-moss' : 'bg-line text-ink-soft'
          }`}
        >
          {technician.available ? 'Available' : 'Unavailable'}
        </button>
      </div>

      {editing ? (
        <div className="mt-3 flex flex-col gap-2 border-t border-line pt-3">
          <div className="grid grid-cols-2 gap-2">
            <select
              value={edit.serviceTypeId}
              onChange={(e) => setEdit((s) => ({ ...s, serviceTypeId: Number(e.target.value) }))}
              className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
            >
              {serviceTypes.map((st) => (
                <option key={st.serviceTypeId} value={st.serviceTypeId}>
                  {st.name}
                </option>
              ))}
            </select>
            <input
              type="number"
              min={0}
              value={edit.yearsExperience}
              onChange={(e) => setEdit((s) => ({ ...s, yearsExperience: Number(e.target.value) }))}
              className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
            />
          </div>
          <textarea
            value={edit.bio}
            onChange={(e) => setEdit((s) => ({ ...s, bio: e.target.value }))}
            rows={2}
            className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
          />
          {updateMutation.isError && (
            <p className="text-xs text-rust">{getApiErrorMessage(updateMutation.error)}</p>
          )}
          <div className="flex gap-3">
            <button
              onClick={() => updateMutation.mutate()}
              disabled={updateMutation.isPending}
              className="rounded-md bg-signal px-3 py-1.5 text-xs font-semibold text-signal-ink transition-opacity hover:opacity-90 disabled:opacity-50"
            >
              {updateMutation.isPending ? 'Saving…' : 'Save'}
            </button>
            <button
              onClick={() => setEditing(false)}
              className="text-xs text-ink-soft underline underline-offset-2"
            >
              Cancel
            </button>
          </div>
        </div>
      ) : (
        <button
          onClick={() => setEditing(true)}
          className="mt-3 text-xs font-medium text-steel underline underline-offset-2"
        >
          Edit
        </button>
      )}
    </div>
  );
}

export function AdminTechniciansPage() {
  const [showCreate, setShowCreate] = useState(false);

  const techniciansQuery = useQuery({ queryKey: ['admin-technicians'], queryFn: adminApi.listAllTechnicians });
  const serviceTypesQuery = useQuery({ queryKey: ['service-types'], queryFn: catalogApi.listServiceTypes });

  return (
    <div>
      <div className="flex items-center justify-between">
        <h1 className="font-display text-2xl font-semibold">Technicians</h1>
        {!showCreate && (
          <button
            onClick={() => setShowCreate(true)}
            className="rounded-md bg-signal px-3 py-1.5 text-sm font-semibold text-signal-ink transition-opacity hover:opacity-90"
          >
            Provision technician
          </button>
        )}
      </div>

      {showCreate && serviceTypesQuery.data && (
        <CreateTechnicianForm serviceTypes={serviceTypesQuery.data} onDone={() => setShowCreate(false)} />
      )}

      {techniciansQuery.isLoading && <p className="mt-4 text-sm text-ink-soft">Loading technicians…</p>}
      {techniciansQuery.isError && (
        <p className="mt-4 text-sm text-rust">{getApiErrorMessage(techniciansQuery.error)}</p>
      )}

      <div className="mt-4 flex flex-col gap-3">
        {techniciansQuery.data?.map((t) => (
          <TechnicianRow key={t.technicianId} technician={t} serviceTypes={serviceTypesQuery.data ?? []} />
        ))}
      </div>
    </div>
  );
}
