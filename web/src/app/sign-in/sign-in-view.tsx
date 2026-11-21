'use client'

import dynamic from 'next/dynamic'
import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'next/navigation'

import { Bezel } from '@/components/bezel'
import { Eyebrow } from '@/components/eyebrow'
import { SignInNotice } from '@/components/sign-in-notice'
import { ProviderDoor } from '@/components/ui/provider-door'
import { asksForAnotherAccount, noticeForRefusal, safeNextPath, type SignInNoticeContent } from '@/lib/sign-in-refusal'

const TestDoor = process.env.NEXT_PUBLIC_E2E_TEST_IDENTITY === 'on' ? dynamic(() => import('@/components/test-door')) : null

const NEEDS_A_SECURE_ADDRESS: SignInNoticeContent = {
  tone: 'quiet',
  role: 'status',
  title: 'Google sign-in needs a secure address',
  body: 'This copy of Currency Converter runs on a local http address, where Google cannot send you back.',
}

export function SignInView() {
  const query = useSearchParams()
  const next = safeNextPath(query.get('next'))
  const [refusal] = useState(() => noticeForRefusal(query))
  const [notice, setNotice] = useState<SignInNoticeContent | null>(refusal)
  const [secureOrigin, setSecureOrigin] = useState<boolean | null>(null)
  const [busy, setBusy] = useState(false)
  const noticeRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    setSecureOrigin(window.isSecureContext)
    if (refusal) window.history.replaceState(null, '', `${window.location.pathname}${next === '/' ? '' : `?next=${encodeURIComponent(next)}`}`)
    const clearBusyOnReturn = (event: PageTransitionEvent) => event.persisted && setBusy(false)
    window.addEventListener('pageshow', clearBusyOnReturn)
    return () => window.removeEventListener('pageshow', clearBusyOnReturn)
  }, [refusal, next])

  useEffect(() => {
    if (notice) noticeRef.current?.focus()
  }, [notice])

  const shownNotice = secureOrigin === false ? NEEDS_A_SECURE_ADDRESS : notice

  return (
    <section className="grid items-start gap-9 md:min-h-142.5 md:grid-cols-[1.02fr_1fr] md:gap-17.5 md:pt-10">
      <div>
        <Eyebrow>Your account</Eyebrow>
        <h1 className="max-w-[15ch] text-page-title">Sign in to Currency Converter.</h1>
        <p className="mt-5.5 max-w-[40ch] text-lead text-pretty">
          Continue with your Google account. Worksheets you open by their key stay open without it.
        </p>
      </div>
      <div className="relative isolate min-w-0 before:absolute before:inset-x-0 before:-inset-y-[12%] before:pointer-events-none before:-z-10 before:bg-(image:--gradient-halo) before:content-['']">
        <Bezel>
          {shownNotice ? <SignInNotice ref={noticeRef} notice={shownNotice} /> : null}
          {secureOrigin === false ? null : (
            <>
              <ProviderDoor
                door="google"
                busy={busy}
                next={next}
                prompt={asksForAnotherAccount(refusal) ? 'select_account' : undefined}
                onOpen={() => {
                  setNotice(null)
                  setBusy(true)
                }}
              />
              <p className="mt-3.5 text-caption text-pretty">
                Google sends Currency Converter your verified email. Your Google password stays with Google.
              </p>
              <p role="status" className="sr-only">
                {busy ? 'Opening Google' : ''}
              </p>
            </>
          )}
        </Bezel>
        {TestDoor ? <TestDoor next={next} /> : null}
      </div>
    </section>
  )
}
