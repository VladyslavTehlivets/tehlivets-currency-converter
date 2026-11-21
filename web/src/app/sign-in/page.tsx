import type { Metadata } from 'next'
import { Suspense } from 'react'

import { Bezel } from '@/components/bezel'
import { FormWaiting } from '@/components/form-waiting'
import { SignInView } from './sign-in-view'

export const metadata: Metadata = { title: 'Sign in' }

export default function SignInPage() {
  return (
    <>
      <link rel="preload" href="/fonts/google-sans-medium-latin.woff2" as="font" type="font/woff2" crossOrigin="" />
      <Suspense
        fallback={
          <Bezel>
            <FormWaiting label="Loading the sign-in door" />
          </Bezel>
        }
      >
        <SignInView />
      </Suspense>
    </>
  )
}
