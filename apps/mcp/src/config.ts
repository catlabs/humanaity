import { z } from "zod";

const configSchema = z.object({
  apiBaseUrl: z.string().url(),
  apiAccessToken: z.string().min(1).optional(),
  apiRefreshToken: z.string().min(1).optional(),
  apiEmail: z.string().email().optional(),
  apiPassword: z.string().min(1).optional(),
  requestTimeoutMs: z.number().int().positive(),
});

export type AppConfig = z.infer<typeof configSchema>;

export function loadConfig(env: NodeJS.ProcessEnv = process.env): AppConfig {
  const timeoutRaw = env.HUMANAITY_API_TIMEOUT_MS;
  const requestTimeoutMs = timeoutRaw ? Number(timeoutRaw) : 15_000;

  return configSchema.parse({
    apiBaseUrl: env.HUMANAITY_API_BASE_URL ?? "http://localhost:8080",
    apiAccessToken: env.HUMANAITY_API_ACCESS_TOKEN,
    apiRefreshToken: env.HUMANAITY_API_REFRESH_TOKEN,
    apiEmail: env.HUMANAITY_API_EMAIL,
    apiPassword: env.HUMANAITY_API_PASSWORD,
    requestTimeoutMs: Number.isFinite(requestTimeoutMs) ? requestTimeoutMs : 15_000,
  });
}
