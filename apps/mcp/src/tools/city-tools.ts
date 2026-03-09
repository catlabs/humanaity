import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import type { BackendClient } from "../backend-client.js";
import { toToolError } from "../errors.js";

export function registerCityTools(
  server: McpServer,
  backendClient: BackendClient,
): void {
  server.tool(
    "cities_list",
    "List all cities available to authenticated user.",
    {
      accessToken: z.string().min(1).optional(),
    },
    async ({ accessToken }) => {
      try {
        const cities = await backendClient.listCities(accessToken);
        return {
          content: [{ type: "text", text: `Fetched ${cities.length} cities.` }],
          structuredContent: { ok: true, cities },
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
    "cities_mine",
    "List cities owned by the authenticated user.",
    {
      accessToken: z.string().min(1).optional(),
    },
    async ({ accessToken }) => {
      try {
        const cities = await backendClient.listMyCities(accessToken);
        return {
          content: [
            { type: "text", text: `Fetched ${cities.length} owned cities.` },
          ],
          structuredContent: { ok: true, cities },
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
    "city_create",
    "Create a city for the authenticated user.",
    {
      name: z.string().min(1).max(100),
      accessToken: z.string().min(1).optional(),
    },
    async ({ name, accessToken }) => {
      try {
        const city = await backendClient.createCity(name, accessToken);
        return {
          content: [
            { type: "text", text: `City created: ${city.name} (#${city.id}).` },
          ],
          structuredContent: { ok: true, city },
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
    "city_update",
    "Rename an existing city.",
    {
      cityId: z.number().int().positive(),
      name: z.string().min(1).max(100),
      accessToken: z.string().min(1).optional(),
    },
    async ({ cityId, name, accessToken }) => {
      try {
        const city = await backendClient.updateCity(
          String(cityId),
          name,
          accessToken,
        );
        return {
          content: [
            { type: "text", text: `City updated: ${city.name} (#${city.id}).` },
          ],
          structuredContent: { ok: true, city },
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
