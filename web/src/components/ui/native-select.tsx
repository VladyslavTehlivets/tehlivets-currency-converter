import * as React from "react"

import { cn } from "@/lib/utils"
import { fieldSurface } from "@/components/ui/field-surface"
import { aThumbTall } from "@/components/ui/thumb-reach"

function ChoiceChevron() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="pointer-events-none absolute right-4 size-4 text-on-surface-variant"
    >
      <path d="m4 6 4 4 4-4" />
    </svg>
  )
}

function NativeSelect({ className, children, ...props }: React.ComponentProps<"select">) {
  return (
    <span className={cn("relative inline-flex w-full min-w-0 items-center", className)}>
      <select
        data-slot="native-select"
        className={cn(fieldSurface, aThumbTall, "appearance-none py-3 pr-10 pl-4")}
        {...props}
      >
        {children}
      </select>
      <ChoiceChevron />
    </span>
  )
}

export { NativeSelect }
