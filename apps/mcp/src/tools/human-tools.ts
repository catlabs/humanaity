import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import type { BackendClient } from "../backend-client.js";
import { toToolError } from "../errors.js";

const normalizedRangeSchema = z.number().finite().min(0).max(1);
const personalitySchema = z.enum([
  "VISIONARY",
  "ENGINEER",
  "STORYTELLER",
  "LEADER",
  "THINKER",
  "DREAMER",
  "BALANCED",
]);

export function registerHumanTools(server: McpServer, backendClient: BackendClient): void {
  server.tool(
    "humans_by_city",
    "List all humans for a given city.",
    {
      cityId: z.string().min(1),
      accessToken: z.string().min(1).optional(),
    },
    async ({ cityId, accessToken }) => {
      try {
        const humans = await backendClient.humansByCity(cityId, accessToken);
        return {
          content: [{ type: "text", text: `Fetched ${humans.length} humans for city ${cityId}.` }],
          structuredContent: { ok: true, cityId, humans },
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
    "human_create",
    "Create a human for a city. Note: backend can recompute personality from traits.",
    {
      cityId: z.number().int().positive(),
      name: z.string().min(1).max(100).optional(),
      busy: z.boolean().optional(),
      x: normalizedRangeSchema.optional(),
      y: normalizedRangeSchema.optional(),
      creativity: normalizedRangeSchema.optional(),
      intellect: normalizedRangeSchema.optional(),
      sociability: normalizedRangeSchema.optional(),
      practicality: normalizedRangeSchema.optional(),
      personality: personalitySchema.optional(),
      accessToken: z.string().min(1).optional(),
    },
    async (args) => {
      const {
        cityId,
        name,
        busy,
        x,
        y,
        creativity,
        intellect,
        sociability,
        practicality,
        personality,
        accessToken,
      } = args;

      try {
        const human = await backendClient.createHuman(
          {
            cityId,
            ...(name !== undefined ? { name } : {}),
            ...(busy !== undefined ? { busy } : {}),
            ...(x !== undefined ? { x } : {}),
            ...(y !== undefined ? { y } : {}),
            ...(creativity !== undefined ? { creativity } : {}),
            ...(intellect !== undefined ? { intellect } : {}),
            ...(sociability !== undefined ? { sociability } : {}),
            ...(practicality !== undefined ? { practicality } : {}),
            ...(personality !== undefined ? { personality } : {}),
          },
          accessToken,
        );

        return {
          content: [{ type: "text", text: `Human created: ${human.name} (#${human.id}).` }],
          structuredContent: { ok: true, human },
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
