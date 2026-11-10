'use client'

import { useRouter, useSearchParams } from 'next/navigation'
import { useEffect, useMemo, useState } from 'react'

import { conversionOutcomeOf, type ConversionOutcome } from '@/lib/conversion-outcome'
import { conversionRequestIn, converterHref, rateAddHref } from '@/lib/routes'
import { convert } from '@/lib/worksheet-api'
import { Bezel } from '@/components/bezel'
import { ConversionReceiptView } from '@/components/conversion-receipt'
import { FindingYourRate } from '@/components/finding-your-rate'
import { HowToBegin } from '@/components/how-to-begin'
import { MissingRate } from '@/components/missing-rate'
import { Notice } from '@/components/notice'
import { PageHead } from '@/components/page-head'
import { Button } from '@/components/ui/button'
import { NestedCtaLink } from '@/components/ui/nested-cta'

export function ConversionResult() {
  const query = useSearchParams()
  const router = useRouter()
  const request = useMemo(() => conversionRequestIn(query), [query])
  const [outcome, setOutcome] = useState<ConversionOutcome | null>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let stillWanted = true
    setOutcome(null)
    convert(request).then((answer) => {
      if (stillWanted) setOutcome(conversionOutcomeOf(answer))
    })
    return () => {
      stillWanted = false
    }
  }, [request, attempt])

  useEffect(() => {
    if (outcome?.kind === 'field-refusal') router.replace(converterHref(request, outcome.refusal))
  }, [outcome, request, router])

  if (outcome === null || outcome.kind === 'field-refusal') return <FindingYourRate />

  switch (outcome.kind) {
    case 'receipt':
      return <ConversionReceiptView receipt={outcome.receipt} anotherConversionHref={converterHref(request)} />
    case 'how-to-begin':
      return <HowToBegin />
    case 'missing-rate':
      return (
        <MissingRate
          missing={outcome.missing}
          addTheRateHref={rateAddHref(outcome.missing, outcome.at ?? request.at)}
          changeTheConversionHref={converterHref(request)}
        />
      )
    case 'trouble':
      return (
        <>
          <PageHead title="No result yet." lede="Your amount stays here." />
          <Bezel>
            <Notice tone="trouble" role="alert">
              {outcome.message}
            </Notice>
            <div className="flex flex-wrap gap-3.5">
              <Button size="commit" onClick={() => setAttempt((tried) => tried + 1)}>
                Try again
              </Button>
              <NestedCtaLink tone="secondary" href={converterHref(request)}>
                Change the conversion
              </NestedCtaLink>
            </div>
          </Bezel>
        </>
      )
  }
}
