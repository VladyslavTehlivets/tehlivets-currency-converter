import { longDate, longMoment } from '@/lib/moments'
import { RATES_ROUTE } from '@/lib/routes'
import type { ConversionReceipt } from '@/lib/worksheet-api'
import { Bezel } from '@/components/bezel'
import { Eyebrow } from '@/components/eyebrow'
import { ExampleRatesNotice } from '@/components/notice'
import { PageHead } from '@/components/page-head'
import { NestedCtaLink } from '@/components/ui/nested-cta'

function pathOf(receipt: ConversionReceipt): string[] {
  const source = receipt.legs[0]?.from ?? ''
  return [`${receipt.amount} ${source}`, ...receipt.legs.map((leg) => `${leg.amountOut} ${leg.to}`)]
}

export function ConversionReceiptView({
  receipt,
  anotherConversionHref,
}: {
  receipt: ConversionReceipt
  anotherConversionHref: string
}) {
  const source = receipt.legs[0]?.from ?? ''
  const target = receipt.legs.at(-1)?.to ?? ''

  return (
    <>
      <PageHead
        title="Here is how the amount moves."
        lede="Your conversion, step by step."
        action={<NestedCtaLink href={anotherConversionHref}>Another conversion</NestedCtaLink>}
      />
      <div className="grid gap-5.5 md:grid-cols-[1.45fr_1fr]">
        <Bezel>
          <Eyebrow>Converted amount</Eyebrow>
          <div className="font-display text-metric tabular-nums wrap-anywhere">
            {receipt.result} <small className="font-body text-unit tracking-normal">{target}</small>
          </div>
          <p className="mt-2">
            {receipt.amount} {source} · Example rates · {longDate(receipt.at)}
          </p>
          <hr className="my-6 border-rule" />
          <h2 className="text-title">The conversion path</h2>
          <ol aria-label="Conversion path" className="flex flex-wrap items-center justify-center gap-3.5 py-8.5">
            {pathOf(receipt).map((node, step) => (
              <li key={step} className="flex min-w-0 items-center gap-3.5">
                {step > 0 ? <span aria-hidden="true">→</span> : null}
                <span className="min-w-0 rounded-node bg-primary-container px-5.75 py-4.5 text-caption font-semibold text-on-primary-container tabular-nums wrap-anywhere">
                  {node}
                </span>
              </li>
            ))}
          </ol>
        </Bezel>
        <Bezel>
          <h2 className="text-title">The selected rates</h2>
          <ul aria-label="Selected rates">
            {receipt.legs.map((leg) => (
              <li
                key={`${leg.from}-${leg.to}`}
                className="flex items-center justify-between gap-2.5 border-b border-rule py-4.5 last:border-b-0 md:gap-4.5"
              >
                <div>
                  <strong className="font-semibold">
                    {leg.from} → {leg.to}
                  </strong>
                  <p className="mt-1.25 text-caption">{longMoment(leg.ratedAt)}</p>
                </div>
                <span className="min-w-0 tabular-nums wrap-anywhere">{leg.rate}</span>
              </li>
            ))}
          </ul>
          <ExampleRatesNotice />
          <NestedCtaLink tone="secondary" href={RATES_ROUTE}>
            Review rates
          </NestedCtaLink>
        </Bezel>
      </div>
    </>
  )
}
