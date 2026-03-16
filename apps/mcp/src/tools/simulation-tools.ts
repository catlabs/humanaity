import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import type { BackendClient } from "../backend-client.js";
import type { EventOutput, SimulationRunOutput } from "../contracts.js";
import { toToolError } from "../errors.js";

interface ToolSuccessPayload {
  ok: true;
  [key: string]: unknown;
}

interface ToolErrorPayload {
  ok: false;
  error: string;
  details?: unknown;
  [key: string]: unknown;
}

function summarizeSimulationRun(run: SimulationRunOutput): string {
  return `run #${run.id} city=${run.cityId} status=${run.status} running=${run.running} tick=${run.tick}`;
}

function describeImportance(importance: number): string {
  if (importance >= 8) {
    return "high";
  }
  if (importance >= 5) {
    return "medium";
  }
  return "low";
}

function toPayloadFacts(payload: Record<string, string>): string[] {
  return Object.entries(payload)
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([key, value]) => `${key}=${value}`);
}

function buildEventExplanation(event: EventOutput): string {
  const actorText =
    event.actorIds.length > 0
      ? `Actors involved: ${event.actorIds.join(", ")}.`
      : "No explicit actor IDs were recorded.";
  const payloadFacts = toPayloadFacts(event.payload);
  const payloadText =
    payloadFacts.length > 0
      ? `Deterministic payload facts: ${payloadFacts.join("; ")}.`
      : "No payload facts were recorded for this event.";
  const enrichmentText =
    event.enrichmentStatus === "READY" || event.enrichmentStatus === "FALLBACK"
      ? `Enrichment status is ${event.enrichmentStatus.toLowerCase()}${
          event.enrichedSnippet ? ` with snippet "${event.enrichedSnippet}"` : ""
        }.`
      : "No enrichment text is attached to this event.";
  return [
    `Event ${event.id} (${event.eventKey}) happened at tick ${event.tick}, year ${event.year}, era ${event.era}.`,
    `It is categorized as ${event.eventCategory}/${event.eventType} with ${describeImportance(event.importance)} importance (${event.importance}/10).`,
    actorText,
    payloadText,
    enrichmentText,
  ].join(" ");
}

function toolSuccess(payload: ToolSuccessPayload) {
  return {
    content: [{ type: "text" as const, text: JSON.stringify(payload, null, 2) }],
    structuredContent: payload,
  };
}

