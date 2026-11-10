'use client'

import { useEffect } from 'react'

import type { Refusal } from '@/lib/worksheet-api'
import { placeRefusals, type FormFieldName } from '@/lib/refusal-copy'

export function useFocusOnTheFirstRefusal(
  refusals: readonly Refusal[],
  fieldsInOrder: readonly FormFieldName[],
) {
  useEffect(() => {
    const { besideField } = placeRefusals(refusals, fieldsInOrder)
    const firstRefused = fieldsInOrder.find((field) => besideField[field])
    if (firstRefused) document.getElementById(firstRefused)?.focus()
  }, [refusals, fieldsInOrder])
}
