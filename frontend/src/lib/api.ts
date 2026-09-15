import { env } from "@/env";

export type FieldError = { field: string; message: string };

/**
 * Erro de API. `message` vem do corpo `ApiError` do backend
 * (GlobalExceptionHandler) quando ele existe; `status` 0 significa que a
 * requisição nem chegou ao servidor (rede, CORS, backend fora do ar).
 */
export class ApiError extends Error {
  readonly status: number;
  readonly path: string;
  readonly fieldErrors: FieldError[];

  constructor(status: number, path: string, message: string, fieldErrors: FieldError[] = []) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.path = path;
    this.fieldErrors = fieldErrors;
  }
}

type Metodo = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

async function apiRequest<T>(method: Metodo, path: string, body?: unknown): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${env.VITE_API_BASE_URL}${path}`, {
      method,
      headers: {
        Accept: "application/json",
        ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(
      0,
      path,
      "Não foi possível conectar ao servidor. Verifique se o backend está no ar.",
    );
  }

  if (!response.ok) {
    throw await erroDaResposta(response, path);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

async function erroDaResposta(response: Response, path: string): Promise<ApiError> {
  const generica = `Falha na requisição ${path} (HTTP ${response.status})`;
  try {
    const corpo = (await response.json()) as { message?: string; fieldErrors?: FieldError[] };
    return new ApiError(response.status, path, corpo.message ?? generica, corpo.fieldErrors ?? []);
  } catch {
    return new ApiError(response.status, path, generica);
  }
}

export function apiGet<T>(path: string): Promise<T> {
  return apiRequest<T>("GET", path);
}

export function apiPost<T>(path: string, body: unknown): Promise<T> {
  return apiRequest<T>("POST", path, body);
}

export function apiPut<T>(path: string, body: unknown): Promise<T> {
  return apiRequest<T>("PUT", path, body);
}

export function apiPatch<T>(path: string): Promise<T> {
  return apiRequest<T>("PATCH", path);
}

export function apiDelete(path: string): Promise<void> {
  return apiRequest<void>("DELETE", path);
}
