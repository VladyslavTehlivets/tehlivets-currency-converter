import Link from 'next/link'

import { cn } from '@/lib/utils'
import { reachesAThumb } from '@/components/ui/thumb-reach'
import { PRIMARY_LINKS } from '@/components/shell/primary-links'

export function SiteFooter() {
  return (
    <footer className="mt-12.5 flex flex-col items-start justify-between gap-5 border-t border-rule py-7.5 text-fine text-on-surface-variant md:mt-22.5 md:flex-row md:items-center">
      <span>Currency Converter · Made with care.</span>
      <nav aria-label="Footer" className="flex flex-wrap gap-5.5">
        {PRIMARY_LINKS.map((link) => (
          <Link key={link.href} href={link.href} className={cn(reachesAThumb, 'whitespace-nowrap')}>
            {link.label}
          </Link>
        ))}
      </nav>
    </footer>
  )
}
