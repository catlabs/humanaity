import type { AppConfig } from "./config.js";
import { BackendApiError } from "./errors.js";
import type {
  AuthRequest,
  AuthTokens,
  BackendMessageResponse,
  BackendSimulationStatusResponse,
  CityInput,
  CityOutput,
  HumanInput,
  HumanOutput,
  MessageResponse,
  RefreshTokenRequest,
  SimulationSnapshot,
  SimulationStatus,
} from "./contracts.js";

type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";

interface RequestOptions {
  body?: unknown;
  accessToken?: string;
}

class TokenStore {
  private accessToken?: string;
  private refreshToken?: string;

  constructor(initial?: Partial<AuthTokens>) {
    this.accessToken = initial?.accessToken;
    this.refreshToken = initial?.refreshToken;
  }

  get access(): string | undefined {
    return this.accessToken;
  }

  get refresh(): string | undefined {
    return this.refreshToken;
  }

  set(tokens: Partial<AuthTokens>): void {
    if (tokens.accessToken) {
      this.accessToken = tokens.accessToken;
    }
    if (tokens.refreshToken) {
      this.refreshToken = tokens.refreshToken;
    }
  }
}

export class BackendClient {
  private readonly config: AppConfig;
  private readonly tokenStore: TokenStore;
  private inFlightLogin?: {
    key: string;
    promise: Promise<AuthTokens>;
  };
  private inFlightRefresh?: {
    key: string;
    promise: Promise<AuthTokens>;
  };

  constructor(config: AppConfig) {
    this.config = config;
    this.tokenStore = new TokenStore({
      accessToken: config.apiAccessToken,
      refreshToken: config.apiRefreshToken,
    });
  }

  getCachedTokens(): Partial<AuthTokens> {
    return {
      accessToken: this.tokenStore.access,
      refreshToken: this.tokenStore.refresh,
    };
  }

  async authLogin(email?: string, password?: string): Promise<AuthTokens> {
    const resolvedEmail = email ?? this.config.apiEmail;
    const resolvedPassword = password ?? this.config.apiPassword;

    if (!resolvedEmail || !resolvedPassword) {
      throw new Error(
        "Email/password not provided. Pass them to auth_login or configure HUMANAITY_API_EMAIL and HUMANAITY_API_PASSWORD.",
      );
    }

    const requestBody: AuthRequest = {
      email: resolvedEmail,
      password: resolvedPassword,
    };

    const requestKey = `${resolvedEmail}\u0000${resolvedPassword}`;
    if (this.inFlightLogin?.key === requestKey) {
      return this.inFlightLogin.promise;
    }

    const loginPromise = this.request<AuthTokens>("POST", "/auth/login", {
      body: requestBody,
    })
      .then((tokens) => {
        this.tokenStore.set(tokens);
        return tokens;
      })
      .finally(() => {
        if (this.inFlightLogin?.key === requestKey) {
          this.inFlightLogin = undefined;
        }
      });

    this.inFlightLogin = {
      key: requestKey,
      promise: loginPromise,
    };

    return loginPromise;
  }

  async authRefresh(refreshToken?: string): Promise<AuthTokens> {
    const effectiveRefreshToken = refreshToken ?? this.tokenStore.refresh;
    if (!effectiveRefreshToken) {
      throw new Error(
        "Refresh token not provided. Pass refreshToken to auth_refresh or run auth_login first.",
      );
    }

    const requestBody: RefreshTokenRequest = {
      refreshToken: effectiveRefreshToken,
    };

    const requestKey = effectiveRefreshToken;
    if (this.inFlightRefresh?.key === requestKey) {
      return this.inFlightRefresh.promise;
    }

    const refreshPromise = this.request<AuthTokens>("POST", "/auth/refresh", {
      body: requestBody,
    })
      .then((tokens) => {
        this.tokenStore.set(tokens);
        return tokens;
      })
      .finally(() => {
        if (this.inFlightRefresh?.key === requestKey) {
          this.inFlightRefresh = undefined;
        }
      });

    this.inFlightRefresh = {
      key: requestKey,
      promise: refreshPromise,
    };

    return refreshPromise;
  }

