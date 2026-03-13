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
    "simulation_step",
    "Advance a city simulation by one or more deterministic steps without starting the scheduler.",
    {
      cityId: z.string().min(1),
      count: z.number().int().positive().max(10_000).optional(),
      accessToken: z.string().min(1).optional(),
    },
    async ({ cityId, count, accessToken }) => {
      try {
        const stepCount = count ?? 1;
        const run = await backendClient.simulationStep(
          cityId,
          stepCount,
          accessToken,
        );
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(
                {
                  ok: true,
                  cityId,
                  count: stepCount,
                  summary: summarizeSimulationRun(run),
                  run,
                },
                null,
                2,
              ),
            },
          ],
          structuredContent: {
            ok: true,
            cityId,
            count: stepCount,
            summary: summarizeSimulationRun(run),
            run,
          },
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
    "simulation_history_events",
    "List city-scoped deterministic history events ordered by tick and sequence.",
    {
      cityId: z.string().min(1),
      fromTick: z.number().int().nonnegative().optional(),
      toTick: z.number().int().nonnegative().optional(),
      limit: z.number().int().positive().max(1_000).optional(),
      accessToken: z.string().min(1).optional(),
    },
    async ({ cityId, fromTick, toTick, limit, accessToken }) => {
      try {
        const events = await backendClient.simulationHistoryEvents(
          cityId,
          { fromTick, toTick, limit },
          accessToken,
        );
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(
                {
                  ok: true,
                  cityId,
                  fromTick,
                  toTick,
                  limit,
                  count: events.length,
                  events,
                },
                null,
                2,
              ),
            },
          ],
          structuredContent: {
            ok: true,
            cityId,
            fromTick,
            toTick,
            limit,
            count: events.length,
            events,
          },
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
    "simulation_history_inventions",
    "List city-scoped deterministic inventions ordered by tick and key.",
    {
      cityId: z.string().min(1),
      fromTick: z.number().int().nonnegative().optional(),
      toTick: z.number().int().nonnegative().optional(),
      limit: z.number().int().positive().max(1_000).optional(),
      accessToken: z.string().min(1).optional(),
    },
    async ({ cityId, fromTick, toTick, limit, accessToken }) => {
      try {
        const inventions = await backendClient.simulationHistoryInventions(
          cityId,
          { fromTick, toTick, limit },
          accessToken,
        );
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(
                {
                  ok: true,
                  cityId,
                  fromTick,
                  toTick,
                  limit,
                  count: inventions.length,
                  inventions,
                },
                null,
                2,
              ),
            },
          ],
          structuredContent: {
            ok: true,
            cityId,
            fromTick,
            toTick,
            limit,
            count: inventions.length,
            inventions,
          },
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
    "simulation_history_timeline",
    "Get a city-scoped timeline bundle containing ordered events and inventions.",
    {
      cityId: z.string().min(1),
      fromTick: z.number().int().nonnegative().optional(),
      toTick: z.number().int().nonnegative().optional(),
      limit: z.number().int().positive().max(1_000).optional(),
      accessToken: z.string().min(1).optional(),
    },
    async ({ cityId, fromTick, toTick, limit, accessToken }) => {
      try {
        const timeline = await backendClient.simulationHistoryTimeline(
          cityId,
          { fromTick, toTick, limit },
          accessToken,
        );
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(
                {
                  ok: true,
                  cityId,
                  fromTick,
                  toTick,
                  limit,
                  timeline,
                },
                null,
                2,
              ),
            },
          ],
          structuredContent: {
            ok: true,
            cityId,
            fromTick,
            toTick,
            limit,
            timeline,
          },
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
    "simulation_overview",
    "List backend-owned simulation overview rows for all cities.",
    {
      accessToken: z.string().min(1).optional(),
    },
    async ({ accessToken }) => {
      try {
        const overviews = await backendClient.simulationOverview(accessToken);
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify({ ok: true, count: overviews.length, overviews }, null, 2),
            },
          ],
          structuredContent: { ok: true, count: overviews.length, overviews },
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
    "Get backend-owned simulation snapshot for one city.",
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
              text: JSON.stringify({ ok: true, cityId, snapshot }, null, 2),
            },
          ],
          structuredContent: { ok: true, cityId, snapshot },
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
