import type { Metadata, Viewport } from 'next'

import { CANVAS_COLOUR } from '@/lib/browser-chrome'
import { ColourMesh } from '@/components/shell/colour-mesh'
import { SiteFooter } from '@/components/shell/site-footer'
import { SiteHeader } from '@/components/shell/site-header'
import { SkipLink } from '@/components/ui/skip-link'
import './globals.css'

const MAIN_CONTENT = 'main'

export const metadata: Metadata = {
  title: { template: '%s · Currency Converter', default: 'Currency Converter' },
  description: 'Convert an amount through dated rates you keep, and follow every step through PLN.',
}

export const viewport: Viewport = {
  themeColor: CANVAS_COLOUR,
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <head>
        <link
          rel="preload"
          href="/fonts/instrument-sans-latin-400-600.woff2"
          as="font"
          type="font/woff2"
          crossOrigin=""
        />
      </head>
      <body>
        <SkipLink target={MAIN_CONTENT}>Skip to content</SkipLink>
        <ColourMesh />
        <div className="mx-auto max-w-290 px-4.5 pt-4 pb-9 md:px-7 md:pt-6 md:pb-16 lg:px-11">
          <SiteHeader />
          <main id={MAIN_CONTENT} tabIndex={-1} className="focus-visible:outline-none">
            {children}
          </main>
          <SiteFooter />
        </div>
      </body>
    </html>
  )
}
