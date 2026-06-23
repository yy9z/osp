#!/usr/bin/env node

import http from "node:http";
import https from "node:https";
import { URL } from "node:url";
import readline from "node:readline";

const BASE_URL = process.env.CAMPUS_MCP_BASE_URL || "http://127.0.0.1:8080";
const INTERNAL_SECRET = process.env.CAMPUS_MCP_INTERNAL_SECRET || "";

const TOOLS = [
  {
    name: "mcp_campus_overview",
    description: "系统内 MCP 场景：汇总首页主动提醒、消息通知和报修状态。",
    inputSchema: {
      type: "object",
      properties: {
        keyword: { type: "string", description: "可选，消息或提醒关键词" }
      }
    }
  },
  {
    name: "mcp_dorm_repair_flow",
    description: "系统内 MCP 场景：检查宿舍档案并提交宿舍报修。",
    inputSchema: {
      type: "object",
      properties: {
        fault_type: { type: "string", description: "故障类型，例如空调/灯/网络/门锁/水管/其他" },
        description: { type: "string", description: "故障补充描述" }
      },
      required: ["fault_type"]
    }
  },
  {
    name: "mcp_secondhand_meetup_flow",
    description: "系统内 MCP 场景：搜索二手商品并联动校内面交点导航。",
    inputSchema: {
      type: "object",
      properties: {
        keyword: { type: "string", description: "商品关键词" },
        category: { type: "string", description: "商品分类" },
        max_price: { type: "string", description: "最高预算" },
        destination: { type: "string", description: "期望面交点" }
      }
    }
  },
  {
    name: "mcp_lostfound_match_flow",
    description: "系统内 MCP 场景：发布寻物或招领记录并返回匹配候选。",
    inputSchema: {
      type: "object",
      properties: {
        type: { type: "string", description: "LOST 或 FOUND，默认 LOST" },
        item_name: { type: "string", description: "物品名称" },
        color: { type: "string", description: "颜色" },
        location: { type: "string", description: "地点" },
        description: { type: "string", description: "补充描述" }
      },
      required: ["item_name"]
    }
  }
];

const TOOL_HANDLERS = {
  mcp_campus_overview: handleCampusOverview,
  mcp_dorm_repair_flow: handleDormRepairFlow,
  mcp_secondhand_meetup_flow: handleSecondhandMeetupFlow,
  mcp_lostfound_match_flow: handleLostfoundMatchFlow
};

const rl = readline.createInterface({
  input: process.stdin,
  crlfDelay: Infinity
});

rl.on("line", async (line) => {
  if (!line || !line.trim()) {
    return;
  }
  let request;
  try {
    request = JSON.parse(line);
  } catch (error) {
    writeError(null, -32700, "Parse error");
    return;
  }

  if (!Object.prototype.hasOwnProperty.call(request, "id")) {
    return;
  }

  try {
    const result = await handleRequest(request);
    writeResponse(request.id, result);
  } catch (error) {
    writeError(request.id, -32603, error.message || "Internal error");
  }
});

async function handleRequest(request) {
  switch (request.method) {
    case "initialize":
      return {
        protocolVersion: "2024-11-05",
        capabilities: { tools: {} },
        serverInfo: { name: "campus-internal-mcp-server", version: "1.0.0" }
      };
    case "tools/list":
      return { tools: TOOLS };
    case "tools/call":
      return callTool(request.params || {});
    default:
      throw new Error(`Unsupported MCP method: ${request.method}`);
  }
}

async function callTool(params) {
  const name = params.name;
  const args = params.arguments || {};
  const handler = TOOL_HANDLERS[name];
  if (!handler) {
    return mcpToolError(`未知系统内 MCP 场景工具：${name}`);
  }
  const userId = args.userId ?? args.user_id;
  if (userId === undefined || userId === null || String(userId).trim() === "") {
    return mcpToolError("缺少当前用户身份，无法调用系统内业务工具。");
  }
  if (!INTERNAL_SECRET) {
    return mcpToolError("系统内 MCP 共享密钥未配置。");
  }

  const cleanArgs = sanitizeArguments(args);
  const flow = await handler(cleanArgs, userId);
  return mcpToolSuccess(flow.summary, flow.data);
}

