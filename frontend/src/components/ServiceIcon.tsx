/**
 * Icon set for the three service types (see V2__seed_reference_data.sql).
 * Drawn in the same monoline, filled-shape style as the Layout.tsx wrench
 * mark - flat geometric shapes, not stroke-based line icons - so they read
 * as part of the same visual family rather than a generic icon pack.
 */
type IconProps = { className?: string };

function ElectricianIcon({ className }: IconProps) {
  return (
    <svg viewBox="0 0 32 32" fill="none" className={className} aria-hidden="true">
      <rect x="4" y="4" width="24" height="24" rx="4" className="fill-signal/15" />
      <path
        d="M17.5 8 10 18h5l-1 6 7.5-10h-5l1-6Z"
        className="fill-signal-ink"
      />
    </svg>
  );
}

function PlumberIcon({ className }: IconProps) {
  return (
    <svg viewBox="0 0 32 32" fill="none" className={className} aria-hidden="true">
      <rect x="4" y="4" width="24" height="24" rx="4" className="fill-steel/15" />
      <path
        d="M12 7h4v5.5a4.5 4.5 0 1 1-4 0V7Z"
        className="fill-steel"
      />
      <rect x="12.5" y="7" width="3" height="3" className="fill-paper-raised" />
    </svg>
  );
}

function CarpenterIcon({ className }: IconProps) {
  return (
    <svg viewBox="0 0 32 32" fill="none" className={className} aria-hidden="true">
      <rect x="4" y="4" width="24" height="24" rx="4" className="fill-moss/15" />
      <path
        d="M8 22 20 10a1.8 1.8 0 0 1 2.5 0l1.5 1.5a1.8 1.8 0 0 1 0 2.5L12 26l-5 1 1-5Z"
        className="fill-moss"
      />
    </svg>
  );
}

function GeneralToolIcon({ className }: IconProps) {
  return (
    <svg viewBox="0 0 32 32" fill="none" className={className} aria-hidden="true">
      <rect x="4" y="4" width="24" height="24" rx="4" className="fill-ink/10" />
      <path
        d="M12 21 20.5 12.5a3 3 0 1 0-2.7-2.7L9.3 18.3a2 2 0 1 0 2.7 2.7Z"
        className="fill-ink-soft"
      />
    </svg>
  );
}

const ICONS_BY_SERVICE_NAME: Record<string, (props: IconProps) => React.JSX.Element> = {
  Electrician: ElectricianIcon,
  Plumber: PlumberIcon,
  Carpenter: CarpenterIcon,
};

/** Looks up the icon for a service-type name; falls back to a generic tool mark for anything added later. */
export function ServiceIcon({ name, className = 'h-12 w-12' }: { name: string; className?: string }) {
  const Icon = ICONS_BY_SERVICE_NAME[name] ?? GeneralToolIcon;
  return <Icon className={className} />;
}
