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

export type CityInput = BackendSchemas["CityInput"];

export type CityOutput = BackendSchemas["CityOutput"];

export type HumanInput = BackendSchemas["HumanInput"];

export type HumanOutput = BackendSchemas["HumanOutput"];

export type SimulationRunInput = BackendSchemas["SimulationRunInput"];

export type SimulationRunOutput = BackendSchemas["SimulationRunOutput"];

export type CityOverviewOutput = BackendSchemas["CityOverviewOutput"];

export type SimulationSnapshotOutput = BackendSchemas["SimulationSnapshotOutput"];

export type EventOutput = BackendSchemas["EventOutput"];

export type InventionOutput = BackendSchemas["InventionOutput"];

export type TimelineOutput = BackendSchemas["TimelineOutput"];

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
