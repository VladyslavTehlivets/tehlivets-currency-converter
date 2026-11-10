import type { Metadata } from 'next'

import { Bezel } from '@/components/bezel'
import { PageHead } from '@/components/page-head'

export const metadata: Metadata = { title: 'How conversion works' }

const STEPS = [
  {
    title: '1 · Choose the direction',
    detail: 'From and to currencies define the rate you need. A rate from USD to PLN does not supply the reverse direction.',
  },
  {
    title: '2 · Find the nearest date',
    detail:
      'Select the available rate nearest to your conversion date, before or after it. When two rates are equally near, the earlier rate wins.',
  },
  {
    title: '3 · Pass through the base',
    detail:
      'The base currency is PLN. A non-base pair converts through it in two steps. Each step shows its selected rate, and the multiplication stays exact.',
  },
  {
    title: '4 · Keep missing rates visible',
    detail: 'The worksheet stops when the required direction has no rate. It names the direction you can add.',
  },
] as const

export default function HowItWorksPage() {
  return (
    <>
      <PageHead title="A result you can follow." lede="The worksheet keeps each choice visible." />
      <Bezel>
        <ol aria-label="How conversion works">
          {STEPS.map((step) => (
            <li key={step.title} className="border-b border-rule py-4.5 last:border-b-0">
              <h2 className="font-body text-body-md font-semibold tracking-normal">{step.title}</h2>
              <p className="mt-1.25 text-caption">{step.detail}</p>
            </li>
          ))}
        </ol>
      </Bezel>
    </>
  )
}
