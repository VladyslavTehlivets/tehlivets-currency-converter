export function SkipLink({ target, children }: { target: string; children: React.ReactNode }) {
  return (
    <a
      href={`#${target}`}
      className="fixed top-0 left-3 z-50 -translate-y-full rounded-node bg-white px-4.5 py-3 text-on-surface shadow-island focus:translate-y-2"
    >
      {children}
    </a>
  )
}
