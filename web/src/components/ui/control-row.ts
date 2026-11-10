import { aThumbTall } from "@/components/ui/thumb-reach"
import { CONTROL_STEPS, heightAt, squareAt, type ControlStep } from "@/components/ui/control-scale"

const pixelsIn = (utility: string) => Number(utility.split("-").at(-1)) * 4

export const aThumb = pixelsIn(aThumbTall)

const overhangOf = (painted: string) => Math.max(0, aThumb - pixelsIn(painted))

export const overhangAt = Object.fromEntries(
  CONTROL_STEPS.map((step) => [
    step,
    { down: overhangOf(heightAt[step]), across: overhangOf(squareAt[step]) },
  ]),
) as Record<ControlStep, { down: number; across: number }>

export const aRowOfControls = "flex gap-2 overflow-x-auto py-1.5"

export const aWrappingRowOfControls = "flex flex-wrap gap-3"

export const rowsAThumbApartAt: Record<ControlStep, string> = {
  commit: "flex flex-col gap-2",
  inline: "flex flex-col gap-3",
  quiet: "flex flex-col gap-4",
}
