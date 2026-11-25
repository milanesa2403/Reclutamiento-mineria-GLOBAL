import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// En desarrollo, las llamadas a /v1 y /api se redirigen al backend (puerto 8080): no hace falta CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/v1': 'http://localhost:8080',
      '/api': 'http://localhost:8080',
    },
  },
});
