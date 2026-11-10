import { cn } from '@/lib/utils'

export function Notice({
  tone = 'example',
  className,
  ...props
}: React.ComponentProps<'p'> & { tone?: 'example' | 'trouble' }) {
  return (
    <p
      className={cn(
        'my-4.5 max-w-none rounded-note px-4.5 py-3.75 text-caption',
        tone === 'example' ? 'bg-primary-container text-on-primary-container' : 'bg-error-container text-error',
        className,
      )}
      {...props}
    />
  )
}

export function ExampleRatesNotice() {
  return <Notice>Example rates. This result is not a market quote.</Notice>
}
