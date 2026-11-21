'use client'

import dynamic from 'next/dynamic'
import { useSearchParams } from 'next/navigation'
import { useEffect, useState } from 'react'

import { Bezel } from '@/components/bezel'
import { Eyebrow } from '@/components/eyebrow'
import { SignInNotice } from '@/components/sign-in-notice'
import { ProviderDoor } from '@/components/ui/provider-door'
import { googleCanReturnHere, refusalNoticeFor, sameOriginPath, type SignInNoticeCopy } from '@/lib/sign-in'

const TestDoor = dynamic(() => import('@/components/test-door').then((module) => module.TestDoor), { ssr: false })

const NO_DOOR_HERE: SignInNoticeCopy = {
  tone: 'neutral',
  title: 'Google sign-in needs the secure address',
  body: 'This copy of Currency Converter runs on a local address. Google cannot send you back to it.',
}

export function SignInView() {
  const query = useSearchParams()
  const next = sameOriginPath(query.get('next'))
  const [arrivalNotice] = useState(() => refusalNoticeFor(query.get('refused')))
  const [busy, setBusy] = useState(false)
  const [doorAvailable, setDoorAvailable] = useState(true)

  useEffect(() => {
    setDoorAvailable(googleCanReturnHere(window.location))
    const url = new URL(window.location.href)
    if (url.searchParams.has('refused')) {
      url.searchParams.delete('refused')
      url.searchParams.delete('door')
      window.history.replaceState(null, '', url)
    }
    const restore = (event: PageTransitionEvent) => event.persisted && setBusy(false)
    window.addEventListener('pageshow', restore)
    return () => window.removeEventListener('pageshow', restore)
  }, [])

  const notice = doorAvailable ? arrivalNotice : NO_DOOR_HERE

  return (
    <div className="grid items-center gap-8 md:min-h-142.5 md:grid-cols-[1.02fr_1fr] lg:gap-17.5">
      <header>
        <Eyebrow>Your account</Eyebrow>
        <h1 className="max-w-[15ch] text-page-title">Sign in to Currency Converter</h1>
        <p className="mt-3.5 max-w-[43ch] text-lead">Use your Google account. Worksheets still open by their own key, signed in or not.</p>
      </header>
      <div>
        <Bezel>
          {notice ? <SignInNotice copy={notice} /> : null}
          {doorAvailable ? (
            <>
              <div onSubmit={() => setBusy(true)}>
                <ProviderDoor door="google" busy={busy} next={next} />
              </div>
              <p className="mt-3 text-caption text-on-surface-variant">Google sends you back here when you finish.</p>
              <p role="status" className="sr-only">
                {busy ? 'Opening Google' : ''}
              </p>
            </>
          ) : null}
        </Bezel>
        <TestDoor next={next} />
      </div>
    </div>
  )
}
