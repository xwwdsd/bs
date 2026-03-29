// vite.config.js
import { defineConfig } from "file:///E:/bs5/qd/node_modules/vite/dist/node/index.js";
import vue from "file:///E:/bs5/qd/node_modules/@vitejs/plugin-vue/dist/index.mjs";
import { resolve } from "path";
var __vite_injected_original_dirname = "E:\\bs5\\qd";
var vite_config_default = defineConfig({
  plugins: [vue()],
  // 定义全局变量
  define: {
    global: "globalThis"
  },
  // 开发服务器配置
  server: {
    port: 3e3,
    open: true,
    proxy: {
      // 代理API请求到后端
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
        // rewrite: (path) => path.replace(/^\/api/, '')
      },
      // 代理静态资源请求
      "/uploads": {
        target: "http://localhost:8080/api/uploads",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/uploads/, "")
      }
    }
  },
  // 路径别名
  resolve: {
    alias: {
      "@": resolve(__vite_injected_original_dirname, "src"),
      "@components": resolve(__vite_injected_original_dirname, "src/components"),
      "@views": resolve(__vite_injected_original_dirname, "src/views"),
      "@stores": resolve(__vite_injected_original_dirname, "src/stores"),
      "@utils": resolve(__vite_injected_original_dirname, "src/utils"),
      "@assets": resolve(__vite_injected_original_dirname, "src/assets")
    }
  },
  // CSS配置
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: ``
      }
    }
  }
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcuanMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCJFOlxcXFxiczVcXFxccWRcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIkU6XFxcXGJzNVxcXFxxZFxcXFx2aXRlLmNvbmZpZy5qc1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vRTovYnM1L3FkL3ZpdGUuY29uZmlnLmpzXCI7aW1wb3J0IHsgZGVmaW5lQ29uZmlnIH0gZnJvbSAndml0ZSdcclxuaW1wb3J0IHZ1ZSBmcm9tICdAdml0ZWpzL3BsdWdpbi12dWUnXHJcbmltcG9ydCB7IHJlc29sdmUgfSBmcm9tICdwYXRoJ1xyXG5cclxuLyoqXHJcbiAqIFZpdGVcdTkxNERcdTdGNkVcdTY1ODdcdTRFRjZcclxuICogXHU5MTREXHU3RjZFXHU1RjAwXHU1M0QxXHU2NzBEXHU1MkExXHU1NjY4XHUzMDAxXHU4REVGXHU1Rjg0XHU1MjJCXHU1NDBEXHU3QjQ5XHJcbiAqL1xyXG5leHBvcnQgZGVmYXVsdCBkZWZpbmVDb25maWcoe1xyXG4gIHBsdWdpbnM6IFt2dWUoKV0sXHJcbiAgXHJcbiAgLy8gXHU1QjlBXHU0RTQ5XHU1MTY4XHU1QzQwXHU1M0Q4XHU5MUNGXHJcbiAgZGVmaW5lOiB7XHJcbiAgICBnbG9iYWw6ICdnbG9iYWxUaGlzJyxcclxuICB9LFxyXG4gIFxyXG4gIC8vIFx1NUYwMFx1NTNEMVx1NjcwRFx1NTJBMVx1NTY2OFx1OTE0RFx1N0Y2RVxyXG4gIHNlcnZlcjoge1xyXG4gICAgcG9ydDogMzAwMCxcclxuICAgIG9wZW46IHRydWUsXHJcbiAgICBwcm94eToge1xyXG4gICAgICAvLyBcdTRFRTNcdTc0MDZBUElcdThCRjdcdTZDNDJcdTUyMzBcdTU0MEVcdTdBRUZcclxuICAgICAgJy9hcGknOiB7XHJcbiAgICAgICAgdGFyZ2V0OiAnaHR0cDovL2xvY2FsaG9zdDo4MDgwJyxcclxuICAgICAgICBjaGFuZ2VPcmlnaW46IHRydWUsXHJcbiAgICAgICAgLy8gcmV3cml0ZTogKHBhdGgpID0+IHBhdGgucmVwbGFjZSgvXlxcL2FwaS8sICcnKVxyXG4gICAgICB9LFxyXG4gICAgICAvLyBcdTRFRTNcdTc0MDZcdTk3NTlcdTYwMDFcdThENDRcdTZFOTBcdThCRjdcdTZDNDJcclxuICAgICAgJy91cGxvYWRzJzoge1xyXG4gICAgICAgIHRhcmdldDogJ2h0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9hcGkvdXBsb2FkcycsXHJcbiAgICAgICAgY2hhbmdlT3JpZ2luOiB0cnVlLFxyXG4gICAgICAgIHJld3JpdGU6IChwYXRoKSA9PiBwYXRoLnJlcGxhY2UoL15cXC91cGxvYWRzLywgJycpXHJcbiAgICAgIH1cclxuICAgIH1cclxuICB9LFxyXG4gIFxyXG4gIC8vIFx1OERFRlx1NUY4NFx1NTIyQlx1NTQwRFxyXG4gIHJlc29sdmU6IHtcclxuICAgIGFsaWFzOiB7XHJcbiAgICAgICdAJzogcmVzb2x2ZShfX2Rpcm5hbWUsICdzcmMnKSxcclxuICAgICAgJ0Bjb21wb25lbnRzJzogcmVzb2x2ZShfX2Rpcm5hbWUsICdzcmMvY29tcG9uZW50cycpLFxyXG4gICAgICAnQHZpZXdzJzogcmVzb2x2ZShfX2Rpcm5hbWUsICdzcmMvdmlld3MnKSxcclxuICAgICAgJ0BzdG9yZXMnOiByZXNvbHZlKF9fZGlybmFtZSwgJ3NyYy9zdG9yZXMnKSxcclxuICAgICAgJ0B1dGlscyc6IHJlc29sdmUoX19kaXJuYW1lLCAnc3JjL3V0aWxzJyksXHJcbiAgICAgICdAYXNzZXRzJzogcmVzb2x2ZShfX2Rpcm5hbWUsICdzcmMvYXNzZXRzJylcclxuICAgIH1cclxuICB9LFxyXG4gIFxyXG4gIC8vIENTU1x1OTE0RFx1N0Y2RVxyXG4gIGNzczoge1xyXG4gICAgcHJlcHJvY2Vzc29yT3B0aW9uczoge1xyXG4gICAgICBzY3NzOiB7XHJcbiAgICAgICAgYWRkaXRpb25hbERhdGE6IGBgXHJcbiAgICAgIH1cclxuICAgIH1cclxuICB9XHJcbn0pXHJcbiJdLAogICJtYXBwaW5ncyI6ICI7QUFBbU4sU0FBUyxvQkFBb0I7QUFDaFAsT0FBTyxTQUFTO0FBQ2hCLFNBQVMsZUFBZTtBQUZ4QixJQUFNLG1DQUFtQztBQVF6QyxJQUFPLHNCQUFRLGFBQWE7QUFBQSxFQUMxQixTQUFTLENBQUMsSUFBSSxDQUFDO0FBQUE7QUFBQSxFQUdmLFFBQVE7QUFBQSxJQUNOLFFBQVE7QUFBQSxFQUNWO0FBQUE7QUFBQSxFQUdBLFFBQVE7QUFBQSxJQUNOLE1BQU07QUFBQSxJQUNOLE1BQU07QUFBQSxJQUNOLE9BQU87QUFBQTtBQUFBLE1BRUwsUUFBUTtBQUFBLFFBQ04sUUFBUTtBQUFBLFFBQ1IsY0FBYztBQUFBO0FBQUEsTUFFaEI7QUFBQTtBQUFBLE1BRUEsWUFBWTtBQUFBLFFBQ1YsUUFBUTtBQUFBLFFBQ1IsY0FBYztBQUFBLFFBQ2QsU0FBUyxDQUFDLFNBQVMsS0FBSyxRQUFRLGNBQWMsRUFBRTtBQUFBLE1BQ2xEO0FBQUEsSUFDRjtBQUFBLEVBQ0Y7QUFBQTtBQUFBLEVBR0EsU0FBUztBQUFBLElBQ1AsT0FBTztBQUFBLE1BQ0wsS0FBSyxRQUFRLGtDQUFXLEtBQUs7QUFBQSxNQUM3QixlQUFlLFFBQVEsa0NBQVcsZ0JBQWdCO0FBQUEsTUFDbEQsVUFBVSxRQUFRLGtDQUFXLFdBQVc7QUFBQSxNQUN4QyxXQUFXLFFBQVEsa0NBQVcsWUFBWTtBQUFBLE1BQzFDLFVBQVUsUUFBUSxrQ0FBVyxXQUFXO0FBQUEsTUFDeEMsV0FBVyxRQUFRLGtDQUFXLFlBQVk7QUFBQSxJQUM1QztBQUFBLEVBQ0Y7QUFBQTtBQUFBLEVBR0EsS0FBSztBQUFBLElBQ0gscUJBQXFCO0FBQUEsTUFDbkIsTUFBTTtBQUFBLFFBQ0osZ0JBQWdCO0FBQUEsTUFDbEI7QUFBQSxJQUNGO0FBQUEsRUFDRjtBQUNGLENBQUM7IiwKICAibmFtZXMiOiBbXQp9Cg==
