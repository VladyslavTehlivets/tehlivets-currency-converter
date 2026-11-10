import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { Slot } from "radix-ui"

import { cn } from "@/lib/utils"
import { reachesAThumb } from "@/components/ui/thumb-reach"
import {
  aControlAt,
  aSquareControlAt,
  CONTROL_STEPS,
  flushWithTheTextBesideIt,
  type AFlushControlTakesTheRegisterOfItsText,
} from "@/components/ui/control-scale"

const squaredAtEveryStep = CONTROL_STEPS.map((size) => ({
  shape: "icon" as const,
  size,
  class: aSquareControlAt[size],
}))

const buttonVariants = cva(
  `group/button ${reachesAThumb} inline-flex shrink-0 cursor-pointer items-center justify-center rounded-full font-semibold whitespace-nowrap transition-[transform,opacity,background-color] duration-(--duration-control) ease-spring select-none active:scale-97 disabled:pointer-events-none disabled:opacity-60 [&_svg]:pointer-events-none [&_svg]:shrink-0`,
  {
    variants: {
      variant: {
        secondary: "bg-white/60 text-on-surface shadow-cta-quiet hover:bg-white",
        muted: "bg-field text-on-surface shadow-highlight hover:bg-white",
      },
      size: aControlAt,
      shape: {
        text: "",
        flush: flushWithTheTextBesideIt,
        icon: "",
      },
    },
    compoundVariants: squaredAtEveryStep,
    defaultVariants: {
      variant: "secondary",
      size: "inline",
      shape: "text",
    },
  }
)

type ButtonProps = React.ComponentProps<"button"> &
  Omit<VariantProps<typeof buttonVariants>, "size" | "shape"> &
  AFlushControlTakesTheRegisterOfItsText<"text" | "flush" | "icon"> & {
    asChild?: boolean
  }

function Button({
  className,
  variant = "secondary",
  size = "inline",
  shape = "text",
  asChild = false,
  ...props
}: ButtonProps) {
  const Comp = asChild ? Slot.Root : "button"

  return (
    <Comp
      data-slot="button"
      data-variant={variant}
      data-size={size}
      data-shape={shape}
      className={cn(buttonVariants({ variant, size, shape, className }))}
      {...props}
    />
  )
}

export { Button, buttonVariants }