  async listCities(accessToken?: string): Promise<CityOutput[]> {
    return this.request<CityOutput[]>("GET", "/api/cities", {
      accessToken: await this.resolveAccessToken(accessToken),
    });
  }

  async listMyCities(accessToken?: string): Promise<CityOutput[]> {
    return this.request<CityOutput[]>("GET", "/api/cities/mine", {
      accessToken: await this.resolveAccessToken(accessToken),
    });
  }

  async createCity(name: string, accessToken?: string): Promise<CityOutput> {
    const requestBody: CityInput = { name };

    return this.request<CityOutput>("POST", "/api/cities", {
      accessToken: await this.resolveAccessToken(accessToken),
      body: requestBody,
    });
  }

  async updateCity(
    cityId: string,
    name: string,
    accessToken?: string,
  ): Promise<CityOutput> {
    const requestBody: CityInput = { name };

    return this.request<CityOutput>(
      "PUT",
      `/api/cities/${encodeURIComponent(cityId)}`,
      {
        accessToken: await this.resolveAccessToken(accessToken),
        body: requestBody,
      },
    );
  }

  async humansByCity(
    cityId: string,
    accessToken?: string,
  ): Promise<HumanOutput[]> {
    return this.request<HumanOutput[]>(
      "GET",
      `/api/humans/city/${encodeURIComponent(cityId)}`,
      {
        accessToken: await this.resolveAccessToken(accessToken),
      },
    );
  }

  async createHuman(
    input: HumanInput,
    accessToken?: string,
  ): Promise<HumanOutput> {
    return this.request<HumanOutput>("POST", "/api/humans", {
      accessToken: await this.resolveAccessToken(accessToken),
      body: input,
    });
  }

  async simulationStart(
    cityId: string,
    accessToken?: string,
  ): Promise<MessageResponse> {
    const response = await this.request<BackendMessageResponse>(
      "POST",
      `/api/simulations/${encodeURIComponent(cityId)}/start`,
      {
        accessToken: await this.resolveAccessToken(accessToken),
      },
    );

    return this.normalizeMessageResponse(response);
  }

  async simulationStop(
    cityId: string,
    accessToken?: string,
  ): Promise<MessageResponse> {
    const response = await this.request<BackendMessageResponse>(
      "POST",
      `/api/simulations/${encodeURIComponent(cityId)}/stop`,
      {
        accessToken: await this.resolveAccessToken(accessToken),
      },
    );

    return this.normalizeMessageResponse(response);
  }

  async simulationStatus(
    cityId: string,
    accessToken?: string,
  ): Promise<SimulationStatus> {
    const response = await this.request<BackendSimulationStatusResponse>(
      "GET",
      `/api/simulations/${encodeURIComponent(cityId)}/status`,
      {
        accessToken: await this.resolveAccessToken(accessToken),
      },
    );

    return this.normalizeSimulationStatus(response);
  }

  async simulationSnapshot(
    cityId: string,
    accessToken?: string,
  ): Promise<SimulationSnapshot> {
    const [status, humans] = await Promise.all([
      this.simulationStatus(cityId, accessToken),
      this.humansByCity(cityId, accessToken),
    ]);

    const busyCount = humans.filter((human) => human.busy ?? false).length;
    const count = humans.length;
    const busyRatio = count === 0 ? 0 : busyCount / count;

    const validCoordinates = humans.filter(
      (human) => Number.isFinite(human.x) && Number.isFinite(human.y),
    );

    const centroid =
      validCoordinates.length === 0
        ? null
        : {
            x:
              validCoordinates.reduce((sum, human) => sum + human.x, 0) /
              validCoordinates.length,
            y:
              validCoordinates.reduce((sum, human) => sum + human.y, 0) /
              validCoordinates.length,
          };

    const bounds =
      validCoordinates.length === 0
        ? null
        : {
            minX: Math.min(...validCoordinates.map((human) => human.x)),
            maxX: Math.max(...validCoordinates.map((human) => human.x)),
            minY: Math.min(...validCoordinates.map((human) => human.y)),
            maxY: Math.max(...validCoordinates.map((human) => human.y)),
          };

    return {
      cityId,
      running: status.running,
      humans,
      metrics: {
        count,
        busyCount,
        busyRatio,
        centroid,
        bounds,
      },
    };
  }