async function handleCampusOverview(args, userId) {
  const steps = await allSettledSteps([
    bridgeCall("campus_tips", {}, userId),
    bridgeCall("message_query", filterDefined({ keyword: args.keyword }), userId),
    bridgeCall("repair_query", {}, userId)
  ]);

  return {
    summary: buildStepSummary("已通过 MCP 汇总校园待办、消息通知和报修状态", steps),
    data: {
      scenario: "mcp_campus_overview",
      steps,
      campusTips: stepData(steps, "campus_tips"),
      messages: stepData(steps, "message_query"),
      repairs: stepData(steps, "repair_query")
    }
  };
}

async function handleDormRepairFlow(args, userId) {
  const dorm = await safeBridgeCall("dorm_query", {}, userId);
  if (!dorm.success) {
    return {
      summary: "未找到宿舍档案，MCP 未提交报修工单。",
      data: {
        scenario: "mcp_dorm_repair_flow",
        clarificationRequired: true,
        clarificationType: "dorm_info_required",
        askFor: "dorm_info",
        agentSummary: "我暂时没有拿到你的宿舍信息，无法直接提交报修。请先完善宿舍信息后再试。",
        steps: [dorm]
      }
    };
  }

  const repair = await safeBridgeCall("dorm_repair", {
    fault_type: args.fault_type,
    description: args.description
  }, userId);

  const data = {
    scenario: "mcp_dorm_repair_flow",
    steps: [dorm, repair],
    dorm: dorm.data,
    repair: repair.data
  };
  if (repair.data && repair.data.clarificationRequired === true) {
    Object.assign(data, repair.data);
  }

  return {
    summary: repair.summary || repair.errorMessage || "MCP 已完成宿舍报修流程。",
    data
  };
}

async function handleSecondhandMeetupFlow(args, userId) {
  const search = await safeBridgeCall("secondhand_search", filterDefined({
    keyword: args.keyword,
    category: args.category,
    max_price: args.max_price,
    sort_preference: args.sort_preference
  }), userId);

  const destination = args.destination || args.meetup_location || "东区学生食堂门口";
  const navigation = await safeBridgeCall("navigation_v2", filterDefined({
    destination,
    campus: args.campus || "东校区",
    task_scene: "secondhand_meetup"
  }), userId);

  return {
    summary: buildStepSummary(`已通过 MCP 搜索二手商品，并推荐在 ${destination} 面交`, [search, navigation]),
    data: {
      scenario: "mcp_secondhand_meetup_flow",
      meetupPoint: destination,
      steps: [search, navigation],
      products: search.data,
      route: navigation.data
    }
  };
}

async function handleLostfoundMatchFlow(args, userId) {
  const type = String(args.type || "LOST").trim().toUpperCase();
  const toolName = type === "FOUND" ? "lostfound_found" : "lostfound_lost";
  const publish = await safeBridgeCall(toolName, filterDefined({
    item_name: args.item_name,
    color: args.color,
    location: args.location,
    description: args.description
  }), userId);

  return {
    summary: publish.summary || publish.errorMessage || "MCP 已完成失物招领流程。",
    data: {
      scenario: "mcp_lostfound_match_flow",
      type: toolName === "lostfound_found" ? "FOUND" : "LOST",
      steps: [publish],
      lostFound: publish.data
    }
  };
}

async function allSettledSteps(promises) {
  const settled = await Promise.all(promises.map((promise) => promise.catch((error) => ({
    toolName: "unknown",
    success: false,
    summary: error.message,
    errorMessage: error.message,
    data: null
  }))));
  return settled;
}

