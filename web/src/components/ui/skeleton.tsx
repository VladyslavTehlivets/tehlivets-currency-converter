import { cn } from "@/lib/utils"

function Skeleton({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="skeleton"
      className={cn("my-5 h-5 max-w-full animate-breathe rounded-xs bg-placeholder", className)}
      {...props}
    />
  )
}

export { Skeleton }
