import { cn } from '@/lib/utils'
import { aThumbTall } from '@/components/ui/thumb-reach'

const PROVIDER_DOORS = {
  google: {
    label: 'Continue with Google',
    action: '/auth/google/start',
    mark: '/brand/google-g-light.png',
    markSet: '/brand/google-g-light@2x.png 2x, /brand/google-g-light@3x.png 3x',
  },
} as const

export type ProviderDoorName = keyof typeof PROVIDER_DOORS

export function ProviderDoor({
  door,
  busy,
  next,
  prompt,
  onOpen,
}: {
  door: ProviderDoorName
  busy: boolean
  next: string
  prompt?: 'select_account'
  onOpen: () => void
}) {
  const provider = PROVIDER_DOORS[door]
  return (
    <form method="get" action={provider.action} onSubmit={onOpen} data-door={door}>
      <input type="hidden" name="next" value={next} />
      {prompt ? <input type="hidden" name="prompt" value={prompt} /> : null}
      <button
        type="submit"
        disabled={busy}
        aria-busy={busy}
        className={cn(
          aThumbTall,
          'flex w-full cursor-pointer items-center justify-center gap-2.5 rounded-full border border-provider-door-edge bg-provider-door px-4 font-provider-door text-base font-medium text-provider-door-ink transition-colors duration-(--duration-control) hover:bg-provider-door-pressed active:border-provider-door-ink active:bg-provider-door-pressed disabled:cursor-default disabled:bg-provider-door-pressed',
        )}
      >
        {busy ? (
          <span
            aria-hidden="true"
            className="size-5 animate-spin rounded-full border-2 border-provider-door-ink/25 border-t-provider-door-ink"
          />
        ) : (
          <img src={provider.mark} srcSet={provider.markSet} alt="" width={20} height={20} />
        )}
        {busy ? 'Opening Google…' : provider.label}
      </button>
    </form>
  )
}
