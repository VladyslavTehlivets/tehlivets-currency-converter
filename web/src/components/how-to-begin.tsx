import { RATE_ADD_ROUTE } from '@/lib/routes'
import { Bezel } from '@/components/bezel'
import { PageHead } from '@/components/page-head'
import { NestedCtaLink } from '@/components/ui/nested-cta'

export function HowToBegin() {
  return (
    <>
      <PageHead title="Start with a rate." lede="Your worksheet has no rates yet." />
      <Bezel>
        <div className="flex min-h-82.5 flex-col items-center justify-center gap-4 text-center">
          <div
            aria-hidden="true"
            className="grid size-19.5 place-items-center rounded-orb bg-primary-container text-3xl text-on-primary-container shadow-highlight"
          >
            ＋
          </div>
          <h2 className="text-headline-lg">Keep your first dated rate.</h2>
          <p className="max-w-[36ch]">Add the direction and the date you want to use.</p>
          <NestedCtaLink href={RATE_ADD_ROUTE}>Add a rate</NestedCtaLink>
        </div>
      </Bezel>
    </>
  )
}
