// Fixed daily slots. If the backend ever exposes configurable slots via
// business-schedule, swap this for an API call - kept static for now since
// there's no such endpoint yet.
export const TIME_SLOTS = [
  '09:00-11:00',
  '11:00-13:00',
  '13:00-15:00',
  '15:00-17:00',
  '17:00-19:00',
] as const;
