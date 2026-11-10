import type { Metadata } from 'next'
import { Suspense } from 'react'

import { FindingYourRate } from '@/components/finding-your-rate'
import { ConversionResult } from './conversion-result'

export const metadata: Metadata = { title: 'Conversion result' }

export default function ResultPage() {
  return (
    <Suspense fallback={<FindingYourRate />}>
      <ConversionResult />
    </Suspense>
  )
}
