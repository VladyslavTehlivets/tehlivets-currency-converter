import { cn } from '@/lib/utils'
import { entersOnArrival, innerCore, outerBezel } from '@/components/ui/bezel-surface'

export function Bezel({ className, children, ...props }: React.ComponentProps<'div'>) {
  return (
    <div className={cn(outerBezel, entersOnArrival, className)} {...props}>
      <div className={innerCore}>{children}</div>
    </div>
  )
}
