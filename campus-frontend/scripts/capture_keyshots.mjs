import { chromium } from 'playwright';
import fs from 'node:fs/promises';
import path from 'node:path';

const base = 'http://127.0.0.1:5173';
const outDir = '/Users/yy/code/OSP/docs/ppt_assets/keyshots';

async function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

async function shot(page, name) {
  const target = path.join(outDir, name);
  await page.screenshot({ path: target, fullPage: false });
  console.log('saved', target);
}

async function login(page) {
  await page.goto(base + '/login', { waitUntil: 'domcontentloaded' });
  await sleep(1200);
  await shot(page, '01_login.png');

  await page.getByPlaceholder('请输入用户名').fill('testuser123');
  await page.getByPlaceholder('请输入密码').fill('123456');
  await page.keyboard.press('Enter');
  await sleep(2600);
  if (page.url().includes('/login')) {
    const btn = page.getByRole('button', { name: /登/ });
    if (await btn.count()) {
      await btn.first().click();
      await sleep(2600);
    }
  }
}

async function closeDialogs(page) {
  const closeBtns = page.locator('.el-message-box__headerbtn, .el-dialog__headerbtn');
  const n = await closeBtns.count();
  for (let i = 0; i < Math.min(3, n); i++) {
    try { await closeBtns.nth(i).click({ timeout: 300 }); } catch {}
  }
}

async function capture() {
  await fs.mkdir(outDir, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const page = await context.newPage();

  await login(page);
  await closeDialogs(page);

  await page.goto(base + '/', { waitUntil: 'domcontentloaded' });
  await sleep(1800);
  await shot(page, '02_dashboard.png');

  await page.goto(base + '/agent', { waitUntil: 'domcontentloaded' });
  await sleep(1800);
  const quick = page.getByRole('button', { name: '图书馆怎么走' });
  if (await quick.count()) {
    await quick.first().click();
    await sleep(9000);
  }
  await shot(page, '03_agent_chat.png');

  await page.goto(base + '/secondhand?tab=market', { waitUntil: 'domcontentloaded' });
  await sleep(2200);
  await shot(page, '04_secondhand.png');

  await page.goto(base + '/lostfound/lost-board', { waitUntil: 'domcontentloaded' });
  await sleep(2200);
  await shot(page, '05_lostfound.png');

  await page.goto(base + '/messages', { waitUntil: 'domcontentloaded' });
  await sleep(2200);
  const conv = page.locator('.conversation-item,.conversation-list .conversation,.conversation-list .item,.message-item').first();
  if (await conv.count()) {
    try { await conv.click({ timeout: 1200 }); await sleep(1200); } catch {}
  }
  await shot(page, '06_messages.png');

  await page.goto(base + '/dormitory/repair', { waitUntil: 'domcontentloaded' });
  await sleep(2200);
  const applyBtn = page.getByRole('button', { name: /申请报修/ });
  if (await applyBtn.count()) {
    try { await applyBtn.first().click(); await sleep(1000); } catch {}
  }
  await shot(page, '07_dorm_repair.png');

  await page.goto(base + '/profile', { waitUntil: 'domcontentloaded' });
  await sleep(2200);
  await shot(page, '08_profile.png');

  await context.close();
  await browser.close();
}

capture().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
