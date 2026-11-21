import { CONVERT_ROUTE, METHOD_ROUTE, RATES_ROUTE, SIGN_IN_ROUTE } from '@/lib/routes'

export const PRIMARY_LINKS = [
  { href: CONVERT_ROUTE, label: 'Convert' },
  { href: RATES_ROUTE, label: 'Rates' },
  { href: METHOD_ROUTE, label: 'How it works' },
  { href: SIGN_IN_ROUTE, label: 'Sign in' },
] as const
