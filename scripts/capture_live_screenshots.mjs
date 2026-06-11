import fs from 'node:fs/promises'
import path from 'node:path'
import { createRequire } from 'node:module'

const requireFromFrontend = createRequire('/Users/yy/code/OSP/campus-frontend/package.json')
const { chromium } = requireFromFrontend('playwright')

const FRONTEND_URL = 'http://127.0.0.1:5173'
const BACKEND_URL = 'http://127.0.0.1:8080'
const OUTPUT_DIR = '/Users/yy/code/OSP/docs/ppt_assets/live_2026-04-15'

const LOGIN_CANDIDATES = [
  { username: 'testuser123', password: '123456' },
  { username: 'user', password: '123456' },
  { username: 'testuser', password: '123456' },
  { username: 'dorm_manager', password: '123456' },
  { username: 'root', password: '123456' }
]

async function ensureDir(dir) {
  await fs.mkdir(dir, { recursive: true })
}

async function loginByApi() {
  for (const credentials of LOGIN_CANDIDATES) {
    try {
      const response = await fetch(`${BACKEND_URL}/api/user/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(credentials)
      })

      const json = await response.json().catch(() => null)
      if (json?.code === 200 && json?.data?.token) {
        return json.data
      }
    } catch (error) {
      // Continue trying other candidates.
    }
  }

  throw new Error('No known account could log in to the live backend.')
}

async function capturePage(page, route, fileName, waitForMs = 2500) {
  await page.goto(`${FRONTEND_URL}${route}`, { waitUntil: 'networkidle' })
  if (waitForMs > 0) {
    await page.waitForTimeout(waitForMs)
  }
  await page.screenshot({
    path: path.join(OUTPUT_DIR, fileName),
    fullPage: true
  })
}

async function captureAgentScenario(page) {
  await page.goto(`${FRONTEND_URL}/agent`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(2000)

  const quickTip = page.locator('.quick-btns button').first()
  if (await quickTip.count()) {
    await quickTip.click()
    await page.waitForTimeout(12000)
    await page.screenshot({
      path: path.join(OUTPUT_DIR, '03_agent_live.png'),
      fullPage: true
    })
    return
  }

  await page.screenshot({
    path: path.join(OUTPUT_DIR, '03_agent_live.png'),
    fullPage: true
  })
}

async function main() {
  await ensureDir(OUTPUT_DIR)

  const loginData = await loginByApi()

  const browser = await chromium.launch({
    channel: 'chrome',
    headless: true
  })

  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    deviceScaleFactor: 1
  })

  const page = await context.newPage()

  await page.goto(`${FRONTEND_URL}/login`, { waitUntil: 'networkidle' })
  await page.evaluate((data) => {
    sessionStorage.setItem('token', data.token)
    sessionStorage.setItem('userInfo', JSON.stringify(data))
  }, loginData)

  await capturePage(page, '/', '01_dashboard_live.png')
  await capturePage(page, '/secondhand', '02_secondhand_live.png')
  await captureAgentScenario(page)
  await capturePage(page, '/lostfound/lost-board', '04_lostfound_live.png')
  await capturePage(page, '/dormitory/repair', '05_dorm_repair_live.png')
  await capturePage(page, '/navigation', '06_navigation_live.png', 4500)

  await browser.close()

  console.log(`Screenshots saved to ${OUTPUT_DIR}`)
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
