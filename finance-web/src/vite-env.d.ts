/// <reference types="vite/client" />

// 声明 .vue 文件模块，使 TypeScript 能正确识别 Vue 单文件组件
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
