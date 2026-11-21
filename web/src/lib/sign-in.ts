export const SIGN_IN_ROUTE = '/sign-in'

export type NoticeTone = 'neutral' | 'trouble'

export interface SignInNoticeCopy {
  tone: NoticeTone
  title: string
  body: string
}

const DID_NOT_FINISH: SignInNoticeCopy = {
  tone: 'trouble',
  title: 'That sign-in did not finish',
  body: 'A sign-in must finish in the browser that started it, within ten minutes. Start again below.',
}

const NOTICES: Record<string, SignInNoticeCopy> = {
  cancelled: {
    tone: 'neutral',
    title: 'You cancelled at Google',
    body: 'Nothing was shared with Currency Converter. Continue whenever you are ready.',
  },
  'state-mismatch': DID_NOT_FINISH,
  'token-for-another-client': DID_NOT_FINISH,
  'email-unverified': {
    tone: 'trouble',
    title: "Google has not verified this account's email",
    body: 'Continue with a Google account whose email is verified.',
  },
  'not-invited': {
    tone: 'trouble',
    title: 'This account is not invited',
    body: 'Ask the owner of Currency Converter to invite you, or use another Google account.',
  },
  'provider-unavailable': {
    tone: 'trouble',
    title: 'Google did not answer',
    body: 'Nothing was changed. Try again in a moment.',
  },
}

export function refusalNoticeFor(code: string | null): SignInNoticeCopy | null {
  if (code === null) return null
  return Object.hasOwn(NOTICES, code) ? NOTICES[code] : DID_NOT_FINISH
}

export function signInHref(next: string): string {
  return next === '/' ? SIGN_IN_ROUTE : `${SIGN_IN_ROUTE}?next=${encodeURIComponent(next)}`
}

export function sameOriginPath(candidate: string | null): string {
  if (!candidate || !candidate.startsWith('/')) return '/'
  if (candidate.startsWith('//') || candidate.startsWith('/\\')) return '/'
  return candidate
}

export function googleCanReturnHere(location: Pick<Location, 'protocol' | 'hostname'>): boolean {
  const loopback = ['localhost', '127.0.0.1', '[::1]', '::1'].includes(location.hostname)
  return location.protocol === 'https:' || loopback
}
