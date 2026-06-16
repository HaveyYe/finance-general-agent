/**
 * 应用入口文件
 * 创建 Vue 实例，安装插件并挂载到 DOM
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'

// 创建 Vue 应用实例
const app = createApp(App)

// 注册 Element Plus 所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 安装插件
app.use(createPinia()) // 状态管理
app.use(router)        // 路由
app.use(ElementPlus)   // UI 组件库

// 挂载到 #app
app.mount('#app')
