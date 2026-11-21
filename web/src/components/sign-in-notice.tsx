'use client'

import { useEffect, useRef } from 'react'

import { cn } from '@/lib/utils'
import type { SignInNoticeCopy } from '@/lib/sign-in'

export function SignInNotice({ copy }: { copy: SignInNoticeCopy }) {
  const notice = useRef<HTMLDivElement>(null)
  useEffect(() => notice.current?.focus(), [])
  return (
    <div
      ref={notice}
      tabIndex={-1}
      role={copy.tone === 'neutral' ? 'status' : 'alert'}
      className={cn(
        'mb-4.5 rounded-note px-4.5 py-3.75 text-caption outline-none',
        copy.tone === 'neutral' ? 'bg-primary-container text-on-primary-container' : 'bg-error-container text-error',
      )}
    >
      <strong className="block text-label-lg">{copy.title}</strong>
      <p>{copy.body}</p>
    </div>
  )
}
