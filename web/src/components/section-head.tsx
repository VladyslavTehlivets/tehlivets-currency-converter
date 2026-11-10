export function SectionHead({ title, lede }: { title: string; lede: string }) {
  return (
    <div className="mb-8.75 md:flex md:items-end md:justify-between md:gap-7.5">
      <h2 className="text-headline-lg">{title}</h2>
      <p className="mt-4.5 max-w-[36ch] md:mt-0">{lede}</p>
    </div>
  )
}
