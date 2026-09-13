<template>
  <el-container class="app-container">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <i class="el-icon-wind"></i>
        <span>浪道设备管理系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        router
        background-color="#1a1a2e"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/dashboard">
          <i class="el-icon-s-home"></i>
          <span>数据概览</span>
        </el-menu-item>
        <el-menu-item index="/equipment">
          <i class="el-icon-s-tools"></i>
          <span>设备管理</span>
        </el-menu-item>
        <el-menu-item index="/binding">
          <i class="el-icon-link"></i>
          <span>档位绑定</span>
        </el-menu-item>
        <el-menu-item index="/statistics">
          <i class="el-icon-data-line"></i>
          <span>统计看板</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-title">
          <router-view v-slot="{ Component }">
            <component :is="Component" />
          </router-view>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const activeMenu = computed(() => route.path)
</script>

<style lang="scss">
.app-container {
  height: 100vh;
}

.sidebar {
  background-color: #1a1a2e;
  display: flex;
  flex-direction: column;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 16px;
    font-weight: bold;
    border-bottom: 1px solid #2d2d44;

    i {
      margin-right: 8px;
      font-size: 20px;
    }
  }

  .sidebar-menu {
    flex: 1;
    border-right: none;
  }
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
  display: flex;
  align-items: center;

  .header-title {
    font-size: 18px;
    font-weight: bold;
    color: #303133;
  }
}

.main {
  padding: 20px;
  background-color: #f5f7fa;
}

:deep(.el-menu-item) {
  margin: 4px 0;

  &:hover {
    background-color: rgba(64, 158, 255, 0.1);
  }
}
</style>
