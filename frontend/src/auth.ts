import { reactive } from 'vue'
import { authApi } from '@/api'

const STORAGE_KEY = 'surf.current-role'

export type Role = 'DIRECTOR' | 'COACH'

/**
 * 当前操作者状态。角色保存在 localStorage，刷新页面后仍然保留；
 * 授权范围由后端按角色返回，作为前端视图收窄依据，最终越权拦截以后端为准。
 */
export const authStore = reactive({
  role: 'DIRECTOR' as Role,
  roleName: '馆长',
  /** 当前角色可见的浪高档位编码 */
  authorizedWaveLevelCodes: [] as string[],
  /** 教练可操作的辅助设备类型；馆长为空数组表示不限 */
  auxiliaryEquipmentTypes: [] as string[],
  loaded: false
})

const isRole = (v: string | null): v is Role => v === 'DIRECTOR' || v === 'COACH'

export function getStoredRole(): Role {
  const stored = localStorage.getItem(STORAGE_KEY)
  return isRole(stored) ? stored : 'DIRECTOR'
}

/** 切换角色并立即向后端拉取该角色的授权范围。 */
export async function switchRole(role: Role) {
  localStorage.setItem(STORAGE_KEY, role)
  await loadAuth()
}

/** 向后端确认当前角色的授权范围（刷新后列表只显示被授权设备的依据）。 */
export async function loadAuth() {
  const role = getStoredRole()
  try {
    const res = await authApi.getMe(role)
    authStore.role = res.data.role as Role
    authStore.roleName = res.data.roleName
    authStore.authorizedWaveLevelCodes = res.data.authorizedWaveLevelCodes
    authStore.auxiliaryEquipmentTypes = res.data.auxiliaryEquipmentTypes
  } catch {
    // 后端不可用时退回本地角色，授权范围置空，避免越权展示
    authStore.role = role
    authStore.roleName = role === 'DIRECTOR' ? '馆长' : '浪道教练'
    authStore.authorizedWaveLevelCodes = []
    authStore.auxiliaryEquipmentTypes = role === 'COACH' ? ['防滑扶手', '缓冲挡垫'] : []
  } finally {
    authStore.loaded = true
  }
}

export function isCoach() {
  return authStore.role === 'COACH'
}

export function isDirector() {
  return authStore.role === 'DIRECTOR'
}

/** 该浪高档位是否在当前角色可见范围内。 */
export function canAccessWaveLevel(code: string) {
  if (isDirector()) return true
  return authStore.authorizedWaveLevelCodes.includes(code)
}

/** 该设备是否在当前角色可操作范围内（教练限防滑扶手/缓冲挡垫）。 */
export function canAccessEquipment(equipment: { equipmentType: string }) {
  if (isDirector()) return true
  return authStore.auxiliaryEquipmentTypes.includes(equipment.equipmentType)
}
