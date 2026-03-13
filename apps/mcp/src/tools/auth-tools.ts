import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import type { BackendClient } from "../backend-client.js";
import { toToolError } from "../errors.js";

export function registerAuthTools(server: McpServer, backendClient: BackendClient): void {
  server.tool(
    "auth_login",
    "Authenticate with backend and cache returned JWT tokens.",
    {
      email: z.string().email().optional(),
      password: z.string().min(1).optional(),
    },
    async ({ email, password }) => {
      try {
        const tokens = await backendClient.authLogin(email, password);
        return {
          content: [{ type: "text", text: "Login successful." }],
          structuredContent: {
            ok: true,
            ...tokens,
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
    "auth_signup",
    "Create a new user account (signup). Does not require authentication.",
    {
      email: z.string().email(),
      password: z.string().min(1),
      confirmPassword: z.string().min(1).optional(),
    },
    async ({ email, password, confirmPassword }) => {
      try {
        const result = await backendClient.authSignup(email, password, confirmPassword);
        return {
          content: [{ type: "text", text: result.message }],
          structuredContent: { ok: true, message: result.message },
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
    "auth_refresh",
    "Refresh access token using refresh token and update token cache.",
    {
      refreshToken: z.string().min(1).optional(),
    },
    async ({ refreshToken }) => {
      try {
        const tokens = await backendClient.authRefresh(refreshToken);
        return {
          content: [{ type: "text", text: "Token refresh successful." }],
          structuredContent: {
            ok: true,
            ...tokens,
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
}
