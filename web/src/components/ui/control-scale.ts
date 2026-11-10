import { aThumbSquare, aThumbTall } from "@/components/ui/thumb-reach"

export const CONTROL_STEPS = ["commit", "inline", "quiet"] as const

export type ControlStep = (typeof CONTROL_STEPS)[number]

export type QuieterThanACommit = Exclude<ControlStep, "commit">

export type AFlushControlTakesTheRegisterOfItsText<Shape extends string> =
  | { shape?: Exclude<Shape, "flush">; size?: ControlStep }
  | { shape: "flush"; size?: QuieterThanACommit }

export const heightAt: Record<ControlStep, string> = {
  commit: aThumbTall,
  inline: "min-h-8",
  quiet: "min-h-7",
}

export const typeSizeAt: Record<ControlStep, string> = {
  commit: "text-base",
  inline: "text-sm",
  quiet: "text-xs",
}

const gapAt: Record<ControlStep, string> = {
  commit: "gap-2",
  inline: "gap-1.5",
  quiet: "gap-1",
}

const sidePaddingAt: Record<ControlStep, string> = {
  commit: "px-4",
  inline: "px-2.5",
  quiet: "px-2",
}

export const squareAt: Record<ControlStep, string> = {
  commit: aThumbSquare,
  inline: "size-8",
  quiet: "size-7",
}

export const flushWithTheTextBesideIt = "px-0"

export const aShareOfItsRow = "flex-1 px-0"

export const roomForALabelThatWraps = "py-2"

function everyStep(sizedBy: (step: ControlStep) => string): Record<ControlStep, string> {
  return Object.fromEntries(
    CONTROL_STEPS.map((step) => [step, sizedBy(step)]),
  ) as Record<ControlStep, string>
}

export const aControlAt = everyStep(
  (step) => `${heightAt[step]} ${typeSizeAt[step]} ${gapAt[step]} ${sidePaddingAt[step]}`,
)

export const aSquareControlAt = everyStep(
  (step) => `${squareAt[step]} ${typeSizeAt[step]} ${gapAt[step]} p-0`,
)
