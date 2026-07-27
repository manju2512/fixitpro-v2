import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { catalogApi } from '../api/catalog';
import { getApiErrorMessage } from '../api/client';
import { ServiceIcon } from '../components/ServiceIcon';

function ServiceCardSkeleton() {
  return (
    <div className="animate-pulse rounded-lg border border-line bg-paper-raised p-5">
      <div className="h-12 w-12 rounded-md bg-line" />
      <div className="mt-4 h-4 w-24 rounded bg-line" />
      <div className="mt-2 h-3 w-full rounded bg-line" />
      <div className="mt-1 h-3 w-3/4 rounded bg-line" />
      <div className="mt-4 h-3 w-16 rounded bg-line" />
    </div>
  );
}

function ServicesSection() {
  const { data: serviceTypes, isLoading, isError, error } = useQuery({
    queryKey: ['service-types'],
    queryFn: catalogApi.listServiceTypes,
  });

  return (
    <section className="mt-16" aria-labelledby="services-heading">
      <p className="font-utility text-xs font-medium uppercase tracking-widest text-ink-soft">
        What we fix
      </p>
      <h2 id="services-heading" className="mt-1 font-display text-2xl font-semibold tracking-tight">
        Services we provide
      </h2>
      <p className="mt-2 max-w-xl text-sm text-ink-soft">
        Vetted, background-checked technicians across three trades. Pick a service, pick a slot,
        track the job to completion.
      </p>

      {isError && (
        <p className="mt-6 rounded-md border border-rust/30 bg-rust/10 px-4 py-3 text-sm text-rust">
          Couldn't load services: {getApiErrorMessage(error)}
        </p>
      )}

      <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {isLoading &&
          Array.from({ length: 3 }).map((_, i) => <ServiceCardSkeleton key={i} />)}

        {serviceTypes?.map((service) => (
          <div
            key={service.serviceTypeId}
            className="flex flex-col rounded-lg border border-line bg-paper-raised p-5 shadow-sm transition-shadow hover:shadow-md"
          >
            <ServiceIcon name={service.name} />
            <h3 className="mt-4 font-display text-lg font-semibold text-ink">{service.name}</h3>
            <p className="mt-1.5 flex-1 text-sm text-ink-soft">{service.description}</p>
            <div className="mt-4 flex items-center justify-between">
              <span className="font-utility text-sm text-ink-soft">
                From <span className="font-medium text-ink">₹{Math.round(service.basePrice)}</span>
              </span>
              <Link
                to="/book"
                className="rounded-md bg-signal px-3 py-1.5 text-sm font-medium text-signal-ink transition-opacity hover:opacity-90"
              >
                Book now
              </Link>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}

const PROCESS_STEPS = [
  {
    stripe: 'border-l-signal',
    dot: 'bg-signal',
    title: 'Request',
    body: 'Tell us the trade, address, and a time slot that works for you.',
  },
  {
    stripe: 'border-l-steel',
    dot: 'bg-steel',
    title: 'Matched',
    body: "We confirm a technician - or you choose one yourself - and you're on the schedule.",
  },
  {
    stripe: 'border-l-moss',
    dot: 'bg-moss',
    title: 'Done',
    body: 'Track status from confirmed to in-progress to completed, then leave a review.',
  },
] as const;

function ProcessSection() {
  return (
    <section className="mt-16" aria-labelledby="process-heading">
      <p className="font-utility text-xs font-medium uppercase tracking-widest text-ink-soft">
        How it works
      </p>
      <h2 id="process-heading" className="mt-1 font-display text-2xl font-semibold tracking-tight">
        Fixit in 3 simple steps
      </h2>

      <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
        {PROCESS_STEPS.map((step, i) => (
          <div
            key={step.title}
            className={`rounded-lg border border-line border-l-4 bg-paper-raised p-5 shadow-sm ${step.stripe}`}
          >
            <span
              className={`flex h-7 w-7 items-center justify-center rounded-full font-utility text-xs font-medium text-paper-raised ${step.dot}`}
            >
              {i + 1}
            </span>
            <h3 className="mt-3 font-display text-base font-semibold text-ink">{step.title}</h3>
            <p className="mt-1 text-sm text-ink-soft">{step.body}</p>
          </div>
        ))}
      </div>
    </section>
  );
}

function AboutSection() {
  return (
    <section className="mt-16 border-t border-line pt-12" aria-labelledby="about-heading">
      <p className="font-utility text-xs font-medium uppercase tracking-widest text-ink-soft">
        About FixitPro
      </p>
      <h2 id="about-heading" className="mt-1 max-w-xl font-display text-2xl font-semibold tracking-tight">
        A booking flow that treats your job like a work order, not a ticket in a void.
      </h2>
      <p className="mt-4 max-w-2xl text-sm leading-relaxed text-ink-soft">
        Most repair bookings disappear into a phone call and a vague promise. FixitPro keeps
        every reservation visible end to end - who's assigned, what stage it's at, and what
        happens next - for customers, technicians, and admins alike.
      </p>
      <p className="mt-3 max-w-2xl text-sm leading-relaxed text-ink-soft">
        We're starting focused: electrical, plumbing, and carpentry, with the same rigor applied
        to matching, scheduling, and follow-up review on every job.
      </p>
    </section>
  );
}

export function HomePage() {
  const { user } = useAuth();

  return (
    <div>
      <div className="max-w-2xl">
        <h1 className="font-display text-3xl font-semibold tracking-tight">
          Home repairs, booked right.
        </h1>
        <p className="mt-3 text-ink-soft">
          {user
            ? `Welcome back, ${user.username}. Book a service below or check on an existing job.`
            : 'Electricians, plumbers, and carpenters — one booking flow, a real status you can track.'}
        </p>
      </div>

      <ServicesSection />
      <ProcessSection />
      <AboutSection />
    </div>
  );
}