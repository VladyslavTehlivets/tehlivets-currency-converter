import { Bezel } from '@/components/bezel'
import { PageHead } from '@/components/page-head'
import { Skeleton } from '@/components/ui/skeleton'

export function FindingYourRate() {
  return (
    <>
      <PageHead title="Finding the closest dated rate." lede="Your amount stays here while the worksheet loads." />
      <div role="status" aria-label="Loading conversion" className="grid gap-5.5 md:grid-cols-[1.45fr_1fr]">
        <Bezel>
          <Skeleton className="w-2/5" />
          <Skeleton className="h-13.5 w-3/4" />
          <Skeleton className="w-3/5" />
          <Skeleton className="h-30" />
        </Bezel>
        <Bezel>
          <Skeleton className="w-1/2" />
          <Skeleton className="h-15" />
          <Skeleton className="h-15" />
        </Bezel>
      </div>
    </>
  )
}
