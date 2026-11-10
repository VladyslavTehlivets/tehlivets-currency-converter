'use client'

import { useRouter, useSearchParams } from 'next/navigation'
import { useMemo, useRef, useState } from 'react'

import { chosenPair, rateChoices } from '@/lib/currency-choices'
import { checkRateDraft, type RateDraft } from '@/lib/draft-checks'
import { localInputFromMoment, localInputNow, momentFromLocalInput } from '@/lib/moments'
import { RATES_ROUTE } from '@/lib/routes'
import { keepRate, type Refusal } from '@/lib/worksheet-api'
import { Bezel } from '@/components/bezel'
import { PageHead } from '@/components/page-head'
import { RateForm } from '@/components/rate-form'

const PREFERRED_DIRECTION = { from: 'USD', to: 'PLN' }

function firstDraft(choices: string[], query: URLSearchParams): RateDraft {
  return {
    ...chosenPair(choices, { from: query.get('from'), to: query.get('to') }, PREFERRED_DIRECTION),
    rate: '',
    ratedAt: localInputFromMoment(query.get('at')) ?? localInputNow(),
  }
}

export function AddARate() {
  const query = useSearchParams()
  const router = useRouter()
  const choices = useMemo(() => rateChoices(), [])
  const [draft, setDraft] = useState(() => firstDraft(choices, query))
  const [refusals, setRefusals] = useState<Refusal[]>([])
  const keeping = useRef(false)

  const keep = async () => {
    if (keeping.current) return
    const checked = checkRateDraft(draft)
    const ratedAt = momentFromLocalInput(draft.ratedAt)
    if (checked.length > 0 || ratedAt === null) return setRefusals(checked)
    keeping.current = true
    const answer = await keepRate({ from: draft.from, to: draft.to, rate: draft.rate, ratedAt })
    keeping.current = false
    if (answer.kind === 'answered') return router.push(RATES_ROUTE)
    setRefusals([answer.kind === 'refused' ? answer.refusal : { code: answer.kind }])
  }

  return (
    <>
      <PageHead title="Keep the rate with its date." lede="A direction, an amount, and a point in time." />
      <Bezel>
        <RateForm choices={choices} draft={draft} refusals={refusals} onDraftChange={setDraft} onKeep={keep} />
      </Bezel>
    </>
  )
}
