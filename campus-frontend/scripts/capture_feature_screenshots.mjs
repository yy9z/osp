import { chromium } from 'playwright';
import fs from 'node:fs/promises';
import path from 'node:path';

const base = process.env.BASE_URL || 'http://localhost:5173';
const agentPrompt = process.env.AGENT_PROMPT || '图书馆怎么走';
const outDir = '/Users/yy/code/OSP/docs/ppt_assets/screenshots';

const shots = [
  { name: '01_dashboard.png', route: '/' },
  { name: '02_agent.png', route: '/agent', post: async (page) => {
      const quick = page.getByRole('button', { name: agentPrompt });
      if (await quick.count()) {
        await quick.first().click();
        await page.waitForTimeout(800);
        return;
      }

      const input = page.getByPlaceholder('输入您的需求，例如：宿舍空调坏了帮我报修...');
      if (await input.count()) {
        await input.first().fill(agentPrompt);
        await page.getByRole('button', { name: '发送' }).first().click();
      }
    }
  },
  { name: '03_secondhand.png', route: '/secondhand?tab=market' },
  { name: '04_lostfound.png', route: '/lostfound/lost-board' },
  { name: '05_messages.png', route: '/messages' },
  { name: '06_dorm_info.png', route: '/dormitory/info' },
  { name: '07_dorm_repair.png', route: '/dormitory/repair' },
  { name: '08_navigation.png', route: '/navigation' },
  { name: '09_profile.png', route: '/profile' },
];

async function login(page) {
  await page.goto(base + '/login', { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(1200);
  await page.getByPlaceholder('请输入用户名').fill('testuser123');
  await page.getByPlaceholder('请输入密码').fill('123456');
  await page.keyboard.press('Enter');
  await page.waitForTimeout(2600);

  if (page.url().includes('/login')) {
    const btn = page.getByRole('button', { name: /登/ });
    if (await btn.count()) {
      await btn.first().click();
      await page.waitForTimeout(2600);
    }
  }

  if (page.url().includes('/login')) {
    throw new Error('登录失败，请检查测试账号或后端状态');
  }
}

async function hideOverlays(page) {
  const closeBtns = page.locator('.el-message-box__headerbtn, .el-dialog__headerbtn');
  const n = await closeBtns.count();
  for (let i = 0; i < Math.min(n, 3); i++) {
    try { await closeBtns.nth(i).click({ timeout: 300 }); } catch {}
  }
}

async function capture() {
  await fs.mkdir(outDir, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const page = await context.newPage();

  await login(page);
  await hideOverlays(page);

  for (const item of shots) {
    const url = base + item.route;
    await page.goto(url, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2400);
    await hideOverlays(page);
    if (item.post) {
      try { await item.post(page); } catch (e) { console.log('post hook fail', item.name, e.message); }
    }

    if (item.name === '02_agent.png') {
      const loadingDots = page.locator('.typing');
      try {
        await page.waitForFunction(() => {
          const bubbles = Array.from(document.querySelectorAll('.msg-row.msg-left .msg-bubble'));
          return bubbles.some((el) => (el.textContent || '').trim().length > 0);
        }, { timeout: 40000 });
      } catch {
        // 忽略等待失败，仍然产出截图用于排查
      }
      try {
        await loadingDots.first().waitFor({ state: 'detached', timeout: 35000 });
      } catch {
        // 兜底继续，避免因为单次慢响应阻断整批截图
      }
      await page.waitForTimeout(1500);
    }

    const target = path.join(outDir, item.name);
    await page.screenshot({ path: target, fullPage: false });
    console.log('saved', target);
  }

  await context.close();
  await browser.close();
}

capture().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