async function safeBridgeCall(toolName, args, userId) {
  try {
    return await bridgeCall(toolName, args, userId);
  } catch (error) {
    return {
      toolName,
      success: false,
      summary: error.message,
      errorMessage: error.message,
      data: null
    };
  }
}

async function bridgeCall(toolName, args, userId) {
  const response = await postJson(`${BASE_URL.replace(/\/$/, "")}/api/internal/mcp/tools/call`, {
    toolName,
    arguments: args || {},
    userId
  }, {
    "X-Campus-MCP-Secret": INTERNAL_SECRET
  });

  if (!response.ok) {
    throw new Error(`内部桥接接口 HTTP ${response.status}: ${response.bodyText}`);
  }
  const payload = response.body;
  if (!payload || payload.code !== 200) {
    throw new Error(payload?.message || "内部桥接接口返回失败");
  }
  const result = payload.data || {};
  return {
    toolName,
    success: Boolean(result.success),
    summary: result.success ? result.summary : result.errorMessage,
    errorMessage: result.errorMessage,
    data: result.data
  };
}

async function postJson(url, body, headers = {}) {
  const payload = JSON.stringify(body);
  if (typeof fetch === "function") {
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...headers
      },
      body: payload
    });
    const bodyText = await response.text();
    return {
      ok: response.ok,
      status: response.status,
      bodyText,
      body: parseJson(bodyText)
    };
  }

  return postJsonWithHttpModule(url, payload, headers);
}

function postJsonWithHttpModule(rawUrl, payload, headers) {
  return new Promise((resolve, reject) => {
    const url = new URL(rawUrl);
    const client = url.protocol === "https:" ? https : http;
    const request = client.request({
      method: "POST",
      hostname: url.hostname,
      port: url.port || (url.protocol === "https:" ? 443 : 80),
      path: `${url.pathname}${url.search}`,
      headers: {
        "Content-Type": "application/json",
        "Content-Length": Buffer.byteLength(payload),
        ...headers
      }
    }, (response) => {
      let bodyText = "";
      response.setEncoding("utf8");
      response.on("data", (chunk) => {
        bodyText += chunk;
      });
      response.on("end", () => {
        resolve({
          ok: response.statusCode >= 200 && response.statusCode < 300,
          status: response.statusCode,
          bodyText,
          body: parseJson(bodyText)
        });
      });
    });
    request.on("error", reject);
    request.write(payload);
    request.end();
  });
}

function parseJson(text) {
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

function sanitizeArguments(args) {
  const clean = {};
  for (const [key, value] of Object.entries(args || {})) {
    if (["userId", "user_id", "token", "authorization"].includes(key)) {
      continue;
    }
    if (value !== undefined && value !== null && String(value).trim() !== "") {
      clean[key] = value;
    }
  }
  return clean;
}

function filterDefined(input) {
  const output = {};
  for (const [key, value] of Object.entries(input || {})) {
    if (value !== undefined && value !== null && String(value).trim() !== "") {
      output[key] = value;
    }
  }
  return output;
}

function stepData(steps, toolName) {
  return steps.find((step) => step.toolName === toolName)?.data || null;
}

function buildStepSummary(prefix, steps) {
  const summaries = (steps || [])
    .map((step) => step.summary || step.errorMessage)
    .filter(Boolean);
  if (summaries.length === 0) {
    return `${prefix}。`;
  }
  return `${prefix}：${summaries.join("；")}`;
}

function mcpToolSuccess(text, data) {
  return {
    content: [{ type: "text", text: text || "系统内 MCP 场景执行完成。" }],
    structuredContent: data || {},
    isError: false
  };
}

function mcpToolError(text) {
  return {
    content: [{ type: "text", text: text || "系统内 MCP 场景执行失败。" }],
    isError: true
  };
}

function writeResponse(id, result) {
  process.stdout.write(`${JSON.stringify({ jsonrpc: "2.0", id, result })}\n`);
}

function writeError(id, code, message) {
  process.stdout.write(`${JSON.stringify({ jsonrpc: "2.0", id, error: { code, message } })}\n`);
}
