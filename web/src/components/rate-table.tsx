import { shortMoment } from '@/lib/moments'
import type { DatedRate } from '@/lib/worksheet-api'
import { Button } from '@/components/ui/button'

const cell = 'border-b border-rule px-2.5 py-4.5 text-left'

const columnHead = `${cell} text-fine font-medium text-on-surface-variant`

export function RateTable({
  rates,
  onRemove,
  ref,
}: {
  rates: DatedRate[]
  onRemove: (rate: DatedRate) => void
  ref: React.Ref<HTMLTableElement>
}) {
  return (
    <div className="-mx-1.75 overflow-x-auto md:mx-0">
      <table ref={ref} tabIndex={-1} className="w-full border-collapse text-label-lg focus-visible:outline-none">
        <caption className="sr-only">Your dated rates</caption>
        <thead>
          <tr>
            <th scope="col" className={columnHead}>
              Direction
            </th>
            <th scope="col" className={columnHead}>
              Rate
            </th>
            <th scope="col" className={columnHead}>
              Date
            </th>
            <th scope="col" className={columnHead}>
              <span className="sr-only">Remove</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {rates.map((rate) => (
            <tr key={rate.id}>
              <td className={`${cell} whitespace-nowrap`}>
                {rate.from} → {rate.to}
              </td>
              <td className={`${cell} tabular-nums wrap-anywhere`}>{rate.rate}</td>
              <td className={cell}>{shortMoment(rate.ratedAt)}</td>
              <td className={`${cell} text-right`}>
                <Button
                  size="commit"
                  aria-label={`Remove ${rate.from} → ${rate.to} at ${shortMoment(rate.ratedAt)}`}
                  onClick={() => onRemove(rate)}
                >
                  Remove
                </Button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
