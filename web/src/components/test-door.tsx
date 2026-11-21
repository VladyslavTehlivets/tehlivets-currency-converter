'use client'

import { useState } from 'react'

import { FormError, errorIdFor } from '@/components/form-field'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { signInThroughTestDoor } from '@/lib/worksheet-api'

const REFUSED = 'That email and password do not match the test member.'
const UNREACHABLE = 'The sign-in service did not answer. Try again.'
const ERROR_ID = errorIdFor('test-door')

export default function TestDoor({ next }: { next: string }) {
  const [signingIn, setSigningIn] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    setSigningIn(true)
    setError(null)
    const answer = await signInThroughTestDoor(String(form.get('email') ?? ''), String(form.get('password') ?? ''))
    if (answer === 'signed-in') {
      window.location.assign(next)
      return
    }
    setSigningIn(false)
    setError(answer === 'refused' ? REFUSED : UNREACHABLE)
  }

  const invalid = error === REFUSED ? { 'aria-invalid': true, 'aria-describedby': ERROR_ID } : {}

  return (
    <section
      aria-label="Test door"
      className="mx-bezel mt-5.5 rounded-core border border-dashed border-on-surface-variant bg-surface-container p-card"
    >
      <span className="mb-3 inline-flex rounded-full bg-placeholder px-3 py-1.5 text-eyebrow font-semibold text-on-surface uppercase">
        Dev only
      </span>
      <p className="mb-4 text-caption text-pretty">The test member&apos;s door. Production never serves it.</p>
      <form onSubmit={submit} className="grid gap-3.5">
        <div className="grid gap-2">
          <Label htmlFor="test-door-email">Test member email</Label>
          <Input id="test-door-email" name="email" type="email" autoComplete="username" readOnly={signingIn} {...invalid} />
        </div>
        <div className="grid gap-2">
          <Label htmlFor="test-door-password">Test member password</Label>
          <Input id="test-door-password" name="password" type="password" autoComplete="current-password" readOnly={signingIn} {...invalid} />
        </div>
        <div id={ERROR_ID}>
          <FormError message={error} />
        </div>
        <Button type="submit" variant="secondary" size="commit" disabled={signingIn} aria-busy={signingIn} className="w-full">
          {signingIn ? 'Signing in…' : 'Sign in as the test member'}
        </Button>
      </form>
    </section>
  )
}
