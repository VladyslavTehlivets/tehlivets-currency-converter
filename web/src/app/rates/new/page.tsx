import type { Metadata } from 'next'
import { Suspense } from 'react'

import { Bezel } from '@/components/bezel'
import { FormWaiting } from '@/components/form-waiting'
import { AddARate } from './add-a-rate'

export const metadata: Metadata = { title: 'Add a dated rate' }

export default function RateAddPage() {
  return (
    <Suspense
      fallback={
        <Bezel>
          <FormWaiting label="Loading the rate form" />
        </Bezel>
      }
    >
      <AddARate />
    </Suspense>
  )
}
