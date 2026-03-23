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
          content: [
            {
              type: "text",
              text: JSON.stringify(
                { ok: true, count: cities.length, cities },
                null,
                2,
              ),
            },
          ],
          structuredContent: { ok: true, count: cities.length, cities },
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
    "List shared cities for the authenticated user. Legacy alias of cities_list.",
    {
      accessToken: z.string().min(1).optional(),
    },
    async ({ accessToken }) => {
      try {
        const cities = await backendClient.listMyCities(accessToken);
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(
                { ok: true, count: cities.length, cities },
                null,
                2,
              ),
            },
          ],
          structuredContent: { ok: true, count: cities.length, cities },
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
    "Create a shared city.",
    {
      name: z.string().min(1).max(100),
      accessToken: z.string().min(1).optional(),
    },
    async ({ name, accessToken }) => {
      try {
        const city = await backendClient.createCity(name, accessToken);
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify({ ok: true, city }, null, 2),
            },
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
            {
              type: "text",
              text: JSON.stringify({ ok: true, city }, null, 2),
            },
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
