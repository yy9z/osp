import fs from 'node:fs/promises'
import path from 'node:path'
import { createRequire } from 'node:module'

const requireFromFrontend = createRequire('/Users/yy/code/OSP/campus-frontend/package.json')
const { chromium } = requireFromFrontend('playwright')

const BASE_URL = 'http://localhost:5173'
const OUTPUT_DIR =
  '/Users/yy/code/OSP/outputs/019ec9ed-2293-7b83-9b0c-0c821d22a3e9/presentations/campus-final-defense/assets/live'

async function login(page) {
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' })
  await page.getByPlaceholder('请输入用户名').fill('testuser123')
  await page.getByPlaceholder('请输入密码').fill('123456')
  await page.getByRole('button', { name: '登 录' }).click()
  await page.waitForURL(`${BASE_URL}/`, { timeout: 15000 })
  await page.waitForTimeout(1200)
}

async function closeOverlays(page) {
  const closeButtons = page.locator('.el-message-box__headerbtn, .el-dialog__headerbtn')
  const count = await closeButtons.count()
  for (let index = 0; index < count; index += 1) {
    try {
      await closeButtons.nth(index).click({ timeout: 300 })
    } catch {
      // Ignore stale or already closed overlays.
    }
  }
}

async function capture(page, route, fileName, waitMs = 1600) {
  await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(waitMs)
  await closeOverlays(page)
  await page.screenshot({
    path: path.join(OUTPUT_DIR, fileName),
    fullPage: false,
  })
}

async function captureAgentHistory(page, title, fileName) {
  await page.goto(`${BASE_URL}/agent`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1400)
  const history = page.getByRole('button', { name: new RegExp(`^${title}`) })
  if ((await history.count()) === 0) {
    throw new Error(`Agent history not found: ${title}`)
  }
  await history.first().click()
  await page.waitForTimeout(900)
  await page.screenshot({
    path: path.join(OUTPUT_DIR, fileName),
    fullPage: false,
  })
}

async function main() {
  await fs.mkdir(OUTPUT_DIR, { recursive: true })
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
    deviceScaleFactor: 1,
  })
  const page = await context.newPage()

  await login(page)
  await capture(page, '/', '01_dashboard.png')
  await captureAgentHistory(page, '查看我的待办提醒', '02_agent_tips.png')
  await captureAgentHistory(page, '宿舍空调坏了帮我报修', '03_agent_repair.png')
  await capture(page, '/secondhand?tab=market', '04_secondhand.png', 2600)
  await capture(page, '/lostfound/lost-board', '05_lostfound.png', 2200)
  await capture(page, '/messages', '06_messages.png', 1800)
  await capture(page, '/dormitory/repair', '07_dorm_repair.png', 1800)
  await capture(page, '/navigation', '08_navigation.png', 5200)

  await context.close()
  await browser.close()
  console.log(`Saved clean browser screenshots to ${OUTPUT_DIR}`)
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
