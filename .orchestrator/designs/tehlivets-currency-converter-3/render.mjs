import { createRequire } from "node:module"
import { mkdirSync, writeFileSync } from "node:fs"
import { dirname, join } from "node:path"
import { fileURLToPath, pathToFileURL } from "node:url"

const playwrightHome = process.env.PLAYWRIGHT_FROM ?? "/home/vtehpi/projects/tehlivets-currency-converter/web/package.json"
const { chromium } = createRequire(playwrightHome)("@playwright/test")

const here = dirname(fileURLToPath(import.meta.url))
const door = pathToFileURL(join(here, "door.html")).href
const shots = join(here, "screens")
mkdirSync(shots, { recursive: true })

const WIDTHS = [
  { width: 390, height: 844 },
  { width: 1440, height: 900 },
  { width: 320, height: 640 },
]

const browser = await chromium.launch()
const findings = []

const sceneIds = await (async () => {
  const page = await browser.newPage()
  await page.goto(door)
  const ids = await page.$$eval("iframe", (frames) => frames.map((frame) => new URL(frame.src).searchParams.get("scene")))
  await page.close()
  return ids
})()

for (const id of sceneIds) {
  for (const viewport of WIDTHS) {
    const page = await browser.newPage({ viewport, deviceScaleFactor: 1, reducedMotion: "reduce" })
    await page.goto(`${door}?scene=${id}`)
    await page.evaluate(() => document.fonts.ready)
    await page.waitForTimeout(150)
    const measured = await page.evaluate(() => {
      const sideways = document.documentElement.scrollWidth - document.documentElement.clientWidth
      const small = [...document.querySelectorAll("button, a, input:not([type=hidden])")].flatMap((element) => {
        const box = element.getBoundingClientRect()
        if (box.width === 0) return []
        const reach = getComputedStyle(element, "::after")
        const reachWidth = reach.content === "none" ? 0 : parseFloat(reach.width) || 0
        const reachHeight = reach.content === "none" ? 0 : parseFloat(reach.height) || 0
        const width = Math.max(box.width, reachWidth)
        const height = Math.max(box.height, reachHeight)
        return width + 0.5 < 44 || height + 0.5 < 44 ? [`${element.tagName.toLowerCase()} "${element.textContent.trim() || element.id}" ${width.toFixed(0)}x${height.toFixed(0)}`] : []
      })
      const tooSmallType = [...document.querySelectorAll("input:not([type=hidden])")].filter((input) => parseFloat(getComputedStyle(input).fontSize) < 16).map((input) => input.id)
      const doors = [...document.querySelectorAll(".door")].map((door) => {
        const box = door.getBoundingClientRect()
        return `${door.dataset.door} ${box.width.toFixed(0)}x${box.height.toFixed(0)} top ${box.top.toFixed(0)} font ${getComputedStyle(door).fontFamily.split(",")[0]}`
      })
      const clipped = [...document.querySelectorAll("button")].filter((element) => element.scrollWidth > element.clientWidth + 1).map((element) => element.textContent.trim())
      return { sideways, small, tooSmallType, doors, clipped, pageHeight: document.documentElement.scrollHeight }
    })
    findings.push({ id, width: viewport.width, ...measured })
    if (viewport.width !== 320) await page.screenshot({ path: join(shots, `${id}-${viewport.width}.png`), fullPage: true })
    await page.close()
  }
}

await browser.close()
writeFileSync(join(shots, "measurements.json"), JSON.stringify(findings, null, 2))
const failures = findings.filter((finding) => finding.sideways > 0 || finding.small.length || finding.tooSmallType.length || finding.clipped.length)
console.log(JSON.stringify({ renders: findings.length, failures }, null, 2))
for (const finding of findings) console.log(finding.id, finding.width, finding.doors.join(" | "), "height", finding.pageHeight)