function toolFailure(error: unknown) {
  const normalized = toToolError(error);
  const payload: ToolErrorPayload = {
    ok: false,
    error: normalized.message,
    details: normalized.details,
  };
  return {
    isError: true as const,
    content: [{ type: "text" as const, text: normalized.message }],
    structuredContent: payload,
  };
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
        return toolSuccess({
          ok: true,
          cityId,
          summary: summarizeSimulationRun(run),
          run,
        });
      } catch (error: unknown) {
        return toolFailure(error);
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
        return toolSuccess({
          ok: true,
          cityId,
          ...status,
        });
      } catch (error: unknown) {
        return toolFailure(error);
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
        return toolSuccess({
          ok: true,
          cityId,
          fromTick,
          toTick,
          limit,
          count: events.length,
          events,
        });
      } catch (error: unknown) {
        return toolFailure(error);
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
        return toolSuccess({
          ok: true,
          cityId,
          fromTick,
          toTick,
          limit,
          count: inventions.length,
          inventions,
        });
      } catch (error: unknown) {
        return toolFailure(error);
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
        return toolSuccess({
          ok: true,
          cityId,
          fromTick,
          toTick,
          limit,
          timeline,
        });
      } catch (error: unknown) {
        return toolFailure(error);
      }
    },
  );

  server.tool(
    "simulation_event_explain",
    "Explain one deterministic history event using backend-owned event fields.",
    {
      cityId: z.string().min(1),
      eventId: z.string().min(1).optional(),
      eventKey: z.string().min(1).optional(),
      fromTick: z.number().int().nonnegative().optional(),
      toTick: z.number().int().nonnegative().optional(),
      limit: z.number().int().positive().max(1_000).optional(),
      accessToken: z.string().min(1).optional(),
    },
    async ({
      cityId,
      eventId,
      eventKey,
      fromTick,
      toTick,
      limit,
      accessToken,
    }) => {
      try {
        if (!eventId && !eventKey) {
          throw new Error(
            "Provide eventId or eventKey to explain a specific event.",
          );
        }

        const parsedEventId =
          eventId === undefined ? undefined : Number.parseInt(eventId, 10);
        if (eventId !== undefined && Number.isNaN(parsedEventId)) {
          throw new Error("eventId must be a numeric string when provided.");
        }

        const resolvedLimit = limit ?? 200;
        const events = await backendClient.simulationHistoryEvents(
          cityId,
          {
            fromTick,
            toTick,
            limit: resolvedLimit,
          },
          accessToken,
        );

        const selectedEvent = events.find((event) => {
          if (parsedEventId !== undefined) {
            return event.id === parsedEventId;
          }
          if (eventKey) {
            return event.eventKey === eventKey;
          }
          return false;
        });

        if (!selectedEvent) {
          throw new Error(
            `Event not found in requested window (cityId=${cityId}, scanned=${events.length}, fromTick=${fromTick ?? "none"}, toTick=${toTick ?? "none"}, limit=${resolvedLimit}).`,
          );
        }

        return toolSuccess({
          ok: true,
          cityId,
          eventReference: {
            eventId: selectedEvent.id,
            eventKey: selectedEvent.eventKey,
          },
          sourceWindow: {
            fromTick,
            toTick,
            limit: resolvedLimit,
            scannedCount: events.length,
          },
          event: selectedEvent,
          explanation: buildEventExplanation(selectedEvent),
        });
      } catch (error: unknown) {
        return toolFailure(error);
      }
    },
  );

  server.tool(
    "simulation_changes_summary",
    "Summarize bounded recent city changes using backend snapshot and timeline history.",
    {
      cityId: z.string().min(1),
      lastTicks: z.number().int().positive().max(1_000).optional(),
      fromTick: z.number().int().nonnegative().optional(),
      toTick: z.number().int().nonnegative().optional(),
      limit: z.number().int().positive().max(1_000).optional(),
      accessToken: z.string().min(1).optional(),
    },
    async ({ cityId, lastTicks, fromTick, toTick, limit, accessToken }) => {
      try {
        const snapshot = await backendClient.simulationSnapshot(cityId, accessToken);
        const currentTick = snapshot.run.tick;
        const resolvedLastTicks = lastTicks ?? 50;
        const resolvedToTick = toTick ?? currentTick;
        const resolvedFromTick =
          fromTick ?? Math.max(0, resolvedToTick - resolvedLastTicks + 1);
        if (resolvedFromTick > resolvedToTick) {
          throw new Error(
            `Invalid tick window: fromTick (${resolvedFromTick}) must be <= toTick (${resolvedToTick}).`,
          );
        }

        const resolvedLimit = limit ?? 500;
        const timeline = await backendClient.simulationHistoryTimeline(
          cityId,
          {
            fromTick: resolvedFromTick,
            toTick: resolvedToTick,
            limit: resolvedLimit,
          },
          accessToken,
        );

        const eventTypeCounts = timeline.events.reduce<Record<string, number>>(
          (accumulator, event) => {
            const key = String(event.eventType);
            accumulator[key] = (accumulator[key] ?? 0) + 1;
            return accumulator;
          },
          {},
        );
        const topEventTypes = Object.entries(eventTypeCounts)
          .sort((left, right) => right[1] - left[1])
          .slice(0, 3)
          .map(([eventType, count]) => ({ eventType, count }));

        const highImportanceEvents = timeline.events
          .filter((event) => event.importance >= 7)
          .slice(-5)
          .map((event) => ({
            id: event.id,
            tick: event.tick,
            eventKey: event.eventKey,
            eventType: event.eventType,
            importance: event.importance,
          }));

        const latestInventions = timeline.inventions.slice(-3).map((invention) => ({
          id: invention.id,
          tickCreated: invention.tickCreated,
          inventionKey: invention.inventionKey,
          title: invention.title,
          impactScore: invention.impactScore,
        }));

        const summary =
          timeline.eventCount === 0 && timeline.inventionCount === 0
            ? `No recorded events or inventions between ticks ${resolvedFromTick} and ${resolvedToTick}.`
            : `Between ticks ${resolvedFromTick} and ${resolvedToTick}, city ${cityId} recorded ${timeline.eventCount} events and ${timeline.inventionCount} inventions.`;
        const snapshotKnowledge = (snapshot as Record<string, unknown>).knowledge as
          | Record<string, unknown>
          | undefined;
        const unlockedDiscoveries = Array.isArray(snapshotKnowledge?.unlockedDiscoveries)
          ? snapshotKnowledge.unlockedDiscoveries.length
          : 0;
        const unlockedInventions = Array.isArray(snapshotKnowledge?.unlockedInventions)
          ? snapshotKnowledge.unlockedInventions.length
          : 0;
        const unlockedApplications = Array.isArray(snapshotKnowledge?.unlockedApplications)
          ? snapshotKnowledge.unlockedApplications.length
          : 0;

        return toolSuccess({
          ok: true,
          cityId,
          summary,
          window: {
            fromTick: resolvedFromTick,
            toTick: resolvedToTick,
            lastTicks: resolvedLastTicks,
            limit: resolvedLimit,
          },
          snapshot: {
            tick: snapshot.run.tick,
            year: snapshot.run.year,
            era: snapshot.run.era,
            population: snapshot.metrics.population,
          },
          counts: {
            eventCount: timeline.eventCount,
            inventionCount: timeline.inventionCount,
          },
          knowledge: {
            unlockedDiscoveries,
            unlockedInventions,
            unlockedApplications,
          },
          topEventTypes,
          highImportanceEvents,
          latestInventions,
        });
      } catch (error: unknown) {
        return toolFailure(error);
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
        return toolSuccess({
          ok: true,
          count: overviews.length,
          overviews,
        });
      } catch (error: unknown) {
        return toolFailure(error);
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
        return toolSuccess({
          ok: true,
          cityId,
          snapshot,
        });
      } catch (error: unknown) {
        return toolFailure(error);
      }
    },
  );
}
