<!--
  应用根组件
  左侧导航栏 + 右侧工作区的整体布局
-->
<template>
  <div class="app-layout">
    <!-- 左侧导航栏 -->
    <aside class="sidebar">
      <!-- 品牌标识 -->
      <div class="sidebar-brand">
        <div class="brand-icon">
          <el-icon :size="28"><TrendCharts /></el-icon>
        </div>
        <div class="brand-text">
          <h1 class="brand-title">财务数智人</h1>
          <p class="brand-subtitle">Finance Agent</p>
        </div>
      </div>

      <!-- 导航菜单 -->
      <nav class="sidebar-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: $route.path === item.path }"
        >
          <span class="nav-icon">{{ item.icon }}</span>
          <span class="nav-label">{{ item.label }}</span>
        </router-link>
      </nav>

      <!-- 底部信息 -->
      <div class="sidebar-footer">
        <div class="version-info">v0.0.1</div>
      </div>
    </aside>

    <!-- 右侧工作区 -->
    <div class="main-area">
      <!-- 顶部状态栏 -->
      <header class="top-bar">
        <div class="top-bar-left">
          <h2 class="page-title">数智工作台</h2>
        </div>
        <div class="top-bar-right">
          <el-tag type="success" effect="dark" round>
            <el-icon class="tag-icon"><Connection /></el-icon>
            本地联调
          </el-tag>
        </div>
      </header>

      <!-- 主内容区 -->
      <main class="content-area">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

/** 导航菜单项配置 */
const navItems = ref([
  { path: '/chat', icon: '💬', label: '对话' },
  { path: '/expense', icon: '🧾', label: '报销' },
  { path: '/knowledge', icon: '📚', label: '知识库' },
])
</script>

<style scoped>
/* 整体布局：左右分栏 */
.app-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background: #f0f2f5;
}

/* ====== 左侧导航栏 ====== */
.sidebar {
  width: 220px;
  min-width: 220px;
  background: linear-gradient(180deg, #1a1f36 0%, #121629 100%);
  display: flex;
  flex-direction: column;
  color: #fff;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.15);
  z-index: 10;
}

/* 品牌标识区域 */
.sidebar-brand {
  padding: 24px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-icon {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #409eff 0%, #53a8ff 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.brand-text {
  overflow: hidden;
}

.brand-title {
  font-size: 17px;
  font-weight: 700;
  margin: 0;
  white-space: nowrap;
  letter-spacing: 1px;
}

.brand-subtitle {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.45);
  margin: 4px 0 0;
  letter-spacing: 0.5px;
}

/* 导航菜单 */
.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 16px;
  border-radius: 10px;
  color: rgba(255, 255, 255, 0.65);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.25s ease;
  cursor: pointer;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.nav-item.active {
  background: linear-gradient(135deg, #409eff 0%, #53a8ff 100%);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.35);
}

.nav-icon {
  font-size: 18px;
  width: 24px;
  text-align: center;
}

.nav-label {
  font-size: 14px;
}

/* 底部版本信息 */
.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.version-info {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.25);
  text-align: center;
}

/* ====== 右侧工作区 ====== */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* 顶部状态栏 */
.top-bar {
  height: 56px;
  min-height: 56px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  z-index: 5;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tag-icon {
  margin-right: 4px;
}

/* 主内容区域 */
.content-area {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* ====== 响应式布局 ====== */
@media (max-width: 768px) {
  .sidebar {
    width: 60px;
    min-width: 60px;
  }

  .brand-text {
    display: none;
  }

  .sidebar-brand {
    justify-content: center;
    padding: 16px 8px;
  }

  .nav-label {
    display: none;
  }

  .nav-item {
    justify-content: center;
    padding: 13px 8px;
  }
}
</style>
