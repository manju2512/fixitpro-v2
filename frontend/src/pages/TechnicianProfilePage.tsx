import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { technicianApi } from '../api/technicians';
import { getApiErrorMessage } from '../api/client';

export function TechnicianProfilePage() {
  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: ['my-technician-profile'], queryFn: technicianApi.getMyProfile });

  const [editing, setEditing] = useState(false);
  const [bio, setBio] = useState('');
  const [yearsExperience, setYearsExperience] = useState('');

  const updateMutation = useMutation({
    mutationFn: () =>
      technicianApi.updateMyProfile({
        bio,
        yearsExperience: yearsExperience === '' ? undefined : Number(yearsExperience),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-technician-profile'] });
      setEditing(false);
    },
  });

  const availabilityMutation = useMutation({
    mutationFn: (available: boolean) => technicianApi.setMyAvailability(available),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['my-technician-profile'] }),
  });

  function startEditing() {
    if (!query.data) return;
    setBio(query.data.bio ?? '');
    setYearsExperience(String(query.data.yearsExperience ?? ''));
    setEditing(true);
  }

  if (query.isLoading) return <p className="text-sm text-ink-soft">Loading your profile…</p>;
  if (query.isError) return <p className="text-sm text-rust">{getApiErrorMessage(query.error)}</p>;
  if (!query.data) return null;

  const tech = query.data;

  return (
    <div className="max-w-lg">
      <h1 className="font-display text-2xl font-semibold tracking-tight">My profile</h1>

      <div className="mt-6 rounded-lg border border-line bg-paper-raised p-6 shadow-sm">
        <div className="flex items-start justify-between">
          <div>
            <p className="font-display text-lg font-semibold">{tech.name}</p>
            <p className="text-sm text-ink-soft">{tech.serviceType}</p>
          </div>
          <span className="font-utility text-sm text-ink-soft">
            {tech.ratingCount > 0 ? `★ ${tech.ratingAvg.toFixed(1)} (${tech.ratingCount} reviews)` : 'No reviews yet'}
          </span>
        </div>

        {editing ? (
          <div className="mt-4 flex flex-col gap-3">
            <label className="flex flex-col gap-1">
              <span className="text-sm font-medium">Bio</span>
              <textarea
                value={bio}
                onChange={(e) => setBio(e.target.value)}
                rows={3}
                className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
              />
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-sm font-medium">Years of experience</span>
              <input
                type="number"
                min="0"
                value={yearsExperience}
                onChange={(e) => setYearsExperience(e.target.value)}
                className="w-32 rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
              />
            </label>

            {updateMutation.isError && (
              <p className="text-sm text-rust">{getApiErrorMessage(updateMutation.error)}</p>
            )}

            <div className="flex gap-2">
              <button
                onClick={() => updateMutation.mutate()}
                disabled={updateMutation.isPending}
                className="rounded-md bg-signal px-4 py-2 text-sm font-semibold text-signal-ink transition-opacity hover:opacity-90 disabled:opacity-50"
              >
                {updateMutation.isPending ? 'Saving…' : 'Save'}
              </button>
              <button
                onClick={() => setEditing(false)}
                className="rounded-md border border-line px-4 py-2 text-sm font-medium text-ink-soft"
              >
                Cancel
              </button>
            </div>
          </div>
        ) : (
          <div className="mt-4">
            <p className="text-sm text-ink-soft">{tech.bio || 'No bio yet.'}</p>
            <p className="mt-1 text-sm text-ink-soft">{tech.yearsExperience} years of experience</p>
            <button
              onClick={startEditing}
              className="mt-3 text-sm font-medium text-steel underline underline-offset-2"
            >
              Edit bio &amp; experience
            </button>
          </div>
        )}

        <div className="mt-6 flex items-center justify-between border-t border-line pt-4">
          <div>
            <p className="text-sm font-medium">Availability</p>
            <p className="text-xs text-ink-soft">
              {tech.available ? 'You are visible to customers booking new jobs.' : "You're currently hidden from new bookings."}
            </p>
          </div>
          <button
            onClick={() => availabilityMutation.mutate(!tech.available)}
            disabled={availabilityMutation.isPending}
            className={`rounded-full px-4 py-1.5 text-sm font-medium transition-colors disabled:opacity-40 ${
              tech.available ? 'bg-moss/15 text-moss hover:bg-moss/25' : 'bg-rust/15 text-rust hover:bg-rust/25'
            }`}
          >
            {tech.available ? 'Available' : 'Unavailable'} · Toggle
          </button>
        </div>
      </div>
    </div>
  );
}
