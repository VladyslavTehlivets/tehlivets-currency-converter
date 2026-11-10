import * as React from "react"

import { cn } from "@/lib/utils"
import { fieldSurface } from "@/components/ui/field-surface"
import { aThumbTall } from "@/components/ui/thumb-reach"

function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(fieldSurface, aThumbTall, "px-4 py-3 disabled:pointer-events-none", className)}
      {...props}
    />
  )
}

export { Input }
