export function Eyebrow({ children }: { children: React.ReactNode }) {
  return (
    <span className="mb-5.5 inline-flex items-center gap-2 rounded-full bg-white/55 px-3.25 py-1.75 text-eyebrow font-semibold text-on-primary-container uppercase shadow-highlight before:size-1.5 before:rounded-full before:bg-on-primary-container before:content-['']">
      {children}
    </span>
  )
}
