import * as React from "react"
import Link from "next/link"
import { cva, type VariantProps } from "class-variance-authority"

import { cn } from "@/lib/utils"

const nestedCtaVariants = cva(
  "group/cta inline-flex min-h-12.5 shrink-0 cursor-pointer items-center justify-between gap-3.5 rounded-full py-1.5 pr-1.5 pl-5.5 text-label-lg font-semibold whitespace-nowrap transition-[transform,opacity] duration-(--duration-control) ease-spring active:scale-97 disabled:cursor-default disabled:opacity-60 md:gap-5.5",
  {
    variants: {
      tone: {
        primary: "bg-primary text-on-primary shadow-cta",
        secondary: "bg-white/60 text-on-surface shadow-cta-quiet",
      },
    },
    defaultVariants: { tone: "primary" },
  }
)

type NestedCtaTone = VariantProps<typeof nestedCtaVariants>

function TrailingGlyph() {
  return (
    <span
      aria-hidden="true"
      className="grid size-9.5 place-items-center rounded-full bg-white/35 text-xl font-normal shadow-glyph transition-transform duration-(--duration-glyph) ease-spring group-hover/cta:translate-x-0.5 group-hover/cta:-translate-y-0.5"
    >
      ↗
    </span>
  )
}

function NestedCtaLink({
  className,
  tone,
  children,
  ...props
}: React.ComponentProps<typeof Link> & NestedCtaTone) {
  return (
    <Link data-slot="nested-cta" className={cn(nestedCtaVariants({ tone }), className)} {...props}>
      {children}
      <TrailingGlyph />
    </Link>
  )
}

function NestedCtaButton({
  className,
  tone,
  children,
  type = "submit",
  ...props
}: React.ComponentProps<"button"> & NestedCtaTone) {
  return (
    <button
      data-slot="nested-cta"
      type={type}
      className={cn(nestedCtaVariants({ tone }), className)}
      {...props}
    >
      {children}
      <TrailingGlyph />
    </button>
  )
}

export { NestedCtaButton, NestedCtaLink, nestedCtaVariants }
