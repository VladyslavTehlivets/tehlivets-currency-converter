import { Skeleton } from '@/components/ui/skeleton'

export function FormWaiting({ label }: { label: string }) {
  return (
    <div role="status" aria-label={label}>
      <Skeleton className="w-1/3" />
      <Skeleton className="h-11" />
      <Skeleton className="h-11" />
      <Skeleton className="h-11" />
    </div>
  )
}
