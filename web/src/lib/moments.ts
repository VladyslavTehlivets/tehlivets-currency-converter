const LOCAL_INPUT_VALUE = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/

const twoDigits = (value: number) => String(value).padStart(2, '0')

function localInputOf(when: Date): string {
  return `${String(when.getFullYear()).padStart(4, '0')}-${twoDigits(when.getMonth() + 1)}-${twoDigits(when.getDate())}T${twoDigits(when.getHours())}:${twoDigits(when.getMinutes())}`
}

function offsetOf(when: Date): string {
  const eastOfUtc = -when.getTimezoneOffset()
  const sign = eastOfUtc < 0 ? '-' : '+'
  const minutes = Math.abs(eastOfUtc)
  return `${sign}${twoDigits(Math.floor(minutes / 60))}:${twoDigits(minutes % 60)}`
}

export function momentFromLocalInput(localInputValue: string): string | null {
  if (!LOCAL_INPUT_VALUE.test(localInputValue)) return null
  const when = new Date(localInputValue)
  if (Number.isNaN(when.getTime())) return null
  return `${localInputOf(when)}:${twoDigits(when.getSeconds())}${offsetOf(when)}`
}

export function localInputFromMoment(moment: string | null): string | null {
  if (!moment) return null
  const when = new Date(moment)
  return Number.isNaN(when.getTime()) ? null : localInputOf(when)
}

export function localInputNow(): string {
  return localInputOf(new Date())
}

const fullDate = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'long', year: 'numeric' })
const shortDate = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
const clockTime = new Intl.DateTimeFormat('en-GB', { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' })

export function longDate(moment: string): string {
  return fullDate.format(new Date(moment))
}

export function longMoment(moment: string): string {
  return `${longDate(moment)} · ${clockTime.format(new Date(moment))}`
}

export function shortMoment(moment: string): string {
  return `${shortDate.format(new Date(moment))}, ${clockTime.format(new Date(moment))}`
}
