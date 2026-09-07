import { z } from "zod";
import { apiGet } from "@/lib/api";

export const healthResponseSchema = z.object({
  status: z.string(),
  application: z.string(),
  timestamp: z.string(),
});

export type HealthResponse = z.infer<typeof healthResponseSchema>;

export async function fetchHealth(): Promise<HealthResponse> {
  const data = await apiGet<unknown>("/api/v1/health");
  return healthResponseSchema.parse(data);
}
