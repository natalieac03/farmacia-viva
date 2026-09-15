import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";

export default defineConfig({
  // Fora do node_modules: o Railpack monta node_modules/.vite como cache de build (ver README > Deploy).
  cacheDir: ".vite-cache",
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    port: 5173,
  },
});
