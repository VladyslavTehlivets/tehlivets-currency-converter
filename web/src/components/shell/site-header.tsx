'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useId, useRef, useState } from 'react'

import { cn } from '@/lib/utils'
import { CONVERT_ROUTE } from '@/lib/routes'
import { Button } from '@/components/ui/button'
import { NestedCtaLink } from '@/components/ui/nested-cta'
import { reachesAThumb } from '@/components/ui/thumb-reach'
import { PRIMARY_LINKS } from '@/components/shell/primary-links'

export function SiteHeader() {
  const currentRoute = usePathname()
  const [menuIsOpen, setMenuIsOpen] = useState(false)
  const menuToggle = useRef<HTMLButtonElement>(null)
  const navigationId = useId()

  const closeMenuAndReturnFocus = (event: React.KeyboardEvent) => {
    if (event.key !== 'Escape' || !menuIsOpen) return
    setMenuIsOpen(false)
    menuToggle.current?.focus()
  }

  return (
    <header
      onKeyDown={closeMenuAndReturnFocus}
      className="relative mx-auto mb-11.5 flex w-full max-w-full items-center justify-between gap-2.5 rounded-full bg-glass py-2 pr-2.5 pl-4 shadow-island md:mb-18 md:w-fit md:justify-start md:gap-5 md:py-2.5 md:pr-3 md:pl-5.5 lg:gap-7.5"
    >
      <Link
        href={CONVERT_ROUTE}
        className={cn(reachesAThumb, 'flex items-center gap-2.5 font-display text-brand font-semibold whitespace-nowrap')}
      >
        <span
          aria-hidden="true"
          className="inline-grid size-7.75 place-items-center rounded-xs bg-primary text-lg text-on-primary shadow-highlight"
        >
          c
        </span>
        Currency Converter
      </Link>
      <nav
        id={navigationId}
        aria-label="Primary"
        className={cn(
          'text-caption md:flex md:gap-3.75 lg:gap-6',
          'max-md:absolute max-md:top-17.5 max-md:right-0 max-md:left-0 max-md:z-20 max-md:flex-col max-md:gap-1.25 max-md:rounded-menu max-md:bg-menu max-md:p-5.5 max-md:shadow-menu',
          menuIsOpen ? 'max-md:flex' : 'max-md:hidden',
        )}
      >
        {PRIMARY_LINKS.map((link) => (
          <Link
            key={link.href}
            href={link.href}
            aria-current={currentRoute === link.href ? 'page' : undefined}
            onClick={() => setMenuIsOpen(false)}
            className={cn(
              reachesAThumb,
              'whitespace-nowrap aria-[current=page]:text-on-primary-container max-md:p-3',
            )}
          >
            {link.label}
          </Link>
        ))}
      </nav>
      <NestedCtaLink href={CONVERT_ROUTE} className="max-md:hidden">
        Explore
      </NestedCtaLink>
      <Button
        ref={menuToggle}
        variant="muted"
        size="commit"
        shape="icon"
        aria-controls={navigationId}
        aria-expanded={menuIsOpen}
        aria-label="Menu"
        onClick={() => setMenuIsOpen((isOpen) => !isOpen)}
        className="md:hidden"
      >
        <span aria-hidden="true">☰</span>
      </Button>
    </header>
  )
}
