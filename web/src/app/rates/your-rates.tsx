'use client'

import { useEffect, useRef, useState } from 'react'

import { rateBookAfterRemoval, rateBookOf, type RateBook } from '@/lib/rate-book'
import { METHOD_ROUTE, RATE_ADD_ROUTE } from '@/lib/routes'
import { listRates, removeRate, type DatedRate } from '@/lib/worksheet-api'
import { Bezel } from '@/components/bezel'
import { HowToBegin } from '@/components/how-to-begin'
import { ExampleRatesNotice, Notice } from '@/components/notice'
import { PageHead } from '@/components/page-head'
import { RateTable } from '@/components/rate-table'
import { SectionHead } from '@/components/section-head'
import { NestedCtaLink } from '@/components/ui/nested-cta'
import { Skeleton } from '@/components/ui/skeleton'

function RatesWaiting() {
  return (
    <div role="status" aria-label="Loading your rates">
      <Skeleton />
      <Skeleton />
      <Skeleton />
    </div>
  )
}

export function YourRates() {
  const [book, setBook] = useState<RateBook | null>(null)
  const rateTable = useRef<HTMLTableElement>(null)

  useEffect(() => {
    let stillWanted = true
    listRates().then((answer) => {
      if (stillWanted) setBook(rateBookOf(answer))
    })
    return () => {
      stillWanted = false
    }
  }, [])

  const remove = async (rate: DatedRate) => {
    const answer = await removeRate(rate.id)
    setBook((current) => (current?.kind === 'rates' ? rateBookAfterRemoval(current.rates, rate.id, answer) : current))
    rateTable.current?.focus()
  }

  if (book?.kind === 'how-to-begin') return <HowToBegin />

  return (
    <>
      <PageHead
        title="The rates behind the result."
        lede="Add dated rates for the directions you need."
        action={<NestedCtaLink href={RATE_ADD_ROUTE}>Add a rate</NestedCtaLink>}
      />
      <Bezel>
        {book === null ? <RatesWaiting /> : null}
        {book?.kind === 'trouble' ? (
          <Notice tone="trouble" role="alert">
            {book.message}
          </Notice>
        ) : null}
        {book?.kind === 'rates' ? (
          <>
            {book.trouble ? (
              <Notice tone="trouble" role="alert">
                {book.trouble}
              </Notice>
            ) : null}
            <RateTable ref={rateTable} rates={book.rates} onRemove={remove} />
            <ExampleRatesNotice />
          </>
        ) : null}
      </Bezel>
      <section className="py-12 md:py-20">
        <SectionHead title="Direction matters." lede="A rate from USD to PLN does not supply the reverse direction." />
        <NestedCtaLink tone="secondary" href={METHOD_ROUTE}>
          How selection works
        </NestedCtaLink>
      </section>
    </>
  )
}
