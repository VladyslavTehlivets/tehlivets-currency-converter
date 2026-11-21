import { sameOriginPath } from '@/lib/sign-in'

const DOOR =
  'relative inline-flex min-h-11 w-full cursor-pointer items-center justify-center gap-2.5 rounded-full border border-provider-door-edge bg-provider-door px-3 font-provider-door text-body-md font-medium whitespace-nowrap text-provider-door-ink transition-[transform,background-color] duration-(--duration-control) ease-spring select-none hover:bg-provider-door-pressed active:scale-97 active:border-provider-door-ink active:bg-provider-door-pressed disabled:pointer-events-none'

const GOOGLE_MARK = '/brand/google-g-light.png'

export function ProviderDoor({ door, busy, next }: { door: 'google'; busy: boolean; next: string }) {
  return (
    <form method="post" action={`/auth/${door}/start`} className="w-full">
      <input type="hidden" name="next" value={sameOriginPath(next)} />
      <button type="submit" className={DOOR} disabled={busy} aria-busy={busy}>
        {busy ? (
          <span aria-hidden className="size-5 animate-spin rounded-full border-2 border-provider-door-edge border-t-transparent" />
        ) : (
          <img src={GOOGLE_MARK} alt="" width={20} height={20} className="size-5" />
        )}
        {busy ? 'Opening Google…' : 'Continue with Google'}
      </button>
    </form>
  )
}
