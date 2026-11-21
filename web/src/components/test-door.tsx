'use client'

import { useEffect, useState } from 'react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { FormField } from '@/components/form-field'
import { sameOriginPath } from '@/lib/sign-in'

export function TestDoor({ next }: { next: string }) {
  const [open, setOpen] = useState(false)
  useEffect(() => {
    fetch('/auth/test/door', { cache: 'no-store' })
      .then((answer) => setOpen(answer.status === 200))
      .catch(() => setOpen(false))
  }, [])
  if (!open) return null
  return (
    <section aria-labelledby="test-door-title" className="mt-6 rounded-note border border-dashed border-rule p-4.5">
      <h2 id="test-door-title" className="text-label-lg font-semibold">
        Dev only · test door
      </h2>
      <form method="post" action="/auth/test/sign-in" className="mt-3">
        <input type="hidden" name="next" value={sameOriginPath(next)} />
        <FormField fieldId="test-email" label="Test email" error={undefined}>
          <Input id="test-email" name="email" type="email" autoComplete="username" required />
        </FormField>
        <FormField fieldId="test-password" label="Test password" error={undefined}>
          <Input id="test-password" name="password" type="password" autoComplete="current-password" required />
        </FormField>
        <Button type="submit">Sign in to dev</Button>
      </form>
    </section>
  )
}
