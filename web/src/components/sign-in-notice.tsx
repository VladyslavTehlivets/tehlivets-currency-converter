import { forwardRef } from 'react'

import { cn } from '@/lib/utils'
import type { SignInNoticeContent } from '@/lib/sign-in-refusal'

export const SignInNotice = forwardRef<HTMLDivElement, { notice: SignInNoticeContent }>(function SignInNotice(
  { notice },
  focusTarget,
) {
  return (
    <div
      ref={focusTarget}
      role={notice.role}
      tabIndex={-1}
      className={cn(
        'mb-4.5 rounded-note px-4.5 py-3.75 text-caption text-pretty [overflow-wrap:anywhere] focus-visible:outline-none',
        notice.tone === 'trouble' ? 'bg-error-container text-on-surface' : 'bg-field text-on-surface',
      )}
    >
      <p className={cn('text-label-lg font-semibold', notice.tone === 'trouble' && 'text-error')}>{notice.title}</p>
      <p className="mt-1">{notice.body}</p>
    </div>
  )
})
