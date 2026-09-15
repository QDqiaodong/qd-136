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
        <el-menu-item index="/wave-level">
          <i class="el-icon-s-flag"></i>
          <span>档位管理</span>
        </el-menu-item>
        <el-menu-item index="/binding">
          <i class="el-icon-link"></i>
          <span>档位绑定</span>
        </el-menu-item>
        <el-menu-item index="/statistics">
          <i class="el-icon-data-line"></i>
          <span>统计看板</span>
        </el-menu-item>
        <el-menu-item index="/inspection">
          <i class="el-icon-circle-check"></i>
          <span>开浪前点检</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-title">浪道辅助设备浪高档位绑定</div>
        <div class="header-role">
          <el-tag :type="authStore.role === 'DIRECTOR' ? 'danger' : 'warning'" effect="dark" class="role-tag">
            {{ authStore.roleName }}
          </el-tag>
          <el-select
            :model-value="authStore.role"
            class="role-select"
            @change="onRoleChange"
          >
            <el-option label="馆长（全部档位）" value="DIRECTOR" />
            <el-option label="浪道教练（授权档位）" value="COACH" />
          </el-select>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authStore, loadAuth, switchRole, type Role } from '@/auth'

const route = useRoute()

const activeMenu = computed(() => route.path)

const onRoleChange = async (role: Role) => {
  await switchRole(role)
  ElMessage.success(`已切换为${authStore.roleName}视角，列表已按授权范围刷新`)
  // 整页刷新，确保各视图完全按新角色重新拉取数据
  window.location.reload()
}

onMounted(() => {
  loadAuth()
})
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
  justify-content: space-between;

  .header-title {
    font-size: 18px;
    font-weight: bold;
    color: #303133;
  }

  .header-role {
    display: flex;
    align-items: center;
    gap: 10px;

    .role-tag {
      font-size: 13px;
    }

    .role-select {
      width: 200px;
    }
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
