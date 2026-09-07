import { env } from "@/env";

export class ApiError extends Error {
  readonly status: number;
  readonly path: string;

  constructor(status: number, path: string, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.path = path;
  }
}

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(`${env.VITE_API_BASE_URL}${path}`, {
    method: "GET",
    headers: {
      Accept: "application/json",
    },
  });

  if (!response.ok) {
    throw new ApiError(
      response.status,
      path,
      `Falha na requisição GET ${path} (HTTP ${response.status})`,
    );
  }

  return (await response.json()) as T;
}
