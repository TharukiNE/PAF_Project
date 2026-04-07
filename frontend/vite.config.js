import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        // Use IPv4 loopback — Node on Windows often resolves "localhost" to ::1 while Tomcat binds to 127.0.0.1.
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/logout': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: false,
      },
      // OAuth2: forward the Google sign-in initiation and callback to the backend.
      // The redirect URI registered in Google Console must be:
      //   http://localhost:5173/login/oauth2/code/google
      '/oauth2': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/login/oauth2': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
})
