import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

const apiBaseUrl = process.env.HUMANAITY_API_BASE_URL ?? "http://localhost:8080";
const email = process.env.HUMANAITY_API_EMAIL ?? `ci-mcp-${Date.now()}@example.com`;
const password = process.env.HUMANAITY_API_PASSWORD ?? "Test1234!";

function fail(message, details) {
  const suffix = details ? `\n${JSON.stringify(details, null, 2)}` : "";
  throw new Error(`${message}${suffix}`);
}

function assert(condition, message, details) {
  if (!condition) {
    fail(message, details);
  }
}

async function callTool(client, name, args = {}) {
  const result = await client.callTool({ name, arguments: args });
  const payload = result?.structuredContent;

  if (!payload) {
    fail(`Tool ${name} returned no structured content`, result);
  }

  if (payload.ok === false || result?.isError) {
    fail(`Tool ${name} failed`, payload);
  }

  return payload;
}

async function run() {
  const transport = new StdioClientTransport({
    command: "node",
    args: ["dist/index.js"],
    cwd: process.cwd(),
    env: {
      ...process.env,
      HUMANAITY_API_BASE_URL: apiBaseUrl,
    },
    stderr: "pipe",
  });

  const client = new Client({ name: "ci-smoke", version: "0.1.0" });
  let connected = false;

  try {
    await client.connect(transport);
    connected = true;

    const health = await client.callTool({ name: "health_check", arguments: {} });
    const healthPayload = health?.structuredContent ?? {};
    assert(healthPayload.status === "ok", "health_check did not report status=ok", healthPayload);

    await callTool(client, "auth_signup", {
      email,
      password,
      confirmPassword: password,
    });

    const login = await callTool(client, "auth_login", { email, password });
    const accessToken = login.accessToken;
    assert(typeof accessToken === "string" && accessToken.length > 0, "auth_login returned no accessToken", login);

    const mine = await callTool(client, "cities_mine", { accessToken });
    const mineCities = Array.isArray(mine.cities) ? mine.cities : [];

    let cityId = mineCities.length > 0 ? String(mineCities[0].id) : undefined;
    if (!cityId) {
      const created = await callTool(client, "city_create", {
        name: "CI Smoke City",
        accessToken,
      });
      cityId = String(created.city?.id ?? "");
    }

    assert(cityId && cityId.length > 0, "Could not resolve cityId from cities_mine/city_create");

    await callTool(client, "simulation_create", {
      cityId,
      seed: 20260317,
      accessToken,
    });
    await callTool(client, "simulation_step", {
      cityId,
      count: 6,
      accessToken,
    });

    const timeline = await callTool(client, "simulation_history_timeline", {
      cityId,
      fromTick: 0,
      limit: 200,
      accessToken,
    });

    const events = timeline?.timeline?.events ?? [];
    const inventions = timeline?.timeline?.inventions ?? [];
    assert(Array.isArray(events), "timeline.events is not an array", timeline);
    assert(Array.isArray(inventions), "timeline.inventions is not an array", timeline);

    console.log(
      JSON.stringify(
        {
          ok: true,
          apiBaseUrl,
          cityId,
          events: events.length,
          inventions: inventions.length,
        },
        null,
        2,
      ),
    );
  } finally {
    if (connected) {
      await client.close();
    }
  }
}

run().catch((error) => {
  console.error(error);
  process.exit(1);
});
