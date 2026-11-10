'use client'

import type { RateDraft } from '@/lib/draft-checks'
import type { Refusal } from '@/lib/worksheet-api'
import { placeRefusals, RATE_FIELDS } from '@/lib/refusal-copy'
import { CurrencyField } from '@/components/currency-field'
import { describedByError, FormError, FormField } from '@/components/form-field'
import { useFocusOnTheFirstRefusal } from '@/components/focus-the-first-refusal'
import { Input } from '@/components/ui/input'
import { NestedCtaButton } from '@/components/ui/nested-cta'

export function RateForm({
  choices,
  draft,
  refusals,
  onDraftChange,
  onKeep,
}: {
  choices: string[]
  draft: RateDraft
  refusals: Refusal[]
  onDraftChange: (draft: RateDraft) => void
  onKeep: () => void
}) {
  const { besideField, onTheForm } = placeRefusals(refusals, RATE_FIELDS)
  const change = (part: Partial<RateDraft>) => onDraftChange({ ...draft, ...part })
  useFocusOnTheFirstRefusal(refusals, RATE_FIELDS)

  return (
    <form
      noValidate
      aria-label="Your dated rate"
      onSubmit={(event) => {
        event.preventDefault()
        onKeep()
      }}
    >
      <div className="grid gap-x-5.5 md:grid-cols-2">
        <CurrencyField
          fieldId="from"
          label="From"
          choices={choices}
          value={draft.from}
          error={besideField.from}
          onChoose={(from) => change({ from })}
        />
        <CurrencyField
          fieldId="to"
          label="To"
          choices={choices}
          value={draft.to}
          error={besideField.to}
          onChoose={(to) => change({ to })}
        />
      </div>
      <FormField fieldId="rate" label="Rate" error={besideField.rate}>
        <Input
          id="rate"
          name="rate"
          inputMode="decimal"
          autoComplete="off"
          required
          value={draft.rate}
          onChange={(event) => change({ rate: event.target.value })}
          {...describedByError('rate', besideField.rate)}
        />
      </FormField>
      <FormField fieldId="ratedAt" label="Rate date" error={besideField.ratedAt}>
        <Input
          id="ratedAt"
          name="ratedAt"
          type="datetime-local"
          required
          value={draft.ratedAt}
          onChange={(event) => change({ ratedAt: event.target.value })}
          {...describedByError('ratedAt', besideField.ratedAt)}
        />
      </FormField>
      <FormError message={onTheForm} />
      <NestedCtaButton>Keep rate</NestedCtaButton>
    </form>
  )
}
