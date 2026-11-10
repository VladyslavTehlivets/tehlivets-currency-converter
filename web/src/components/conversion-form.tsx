'use client'

import type { ConversionDraft } from '@/lib/draft-checks'
import type { Refusal } from '@/lib/worksheet-api'
import { CONVERSION_FIELDS, placeRefusals } from '@/lib/refusal-copy'
import { CurrencyField } from '@/components/currency-field'
import { describedByError, FormError, FormField } from '@/components/form-field'
import { useFocusOnTheFirstRefusal } from '@/components/focus-the-first-refusal'
import { Input } from '@/components/ui/input'
import { NestedCtaButton } from '@/components/ui/nested-cta'

export function ConversionForm({
  choices,
  draft,
  refusals,
  onDraftChange,
  onConvert,
}: {
  choices: string[]
  draft: ConversionDraft
  refusals: Refusal[]
  onDraftChange: (draft: ConversionDraft) => void
  onConvert: () => void
}) {
  const { besideField, onTheForm } = placeRefusals(refusals, CONVERSION_FIELDS)
  const change = (part: Partial<ConversionDraft>) => onDraftChange({ ...draft, ...part })
  useFocusOnTheFirstRefusal(refusals, CONVERSION_FIELDS)

  return (
    <form
      noValidate
      aria-label="Your conversion"
      onSubmit={(event) => {
        event.preventDefault()
        onConvert()
      }}
    >
      <FormField fieldId="amount" label="Amount" error={besideField.amount}>
        <Input
          id="amount"
          name="amount"
          inputMode="decimal"
          autoComplete="off"
          required
          value={draft.amount}
          onChange={(event) => change({ amount: event.target.value })}
          {...describedByError('amount', besideField.amount)}
        />
      </FormField>
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
      <FormField fieldId="at" label="Conversion date" error={besideField.at}>
        <Input
          id="at"
          name="at"
          type="datetime-local"
          required
          value={draft.at}
          onChange={(event) => change({ at: event.target.value })}
          {...describedByError('at', besideField.at)}
        />
      </FormField>
      <FormError message={onTheForm} />
      <NestedCtaButton>Convert amount</NestedCtaButton>
    </form>
  )
}
