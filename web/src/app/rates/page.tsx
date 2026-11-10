import type { Metadata } from 'next'

import { YourRates } from './your-rates'

export const metadata: Metadata = { title: 'Your rates' }

export default function RatesPage() {
  return <YourRates />
}
