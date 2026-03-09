import type { ApiErrorPayload } from "./contracts.js";

export class BackendApiError extends Error {
  readonly status: number;
  readonly path: string;
  readonly payload?: ApiErrorPayload | unknown;

  constructor(params: {
    status: number;
    path: string;
    message: string;
    payload?: unknown;
  }) {
    super(params.message);
    this.name = "BackendApiError";
    this.status = params.status;
    this.path = params.path;
    this.payload = params.payload;
  }
}

export function toToolError(error: unknown): {
  message: string;
  details?: unknown;
} {
  if (error instanceof BackendApiError) {
    return {
      message: `Backend request failed (${error.status}) on ${error.path}: ${error.message}`,
      details: error.payload,
    };
  }

  if (error instanceof Error) {
    return { message: error.message };
  }

  return { message: "Unknown error" };
}
