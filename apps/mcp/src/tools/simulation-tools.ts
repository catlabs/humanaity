import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import type { BackendClient } from "../backend-client.js";
import type { SimulationRunOutput } from "../contracts.js";
import { toToolError } from "../errors.js";

function summarizeSimulationRun(run: SimulationRunOutput): string {
  return `run #${run.id} city=${run.cityId} status=${run.status} running=${run.running} tick=${run.tick}`;
}

export function registerSimulationTools(server: McpServer, backendClient: BackendClient): void {
  server.tool(
    "simulation_create",
    "Create a simulation run for a city or return the existing one.",
    {
      cityId: z.string().min(1),
      seed: z.number().int().optional(),
      accessToken: z.string().min(1).optional(),
    },
    async ({ cityId, seed, accessToken }) => {
      try {
        const run = await backendClient.simulationCreate(cityId, seed, accessToken);
        return {
          content: [{ type: "text", text: `Simulation run created/loaded: ${summarizeSimulationRun(run)}.` }],
          structuredContent: { ok: true, run },
        };
      } catch (error: unknown) {
        const normalized = toToolError(error);
        return {
          isError: true,
          content: [{ type: "text", text: normalized.message }],
          structuredContent: {
            ok: false,
            error: normalized.message,
            details: normalized.details,
          },
        };
      }
    },
  );

  server.tool(
    "simulation_load",
    "Load persisted simulation run metadata for a city.",
    {
      cityId: z.string().min(1),
      accessToken: z.string().min(1).optional(),
    },
    async ({ cityId, accessToken }) => {
      try {
        const run = await backendClient.simulationLoad(cityId, accessToken);
        return {
          content: [{ type: "text", text: `Simulation run loaded: ${summarizeSimulationRun(run)}.` }],
          structuredContent: { ok: true, run },
        };
      } catch (error: unknown) {
        const normalized = toToolError(error);
        return {
          isError: true,
          content: [{ type: "text", text: normalized.message }],
          structuredContent: {
            ok: false,
            error: normalized.message,
            details: normalized.details,
          },
        };
      }
    },
  );

  server.tool(
    "simulation_pause",
    "Pause a simulation run for a city.",
    {
      cityId: z.string().min(1),
      accessToken: z.string().min(1).optional(),
    },
    async ({ cityId, accessToken }) => {
      try {
        const run = await backendClient.simulationPause(cityId, accessToken);
        return {
          content: [{ type: "text", text: `Simulation paused: ${summarizeSimulationRun(run)}.` }],
          structuredContent: { ok: true, run },
        };
      } catch (error: unknown) {
        const normalized = toToolError(error);
        return {
          isError: true,
          content: [{ type: "text", text: normalized.message }],
          structuredContent: {
            ok: false,
            error: normalized.message,
            details: normalized.details,
          },
        };
      }
    },
  );

  server.tool(
    "simulation_resume",
    "Resume a simulation run for a city.",
    {
      cityId: z.string().min(1),
      accessToken: z.string().min(1).optional(),
    },
    async ({ cityId, accessToken }) => {
      try {
        const run = await backendClient.simulationResume(cityId, accessToken);
        return {
          content: [{ type: "text", text: `Simulation resumed: ${summarizeSimulationRun(run)}.` }],
          structuredContent: { ok: true, run },
        };
      } catch (error: unknown) {
        const normalized = toToolError(error);
        return {
          isError: true,
          content: [{ type: "text", text: normalized.message }],
          structuredContent: {
            ok: false,
            error: normalized.message,
            details: normalized.details,
          },
        };
      }
    },
  );

  server.tool(
    "simulation_start",
    "Start simulation for a city.",
    {
      cityId: z.string().min(1),
      accessToken: z.string().min(1).optional(),
    },
    async ({ cityId, accessToken }) => {
      try {
        const result = await backendClient.simulationStart(cityId, accessToken);
        return {
          content: [{ type: "text", text: result.message }],
          structuredContent: { ok: true, cityId, ...result },
        };
      } catch (error: unknown) {
        const normalized = toToolError(error);
        return {
          isError: true,
          content: [{ type: "text", text: normalized.message }],
          structuredContent: {
            ok: false,
            error: normalized.message,
            details: normalized.details,
          },
        };
      }
    },
  );

  server.tool(
    "simulation_stop",
    "Stop simulation for a city.",
    {
      cityId: z.string().min(1),
      accessToken: z.string().min(1).optional(),
    },
    async ({ cityId, accessToken }) => {
      try {
        const result = await backendClient.simulationStop(cityId, accessToken);
        return {
          content: [{ type: "text", text: result.message }],
          structuredContent: { ok: true, cityId, ...result },
        };
      } catch (error: unknown) {
        const normalized = toToolError(error);
        return {
          isError: true,
          content: [{ type: "text", text: normalized.message }],
          structuredContent: {
            ok: false,
            error: normalized.message,
            details: normalized.details,
          },
        };
      }
    },
  );

  server.tool(
    "simulation_status",
    "Read running status of a city simulation.",
    {
      cityId: z.string().min(1),
      accessToken: z.string().min(1).optional(),
    },
    async ({ cityId, accessToken }) => {
      try {
        const status = await backendClient.simulationStatus(cityId, accessToken);
        return {
          content: [{ type: "text", text: `Simulation running: ${status.running}` }],
          structuredContent: { ok: true, cityId, ...status },
        };
      } catch (error: unknown) {
        const normalized = toToolError(error);
        return {
          isError: true,
          content: [{ type: "text", text: normalized.message }],
          structuredContent: {
            ok: false,
            error: normalized.message,
            details: normalized.details,
          },
        };
      }
    },
  );

  server.tool(
    "simulation_snapshot",
    "Get simulation status + humans and compute snapshot metrics.",
    {
      cityId: z.string().min(1),
      accessToken: z.string().min(1).optional(),
    },
    async ({ cityId, accessToken }) => {
      try {
        const snapshot = await backendClient.simulationSnapshot(cityId, accessToken);
        return {
          content: [
            {
              type: "text",
              text: `Snapshot for city ${cityId}: running=${snapshot.running}, humans=${snapshot.metrics.count}, busyRatio=${snapshot.metrics.busyRatio.toFixed(3)}`,
            },
          ],
          structuredContent: { ok: true, snapshot },
        };
      } catch (error: unknown) {
        const normalized = toToolError(error);
        return {
          isError: true,
          content: [{ type: "text", text: normalized.message }],
          structuredContent: {
            ok: false,
            error: normalized.message,
            details: normalized.details,
          },
        };
      }
    },
  );
}
