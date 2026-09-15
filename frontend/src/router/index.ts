import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue')
  },
  {
    path: '/equipment',
    name: 'Equipment',
    component: () => import('@/views/Equipment.vue')
  },
  {
    path: '/binding',
    name: 'Binding',
    component: () => import('@/views/Binding.vue')
  },
  {
    path: '/statistics',
    name: 'Statistics',
    component: () => import('@/views/Statistics.vue')
  },
  {
    path: '/inspection',
    name: 'Inspection',
    component: () => import('@/views/Inspection.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
