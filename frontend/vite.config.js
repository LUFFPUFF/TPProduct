import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import path from 'path';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    origin: 'http://dialogx.ru',
    cors: true,
  },
  build: {
    lib: {
      entry: path.resolve(__dirname, 'src/components/embed.jsx'),
      name: 'ChatWidget',
      fileName: () => 'widget.js',
      formats: ['iife'],
    },
    rollupOptions: {
      output: {
        assetFileNames: 'widget.css',
      },
    },
  },
});
