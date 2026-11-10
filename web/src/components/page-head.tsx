import { Eyebrow } from '@/components/eyebrow'

export function PageHead({
  title,
  lede,
  action,
}: {
  title: string
  lede: string
  action?: React.ReactNode
}) {
  return (
    <header className="mt-3 mb-7 flex flex-col items-start gap-5.5 md:mb-9.5 md:flex-row md:items-end md:justify-between">
      <div>
        <Eyebrow>Follow the amount. Understand the rate.</Eyebrow>
        <h1 className="max-w-[24ch] text-page-title">{title}</h1>
        <p className="mt-3.5">{lede}</p>
      </div>
      {action}
    </header>
  )
}
