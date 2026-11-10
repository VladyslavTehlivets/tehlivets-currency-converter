const WORKSHEET_KEY_ENTRY = 'tehlivets-currency-converter.worksheet-key'

function browserStorage(): Storage | null {
  try {
    return globalThis.localStorage ?? null
  } catch {
    return null
  }
}

export function heldWorksheetKey(): string | null {
  return browserStorage()?.getItem(WORKSHEET_KEY_ENTRY) ?? null
}

export function holdWorksheetKey(worksheetKey: string): void {
  browserStorage()?.setItem(WORKSHEET_KEY_ENTRY, worksheetKey)
}

export function forgetWorksheetKey(): void {
  browserStorage()?.removeItem(WORKSHEET_KEY_ENTRY)
}
