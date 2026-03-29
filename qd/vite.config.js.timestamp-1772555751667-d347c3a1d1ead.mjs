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
        target: "http://localhost:8080/api",
        changeOrigin: true
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
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcuanMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCJFOlxcXFxiczVcXFxccWRcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIkU6XFxcXGJzNVxcXFxxZFxcXFx2aXRlLmNvbmZpZy5qc1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vRTovYnM1L3FkL3ZpdGUuY29uZmlnLmpzXCI7aW1wb3J0IHsgZGVmaW5lQ29uZmlnIH0gZnJvbSAndml0ZSdcclxuaW1wb3J0IHZ1ZSBmcm9tICdAdml0ZWpzL3BsdWdpbi12dWUnXHJcbmltcG9ydCB7IHJlc29sdmUgfSBmcm9tICdwYXRoJ1xyXG5cclxuLyoqXHJcbiAqIFZpdGVcdTkxNERcdTdGNkVcdTY1ODdcdTRFRjZcclxuICogXHU5MTREXHU3RjZFXHU1RjAwXHU1M0QxXHU2NzBEXHU1MkExXHU1NjY4XHUzMDAxXHU4REVGXHU1Rjg0XHU1MjJCXHU1NDBEXHU3QjQ5XHJcbiAqL1xyXG5leHBvcnQgZGVmYXVsdCBkZWZpbmVDb25maWcoe1xyXG4gIHBsdWdpbnM6IFt2dWUoKV0sXHJcbiAgXHJcbiAgLy8gXHU1QjlBXHU0RTQ5XHU1MTY4XHU1QzQwXHU1M0Q4XHU5MUNGXHJcbiAgZGVmaW5lOiB7XHJcbiAgICBnbG9iYWw6ICdnbG9iYWxUaGlzJyxcclxuICB9LFxyXG4gIFxyXG4gIC8vIFx1NUYwMFx1NTNEMVx1NjcwRFx1NTJBMVx1NTY2OFx1OTE0RFx1N0Y2RVxyXG4gIHNlcnZlcjoge1xyXG4gICAgcG9ydDogMzAwMCxcclxuICAgIG9wZW46IHRydWUsXHJcbiAgICBwcm94eToge1xyXG4gICAgICAvLyBcdTRFRTNcdTc0MDZBUElcdThCRjdcdTZDNDJcdTUyMzBcdTU0MEVcdTdBRUZcclxuICAgICAgJy9hcGknOiB7XHJcbiAgICAgICAgdGFyZ2V0OiAnaHR0cDovL2xvY2FsaG9zdDo4MDgwJyxcclxuICAgICAgICBjaGFuZ2VPcmlnaW46IHRydWUsXHJcbiAgICAgICAgLy8gcmV3cml0ZTogKHBhdGgpID0+IHBhdGgucmVwbGFjZSgvXlxcL2FwaS8sICcnKVxyXG4gICAgICB9LFxyXG4gICAgICAvLyBcdTRFRTNcdTc0MDZcdTk3NTlcdTYwMDFcdThENDRcdTZFOTBcdThCRjdcdTZDNDJcclxuICAgICAgJy91cGxvYWRzJzoge1xyXG4gICAgICAgIHRhcmdldDogJ2h0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9hcGknLFxyXG4gICAgICAgIGNoYW5nZU9yaWdpbjogdHJ1ZVxyXG4gICAgICB9XHJcbiAgICB9XHJcbiAgfSxcclxuICBcclxuICAvLyBcdThERUZcdTVGODRcdTUyMkJcdTU0MERcclxuICByZXNvbHZlOiB7XHJcbiAgICBhbGlhczoge1xyXG4gICAgICAnQCc6IHJlc29sdmUoX19kaXJuYW1lLCAnc3JjJyksXHJcbiAgICAgICdAY29tcG9uZW50cyc6IHJlc29sdmUoX19kaXJuYW1lLCAnc3JjL2NvbXBvbmVudHMnKSxcclxuICAgICAgJ0B2aWV3cyc6IHJlc29sdmUoX19kaXJuYW1lLCAnc3JjL3ZpZXdzJyksXHJcbiAgICAgICdAc3RvcmVzJzogcmVzb2x2ZShfX2Rpcm5hbWUsICdzcmMvc3RvcmVzJyksXHJcbiAgICAgICdAdXRpbHMnOiByZXNvbHZlKF9fZGlybmFtZSwgJ3NyYy91dGlscycpLFxyXG4gICAgICAnQGFzc2V0cyc6IHJlc29sdmUoX19kaXJuYW1lLCAnc3JjL2Fzc2V0cycpXHJcbiAgICB9XHJcbiAgfSxcclxuICBcclxuICAvLyBDU1NcdTkxNERcdTdGNkVcclxuICBjc3M6IHtcclxuICAgIHByZXByb2Nlc3Nvck9wdGlvbnM6IHtcclxuICAgICAgc2Nzczoge1xyXG4gICAgICAgIGFkZGl0aW9uYWxEYXRhOiBgYFxyXG4gICAgICB9XHJcbiAgICB9XHJcbiAgfVxyXG59KVxyXG4iXSwKICAibWFwcGluZ3MiOiAiO0FBQW1OLFNBQVMsb0JBQW9CO0FBQ2hQLE9BQU8sU0FBUztBQUNoQixTQUFTLGVBQWU7QUFGeEIsSUFBTSxtQ0FBbUM7QUFRekMsSUFBTyxzQkFBUSxhQUFhO0FBQUEsRUFDMUIsU0FBUyxDQUFDLElBQUksQ0FBQztBQUFBO0FBQUEsRUFHZixRQUFRO0FBQUEsSUFDTixRQUFRO0FBQUEsRUFDVjtBQUFBO0FBQUEsRUFHQSxRQUFRO0FBQUEsSUFDTixNQUFNO0FBQUEsSUFDTixNQUFNO0FBQUEsSUFDTixPQUFPO0FBQUE7QUFBQSxNQUVMLFFBQVE7QUFBQSxRQUNOLFFBQVE7QUFBQSxRQUNSLGNBQWM7QUFBQTtBQUFBLE1BRWhCO0FBQUE7QUFBQSxNQUVBLFlBQVk7QUFBQSxRQUNWLFFBQVE7QUFBQSxRQUNSLGNBQWM7QUFBQSxNQUNoQjtBQUFBLElBQ0Y7QUFBQSxFQUNGO0FBQUE7QUFBQSxFQUdBLFNBQVM7QUFBQSxJQUNQLE9BQU87QUFBQSxNQUNMLEtBQUssUUFBUSxrQ0FBVyxLQUFLO0FBQUEsTUFDN0IsZUFBZSxRQUFRLGtDQUFXLGdCQUFnQjtBQUFBLE1BQ2xELFVBQVUsUUFBUSxrQ0FBVyxXQUFXO0FBQUEsTUFDeEMsV0FBVyxRQUFRLGtDQUFXLFlBQVk7QUFBQSxNQUMxQyxVQUFVLFFBQVEsa0NBQVcsV0FBVztBQUFBLE1BQ3hDLFdBQVcsUUFBUSxrQ0FBVyxZQUFZO0FBQUEsSUFDNUM7QUFBQSxFQUNGO0FBQUE7QUFBQSxFQUdBLEtBQUs7QUFBQSxJQUNILHFCQUFxQjtBQUFBLE1BQ25CLE1BQU07QUFBQSxRQUNKLGdCQUFnQjtBQUFBLE1BQ2xCO0FBQUEsSUFDRjtBQUFBLEVBQ0Y7QUFDRixDQUFDOyIsCiAgIm5hbWVzIjogW10KfQo=