  private async resolveAccessToken(
    explicitAccessToken?: string,
  ): Promise<string> {
    if (explicitAccessToken) {
      return explicitAccessToken;
    }

    if (this.tokenStore.access) {
      return this.tokenStore.access;
    }

    if (this.config.apiEmail && this.config.apiPassword) {
      const tokens = await this.authLogin(
        this.config.apiEmail,
        this.config.apiPassword,
      );
      return tokens.accessToken;
    }

    throw new Error(
      "Access token missing. Pass `accessToken`, run auth_login, or set HUMANAITY_API_EMAIL and HUMANAITY_API_PASSWORD.",
    );
  }

  private async request<T>(
    method: HttpMethod,
    path: string,
    options?: RequestOptions,
  ): Promise<T> {
    const controller = new AbortController();
    const timeoutHandle = setTimeout(() => {
      controller.abort();
    }, this.config.requestTimeoutMs);

    try {
      const response = await fetch(`${this.config.apiBaseUrl}${path}`, {
        method,
        headers: {
          "Content-Type": "application/json",
          ...(options?.accessToken
            ? { Authorization: `Bearer ${options.accessToken}` }
            : {}),
        },
        body: options?.body ? JSON.stringify(options.body) : undefined,
        signal: controller.signal,
      });

      const payload = await this.readPayload(response);
      if (!response.ok) {
        const message =
          this.extractErrorMessage(payload) ?? response.statusText;
        throw new BackendApiError({
          status: response.status,
          path,
          message,
          payload,
        });
      }

      return payload as T;
    } catch (error: unknown) {
      if (error instanceof BackendApiError) {
        throw error;
      }
      if (error instanceof Error && error.name === "AbortError") {
        throw new Error(
          `Request timeout after ${this.config.requestTimeoutMs}ms for ${path}`,
        );
      }
      throw error;
    } finally {
      clearTimeout(timeoutHandle);
    }
  }

  private async readPayload(response: Response): Promise<unknown> {
    const text = await response.text();
    if (!text) {
      return {};
    }

    try {
      return JSON.parse(text) as unknown;
    } catch {
      return { message: text };
    }
  }

  private extractErrorMessage(payload: unknown): string | undefined {
    if (!payload || typeof payload !== "object") {
      return undefined;
    }

    const candidate = payload as { message?: unknown; error?: unknown };
    if (typeof candidate.message === "string" && candidate.message.length > 0) {
      return candidate.message;
    }
    if (typeof candidate.error === "string" && candidate.error.length > 0) {
      return candidate.error;
    }

    return undefined;
  }

  private normalizeMessageResponse(
    payload: BackendMessageResponse,
  ): MessageResponse {
    if ("message" in payload && typeof payload.message === "string") {
      return { message: payload.message };
    }

    const firstStringValue = Object.values(payload).find(
      (value) => typeof value === "string",
    );

    return {
      message: firstStringValue ?? "Operation completed.",
    };
  }

  private normalizeSimulationStatus(
    payload: BackendSimulationStatusResponse,
  ): SimulationStatus {
    if ("running" in payload && typeof payload.running === "boolean") {
      return { running: payload.running };
    }

    const firstBooleanValue = Object.values(payload).find(
      (value) => typeof value === "boolean",
    );

    return {
      running: firstBooleanValue ?? false,
    };
  }
}
