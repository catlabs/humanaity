import type { components, operations } from "./generated/api-types.js";

export type BackendSchemas = components["schemas"];
type BackendOperation = keyof operations;
type BackendOperationSuccessPayload<TOperation extends BackendOperation> =
  operations[TOperation]["responses"][200]["content"][
    keyof operations[TOperation]["responses"][200]["content"]
  ];

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export interface ApiErrorPayload {
  message?: string;
  error?: string;
  details?: unknown;
}

export type AuthRequest = BackendSchemas["AuthRequest"];

export type RefreshTokenRequest = BackendSchemas["RefreshTokenRequest"];

export type SignupRequest = BackendSchemas["SignupRequest"];

export type CityInput = BackendSchemas["CityInput"];

export type CityOutput = BackendSchemas["CityOutput"];

export type HumanInput = BackendSchemas["HumanInput"];

export type HumanOutput = BackendSchemas["HumanOutput"];

export type BackendMessageResponse =
  BackendOperationSuccessPayload<"startSimulation">;

export type BackendSimulationStatusResponse =
  BackendOperationSuccessPayload<"isSimulationRunning">;

export interface MessageResponse {
  message: string;
}

export interface SimulationStatus {
  running: boolean;
}

export interface SnapshotMetrics {
  count: number;
  busyCount: number;
  busyRatio: number;
  centroid: {
    x: number;
    y: number;
  } | null;
  bounds: {
    minX: number;
    maxX: number;
    minY: number;
    maxY: number;
  } | null;
}

export interface SimulationSnapshot {
  cityId: string;
  running: boolean;
  humans: HumanOutput[];
  metrics: SnapshotMetrics;
}
