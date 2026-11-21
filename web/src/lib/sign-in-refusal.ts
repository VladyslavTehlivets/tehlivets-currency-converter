export type RefusalTone = 'quiet' | 'trouble'

export interface SignInNoticeContent {
  tone: RefusalTone
  role: 'status' | 'alert'
  title: string
  body: string
}

export const REFUSAL_CODES = [
  'cancelled',
  'state-mismatch',
  'token-for-another-client',
  'email-unverified',
  'not-invited',
  'provider-unavailable',
] as const

const DID_NOT_FINISH: SignInNoticeContent = {
  tone: 'quiet',
  role: 'alert',
  title: 'That sign-in did not finish',
  body: 'A sign-in must finish in the browser that started it, within ten minutes. Start again below.',
}

const NOTICE_FOR_CODE: Record<string, SignInNoticeContent> = {
  cancelled: {
    tone: 'quiet',
    role: 'status',
    title: 'You cancelled at Google',
    body: 'Nothing was shared with Currency Converter. Continue whenever you are ready.',
  },
  'state-mismatch': DID_NOT_FINISH,
  'token-for-another-client': DID_NOT_FINISH,
  'email-unverified': {
    tone: 'trouble',
    role: 'alert',
    title: "Google has not verified this account's email",
    body: 'Continue with a Google account whose email is verified.',
  },
  'not-invited': {
    tone: 'trouble',
    role: 'alert',
    title: 'This Google account cannot sign in here',
    body: 'Continue with another Google account.',
  },
  'provider-unavailable': {
    tone: 'quiet',
    role: 'alert',
    title: 'Google is not answering',
    body: 'Try again in a minute.',
  },
}

const GOOGLE_DOOR = 'google'

export function noticeForRefusal(query: URLSearchParams): SignInNoticeContent | null {
  const code = query.get('refused')
  if (code === null) return null
  if (query.get('door') !== GOOGLE_DOOR) return DID_NOT_FINISH
  return Object.hasOwn(NOTICE_FOR_CODE, code) ? NOTICE_FOR_CODE[code] : DID_NOT_FINISH
}

export function asksForAnotherAccount(notice: SignInNoticeContent | null): boolean {
  return notice?.tone === 'trouble'
}

export function safeNextPath(raw: string | null): string {
  if (raw === null || !raw.startsWith('/')) return '/'
  if (raw.startsWith('//') || raw.startsWith('/\\')) return '/'
  return /[\u0000-\u001f\u007f]/.test(raw) ? '/' : raw
}
