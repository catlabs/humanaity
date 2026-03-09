import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { BackendClient } from "./backend-client.js";
import { loadConfig } from "./config.js";
import { registerAuthTools } from "./tools/auth-tools.js";
import { registerCityTools } from "./tools/city-tools.js";
import { registerHumanTools } from "./tools/human-tools.js";
import { registerSimulationTools } from "./tools/simulation-tools.js";

const server = new McpServer({
  name: "humanaity-mcp",
  version: "0.1.0",
});
const config = loadConfig();
const backendClient = new BackendClient(config);

server.tool(
  "health_check",
  "Returns server health and bridge metadata.",
  {},
  async () => {
    const cachedTokens = backendClient.getCachedTokens();
    return {
      content: [
        {
          type: "text",
          text: JSON.stringify(
            {
              status: "ok",
              server: "humanaity-mcp",
              version: "0.1.0",
              apiBaseUrl: config.apiBaseUrl,
              hasCachedAccessToken: Boolean(cachedTokens.accessToken),
              hasCachedRefreshToken: Boolean(cachedTokens.refreshToken),
              timestamp: new Date().toISOString(),
            },
            null,
            2,
          ),
        },
      ],
      structuredContent: {
        status: "ok",
        server: "humanaity-mcp",
        version: "0.1.0",
        apiBaseUrl: config.apiBaseUrl,
        hasCachedAccessToken: Boolean(cachedTokens.accessToken),
        hasCachedRefreshToken: Boolean(cachedTokens.refreshToken),
      },
    };
  },
);

registerAuthTools(server, backendClient);
registerCityTools(server, backendClient);
registerHumanTools(server, backendClient);
registerSimulationTools(server, backendClient);

const transport = new StdioServerTransport();
await server.connect(transport);
