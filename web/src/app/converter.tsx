'use client'

import { useRouter, useSearchParams } from 'next/navigation'
import { useEffect, useState } from 'react'

import { headingForRefusedConversion } from '@/lib/converter-heading'
import { chosenPair, conversionChoices } from '@/lib/currency-choices'
import { checkConversionDraft, type ConversionDraft } from '@/lib/draft-checks'
import { localInputFromMoment, localInputNow, momentFromLocalInput } from '@/lib/moments'
import { THE_WORKSHEET_DID_NOT_ANSWER } from '@/lib/refusal-copy'
import { conversionRequestIn, RATES_ROUTE, refusalsIn, resultHref } from '@/lib/routes'
import { listRates, type Refusal } from '@/lib/worksheet-api'
import { Bezel } from '@/components/bezel'
import { ConversionForm } from '@/components/conversion-form'
import { Eyebrow } from '@/components/eyebrow'
import { FormWaiting } from '@/components/form-waiting'
import { Notice } from '@/components/notice'
import { PageHead } from '@/components/page-head'
import { SectionHead } from '@/components/section-head'
import { NestedCtaLink } from '@/components/ui/nested-cta'

const PREFERRED_PAIR = { from: 'USD', to: 'EUR' }

interface OpenForm {
  choices: string[]
  draft: ConversionDraft
}

function firstDraft(choices: string[], query: URLSearchParams): ConversionDraft {
  const asked = conversionRequestIn(query)
  return {
    amount: asked.amount,
    ...chosenPair(choices, { from: query.get('from'), to: query.get('to') }, PREFERRED_PAIR),
    at: localInputFromMoment(asked.at) ?? localInputNow(),
  }
}

export function Converter() {
  const query = useSearchParams()
  const router = useRouter()
  const [form, setForm] = useState<OpenForm | null>(null)
  const [trouble, setTrouble] = useState<string | null>(null)
  const [refusals, setRefusals] = useState<Refusal[]>(() => refusalsIn(query))

  useEffect(() => {
    let stillWanted = true
    listRates().then((answer) => {
      if (!stillWanted) return
      const choices = conversionChoices(answer.kind === 'answered' ? answer.value : [])
      setForm({ choices, draft: firstDraft(choices, query) })
      setTrouble(answer.kind === 'unreachable' || answer.kind === 'refused' ? THE_WORKSHEET_DID_NOT_ANSWER : null)
    })
    return () => {
      stillWanted = false
    }
  }, [query])

  const convertTheDraft = (draft: ConversionDraft) => {
    const checked = checkConversionDraft(draft)
    const at = momentFromLocalInput(draft.at)
    if (checked.length > 0 || at === null) return setRefusals(checked)
    router.push(resultHref({ amount: draft.amount, from: draft.from, to: draft.to, at }))
  }

  const workingSurface = (
    <>
      {trouble ? (
        <Notice tone="trouble" role="status">
          {trouble}
        </Notice>
      ) : null}
      {form ? (
        <ConversionForm
          choices={form.choices}
          draft={form.draft}
          refusals={refusals}
          onDraftChange={(draft) => setForm({ ...form, draft })}
          onConvert={() => convertTheDraft(form.draft)}
        />
      ) : (
        <FormWaiting label="Loading your conversion" />
      )}
    </>
  )

  const refusedHeading = headingForRefusedConversion(refusals)
  if (refusedHeading) {
    return (
      <>
        <PageHead title={refusedHeading.title} lede={refusedHeading.lede} />
        <Bezel>{workingSurface}</Bezel>
      </>
    )
  }

  return (
    <>
      <section className="grid items-center gap-11 pt-4 pb-10 md:min-h-142.5 md:grid-cols-[1.02fr_1fr] md:gap-8.75 md:pt-0 md:pb-25 lg:gap-17.5">
        <div className="min-w-0">
          <Eyebrow>A clear conversion worksheet</Eyebrow>
          <h1 className="max-w-[15ch] text-display-lg">
            Every amount. <br />
            A visible path.
          </h1>
          <p className="mt-5.5 max-w-[43ch] text-lead md:mt-6.5">
            Choose an amount and a date. Follow the rate selection and each step through the base currency.
          </p>
          <div className="mt-7.5 flex flex-wrap items-center gap-2.5 md:gap-3.5">
            <NestedCtaLink tone="secondary" href={RATES_ROUTE}>
              Review your rates
            </NestedCtaLink>
          </div>
        </div>
        <div className="relative isolate min-w-0 before:absolute before:inset-x-0 before:-inset-y-[12%] before:pointer-events-none before:-z-10 before:bg-(image:--gradient-halo) before:content-['']">
          <Bezel>
            <Eyebrow>Your conversion</Eyebrow>
            {workingSurface}
          </Bezel>
        </div>
      </section>
      <Notice>Your worksheet uses rates you provide. These are example rates, not current market quotes.</Notice>
      <section className="py-12 md:py-20">
        <SectionHead title="See what the number means." lede="Keep the amount, date, and selected rate together." />
        <div className="grid gap-5.5 md:grid-cols-2">
          <Bezel>
            <h3 className="text-title">A dated rate.</h3>
            <p className="mt-3">The worksheet selects the available rate nearest to the conversion date.</p>
          </Bezel>
          <Bezel>
            <h3 className="text-title">A visible route.</h3>
            <p className="mt-3">When both currencies differ from the base currency, the conversion passes through PLN.</p>
          </Bezel>
        </div>
      </section>
    </>
  )
}
