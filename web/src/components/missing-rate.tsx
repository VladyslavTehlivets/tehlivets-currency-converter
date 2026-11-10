import type { MissingDirection } from '@/lib/worksheet-api'
import { Bezel } from '@/components/bezel'
import { PageHead } from '@/components/page-head'
import { NestedCtaLink } from '@/components/ui/nested-cta'

export function MissingRate({
  missing,
  addTheRateHref,
  changeTheConversionHref,
}: {
  missing: MissingDirection
  addTheRateHref: string
  changeTheConversionHref: string
}) {
  return (
    <>
      <PageHead title="One step needs a rate." lede="The required conversion direction is not available." />
      <Bezel>
        <div className="flex min-h-82.5 flex-col items-center justify-center gap-4 text-center">
          <h2 className="text-headline-lg">No result yet.</h2>
          <p className="max-w-[36ch]">Add a dated rate for the missing direction, then try again.</p>
          <p className="font-semibold text-on-primary-container">
            Missing direction: {missing.from} → {missing.to}
          </p>
          <div className="flex flex-wrap justify-center gap-3.5">
            <NestedCtaLink href={addTheRateHref}>Add the missing rate</NestedCtaLink>
            <NestedCtaLink tone="secondary" href={changeTheConversionHref}>
              Change the conversion
            </NestedCtaLink>
          </div>
        </div>
      </Bezel>
    </>
  )
}
