import { Suspense } from 'react'

import { Bezel } from '@/components/bezel'
import { FormWaiting } from '@/components/form-waiting'
import { Converter } from './converter'

export default function ConvertPage() {
  return (
    <Suspense
      fallback={
        <Bezel>
          <FormWaiting label="Loading your conversion" />
        </Bezel>
      }
    >
      <Converter />
    </Suspense>
  )
}
