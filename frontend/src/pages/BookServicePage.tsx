import { useMemo, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { catalogApi } from '../api/catalog';
import { reservationsApi } from '../api/reservations';
import { getApiErrorMessage } from '../api/client';
import { TIME_SLOTS } from '../constants';
import type { ServiceType, Technician } from '../types';

const PHONE_PATTERN = /^[6-9]\d{9}$/;

function validatePhone(value: string): string | null {
  if (!value) return null;
  if (!PHONE_PATTERN.test(value)) return 'Must be exactly 10 digits, starting with 6-9';
  return null;
}

type Step = 'service' | 'technician' | 'details' | 'confirm';

const STEPS: { key: Step; label: string }[] = [
  { key: 'service', label: 'Service' },
  { key: 'technician', label: 'Technician' },
  { key: 'details', label: 'Details' },
  { key: 'confirm', label: 'Confirm' },
];

function StepIndicator({ current }: { current: Step }) {
  const currentIndex = STEPS.findIndex((s) => s.key === current);
  return (
    <ol className="mb-8 flex items-center gap-2 font-utility text-xs">
      {STEPS.map((step, i) => (
        <li key={step.key} className="flex items-center gap-2">
          <span
            className={`flex h-6 w-6 items-center justify-center rounded-full ${
              i <= currentIndex ? 'bg-signal text-signal-ink' : 'bg-line text-ink-soft'
            }`}
          >
            {i + 1}
          </span>
          <span className={i <= currentIndex ? 'text-ink' : 'text-ink-soft'}>{step.label}</span>
          {i < STEPS.length - 1 && <span className="mx-1 h-px w-6 bg-line" aria-hidden="true" />}
        </li>
      ))}
    </ol>
  );
}

export function BookServicePage() {
  const navigate = useNavigate();
  const [step, setStep] = useState<Step>('service');
  const [serviceType, setServiceType] = useState<ServiceType | null>(null);
  const [technician, setTechnician] = useState<Technician | null>(null); // null = auto-assign
  const [autoAssign, setAutoAssign] = useState(true);
  const [phoneTouched, setPhoneTouched] = useState(false);
  const [form, setForm] = useState({
    reservationDate: '',
    timeSlot: TIME_SLOTS[0],
    address: '',
    telephone: '',
    comments: '',
  });

  const serviceTypesQuery = useQuery({
    queryKey: ['service-types'],
    queryFn: catalogApi.listServiceTypes,
  });

  const techniciansQuery = useQuery({
    queryKey: ['technicians', serviceType?.serviceTypeId],
    queryFn: () => catalogApi.listTechnicians(serviceType!.serviceTypeId),
    enabled: step === 'technician' && !!serviceType,
  });

  const createMutation = useMutation({
    mutationFn: () =>
      reservationsApi.create({
        serviceTypeId: serviceType!.serviceTypeId,
        technicianId: autoAssign ? undefined : technician?.technicianId,
        reservationDate: form.reservationDate,
        timeSlot: form.timeSlot,
        address: form.address,
        telephone: form.telephone,
        comments: form.comments || undefined,
      }),
  });

  const todayIso = useMemo(() => new Date().toISOString().slice(0, 10), []);

  function update<K extends keyof typeof form>(key: K, value: string) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function handlePhoneChange(raw: string) {
    // Strip anything non-digit as the user types and hard-cap at 10 -
    // structurally prevents "type however many you want" rather than just
    // validating after the fact (same pattern as SignupPage).
    update('telephone', raw.replace(/\D/g, '').slice(0, 10));
  }

  const phoneError = validatePhone(form.telephone);
  const isDetailsFormValid = form.reservationDate && form.address.trim() && !phoneError && form.telephone.length === 10;

  if (createMutation.isSuccess) {
    const res = createMutation.data;
    return (
      <div className="max-w-lg">
        <h1 className="font-display text-2xl font-semibold">Booking confirmed</h1>
        <p className="mt-2 text-sm text-ink-soft">
          Reservation #{res.reservationId} is {res.status === 'CONFIRMED' ? 'confirmed' : 'awaiting confirmation'}.
        </p>
        <div className="mt-4 rounded-lg border border-line border-l-4 border-l-signal bg-paper-raised p-4">
          <p className="font-medium">{res.serviceTypeName}</p>
          <p className="mt-1 font-utility text-sm text-ink-soft">
            {res.reservationDate} · {res.timeSlot}
          </p>
          <p className="mt-1 text-sm text-ink-soft">
            {res.technicianName ? `Technician: ${res.technicianName}` : 'Technician: to be assigned'}
          </p>
        </div>
        <button
          onClick={() => navigate('/my-bookings')}
          className="mt-6 rounded-md bg-signal px-4 py-2 text-sm font-semibold text-signal-ink transition-opacity hover:opacity-90"
        >
          View my bookings
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-lg">
      <h1 className="font-display text-2xl font-semibold">Book a service</h1>
      <StepIndicator current={step} />

      {step === 'service' && (
        <div>
          {serviceTypesQuery.isLoading && <p className="text-sm text-ink-soft">Loading services…</p>}
          {serviceTypesQuery.isError && (
            <p className="text-sm text-rust">{getApiErrorMessage(serviceTypesQuery.error)}</p>
          )}
          <div className="flex flex-col gap-3">
            {serviceTypesQuery.data?.map((st) => (
              <button
                key={st.serviceTypeId}
                onClick={() => {
                  setServiceType(st);
                  setTechnician(null);
                  setAutoAssign(true);
                  setStep('technician');
                }}
                className="rounded-lg border border-line bg-paper-raised p-4 text-left transition-colors hover:border-signal"
              >
                <div className="flex items-baseline justify-between">
                  <span className="font-medium">{st.name}</span>
                  <span className="font-utility text-sm text-ink-soft">₹{st.basePrice.toFixed(0)}+</span>
                </div>
                <p className="mt-1 text-sm text-ink-soft">{st.description}</p>
              </button>
            ))}
          </div>
        </div>
      )}

      {step === 'technician' && serviceType && (
        <div>
          <button
            onClick={() => {
              setAutoAssign(true);
              setTechnician(null);
              setStep('details');
            }}
            className={`mb-3 w-full rounded-lg border p-4 text-left transition-colors ${
              autoAssign ? 'border-signal bg-signal/10' : 'border-line bg-paper-raised hover:border-signal'
            }`}
          >
            <p className="font-medium">Auto-assign the best available technician</p>
            <p className="mt-1 text-sm text-ink-soft">
              Starts as CONFIRMED right away if someone's free for that slot.
            </p>
          </button>

          {techniciansQuery.isLoading && <p className="text-sm text-ink-soft">Loading technicians…</p>}
          {techniciansQuery.isError && (
            <p className="text-sm text-rust">{getApiErrorMessage(techniciansQuery.error)}</p>
          )}

          <div className="flex flex-col gap-3">
            {techniciansQuery.data?.map((t) => (
              <button
                key={t.technicianId}
                onClick={() => {
                  setAutoAssign(false);
                  setTechnician(t);
                  setStep('details');
                }}
                className="rounded-lg border border-line bg-paper-raised p-4 text-left transition-colors hover:border-signal"
              >
                <div className="flex items-baseline justify-between">
                  <span className="font-medium">{t.name}</span>
                  <span className="font-utility text-sm text-ink-soft">
                    {t.ratingCount > 0 ? `★ ${t.ratingAvg.toFixed(1)} (${t.ratingCount})` : 'No reviews yet'}
                  </span>
                </div>
                <p className="mt-1 text-sm text-ink-soft">
                  {t.yearsExperience} yrs experience · {t.bio}
                </p>
              </button>
            ))}
            {techniciansQuery.data?.length === 0 && (
              <p className="text-sm text-ink-soft">
                No technicians currently listed as available — auto-assign will still queue your booking as
                PENDING.
              </p>
            )}
          </div>

          <button onClick={() => setStep('service')} className="mt-4 text-sm text-ink-soft underline underline-offset-2">
            Back
          </button>
        </div>
      )}

      {step === 'details' && (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            setPhoneTouched(true);
            if (!isDetailsFormValid) return;
            setStep('confirm');
          }}
          className="flex flex-col gap-4"
        >
          <label className="flex flex-col gap-1">
            <span className="text-sm font-medium">Date</span>
            <input
              type="date"
              min={todayIso}
              required
              value={form.reservationDate}
              onChange={(e) => update('reservationDate', e.target.value)}
              className="rounded-md border border-line bg-paper-raised px-3 py-2 text-sm outline-none focus-visible:border-signal"
            />
          </label>

          <label className="flex flex-col gap-1">
            <span className="text-sm font-medium">Time slot</span>
            <select
              value={form.timeSlot}
              onChange={(e) => update('timeSlot', e.target.value)}
              className="rounded-md border border-line bg-paper-raised px-3 py-2 text-sm outline-none focus-visible:border-signal"
            >
              {TIME_SLOTS.map((slot) => (
                <option key={slot} value={slot}>
                  {slot}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-1">
            <span className="text-sm font-medium">Address</span>
            <input
              required
              value={form.address}
              onChange={(e) => update('address', e.target.value)}
              className="rounded-md border border-line bg-paper-raised px-3 py-2 text-sm outline-none focus-visible:border-signal"
            />
          </label>

          <label className="flex flex-col gap-1">
            <span className="text-sm font-medium">Phone</span>
            <input
              type="tel"
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={10}
              required
              value={form.telephone}
              onChange={(e) => handlePhoneChange(e.target.value)}
              onBlur={() => setPhoneTouched(true)}
              className="rounded-md border border-line bg-paper-raised px-3 py-2 text-sm outline-none focus-visible:border-signal"
            />
            <span className={`text-xs ${phoneTouched && phoneError ? 'text-rust' : 'text-ink-soft'}`}>
              {phoneTouched && phoneError ? phoneError : 'Exactly 10 digits, starting with 6-9.'}
            </span>
          </label>

          <label className="flex flex-col gap-1">
            <span className="text-sm font-medium">Comments (optional)</span>
            <textarea
              value={form.comments}
              onChange={(e) => update('comments', e.target.value)}
              rows={3}
              className="rounded-md border border-line bg-paper-raised px-3 py-2 text-sm outline-none focus-visible:border-signal"
            />
          </label>

          <div className="mt-2 flex gap-3">
            <button
              type="button"
              onClick={() => setStep('technician')}
              className="rounded-md border border-line px-4 py-2 text-sm font-medium text-ink-soft"
            >
              Back
            </button>
            <button
              type="submit"
              disabled={!isDetailsFormValid}
              className="rounded-md bg-signal px-4 py-2 text-sm font-semibold text-signal-ink transition-opacity hover:opacity-90 disabled:opacity-50"
            >
              Review booking
            </button>
          </div>
        </form>
      )}

      {step === 'confirm' && serviceType && (
        <div>
          <div className="rounded-lg border border-line bg-paper-raised p-4">
            <p className="font-medium">{serviceType.name}</p>
            <p className="mt-1 text-sm text-ink-soft">
              {autoAssign ? 'Auto-assigned technician' : technician?.name}
            </p>
            <p className="mt-1 font-utility text-sm text-ink-soft">
              {form.reservationDate} · {form.timeSlot}
            </p>
            <p className="mt-1 text-sm text-ink-soft">{form.address}</p>
            <p className="text-sm text-ink-soft">{form.telephone}</p>
            {form.comments && <p className="mt-1 text-sm text-ink-soft">"{form.comments}"</p>}
          </div>

          {createMutation.isError && (
            <p role="alert" className="mt-3 rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">
              {getApiErrorMessage(createMutation.error)}
            </p>
          )}

          <div className="mt-4 flex gap-3">
            <button
              onClick={() => setStep('details')}
              className="rounded-md border border-line px-4 py-2 text-sm font-medium text-ink-soft"
            >
              Back
            </button>
            <button
              onClick={() => createMutation.mutate()}
              disabled={createMutation.isPending}
              className="rounded-md bg-signal px-4 py-2 text-sm font-semibold text-signal-ink transition-opacity hover:opacity-90 disabled:opacity-50"
            >
              {createMutation.isPending ? 'Booking…' : 'Confirm booking'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
