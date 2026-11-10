import { CONVERT_ROUTE, METHOD_ROUTE, RATES_ROUTE } from '@/lib/routes'

export const PRIMARY_LINKS = [
  { href: CONVERT_ROUTE, label: 'Convert' },
  { href: RATES_ROUTE, label: 'Rates' },
  { href: METHOD_ROUTE, label: 'How it works' },
] as const
