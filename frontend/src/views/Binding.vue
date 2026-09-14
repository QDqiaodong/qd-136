<template>
  <div class="binding-page">
    <!-- 角色可见范围提示 -->
    <el-alert
      v-if="coachMode"
      class="role-banner"
      type="warning"
      :closable="false"
      show-icon
    >
      <template #title>
        当前为浪道教练视角：仅可查看并调整已授权档位（{{ authorizedLevelNames }}）上的
        <b>防滑扶手</b> 与 <b>缓冲挡垫</b>，未授权档位（{{ unauthorizedLevelNames }}）不可见、不可调整。
      </template>
    </el-alert>
    <el-alert
      v-else
      class="role-banner"
      type="success"
      :closable="false"
      show-icon
      title="当前为馆长视角：可见并操作全部浪高档位与辅助设备。"
    />

    <div class="page-tabs">
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="设备绑定" name="bind">
          <div class="tab-content">
            <div class="bind-section">
              <h3>初始绑定</h3>
              <el-form :model="bindForm" label-width="120px" class="bind-form">
                <el-form-item label="选择设备" required>
                  <el-select v-model="bindForm.equipmentId" placeholder="请选择设备">
                    <el-option
                      v-for="eq in equipments"
                      :key="eq.id"
                      :label="`${eq.equipmentCode} - ${eq.equipmentName}`"
                      :value="eq.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="浪高档位" required>
                  <el-select v-model="bindForm.waveLevelCode" placeholder="请选择浪高档位">
                    <el-option
                      v-for="wl in visibleWaveLevels"
                      :key="wl.levelCode"
                      :label="`${wl.levelCode} - ${wl.levelName}`"
                      :value="wl.levelCode"
                    />
                  </el-select>
                  <div v-if="coachMode" class="field-hint">仅显示已授权档位</div>
                </el-form-item>
                <el-form-item label="绑定类型">
                  <el-select v-model="bindForm.bindingType">
                    <el-option label="初始绑定" value="INITIAL" />
                    <el-option label="手动绑定" value="MANUAL" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="doBind">绑定</el-button>
                </el-form-item>
              </el-form>
            </div>

            <div class="bind-section">
              <h3>档位调整</h3>
              <el-form :model="adjustForm" label-width="120px" class="bind-form">
                <el-form-item label="选择设备" required>
                  <el-select v-model="adjustForm.equipmentId" placeholder="请选择已绑定设备">
                    <el-option
                      v-for="eq in boundEquipments"
                      :key="eq.equipmentId"
                      :label="`${eq.equipmentCode} - ${eq.equipmentName} (当前: ${eq.waveLevelName})`"
                      :value="eq.equipmentId"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="新浪高档位" required>
                  <el-select v-model="adjustForm.newWaveLevelCode" placeholder="请选择新浪高档位">
                    <el-option
                      v-for="wl in visibleWaveLevels"
                      :key="wl.levelCode"
                      :label="`${wl.levelCode} - ${wl.levelName}`"
                      :value="wl.levelCode"
                    />
                  </el-select>
                  <div v-if="coachMode" class="field-hint">仅可调整到已授权档位</div>
                </el-form-item>
                <el-form-item label="调整原因">
                  <el-input v-model="adjustForm.adjustReason" type="textarea" :rows="3" />
                </el-form-item>
                <el-form-item label="操作人" required>
                  <el-input v-model="adjustForm.operator" />
                </el-form-item>
                <el-form-item label="备注">
                  <el-input v-model="adjustForm.remark" />
                </el-form-item>
                <el-form-item>
                  <el-button type="warning" @click="doAdjust">调整</el-button>
                </el-form-item>
              </el-form>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="绑定状态" name="status">
          <div class="status-content">
            <el-table :data="bindingStatus" border>
              <el-table-column prop="equipmentCode" label="设备编号" width="120" />
              <el-table-column prop="equipmentName" label="设备名称" width="150" />
              <el-table-column prop="equipmentType" label="设备类型" width="120">
                <template #default="scope">
                  <el-tag :type="scope.row.equipmentType === '防滑扶手' ? 'primary' : 'success'">
                    {{ scope.row.equipmentType }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="waveLevelCode" label="档位编码" width="100" />
              <el-table-column prop="waveLevelName" label="档位名称" width="100">
                <template #default="scope">
                  <el-tag :type="getLevelTagType(scope.row.waveLevelCode)">
                    {{ scope.row.waveLevelName }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="bindingType" label="绑定类型" width="100" />
              <el-table-column prop="effectiveDate" label="生效时间" min-width="180" />
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane label="调整记录" name="records">
          <div class="records-content">
            <el-table :data="adjustRecords" border>
              <el-table-column prop="equipmentCode" label="设备编号" width="120" />
              <el-table-column prop="equipmentName" label="设备名称" width="150" />
              <el-table-column prop="previousWaveLevelName" label="原档位" width="100">
                <template #default="scope">
                  {{ scope.row.previousWaveLevelName || '-' }}
                </template>
              </el-table-column>
              <el-table-column prop="newWaveLevelName" label="新档位" width="100">
                <template #default="scope">
                  <el-tag type="success">{{ scope.row.newWaveLevelName }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="adjustReason" label="调整原因" />
              <el-table-column prop="operator" label="操作人" width="100" />
              <el-table-column prop="adjustTime" label="调整时间" width="180" />
              <el-table-column prop="remark" label="备注" />
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import {
  waveLevelApi,
  bindingApi,
  type Equipment,
  type WaveLevel,
  type BindingStatus,
  type EquipmentAdjustRecord
} from '@/api'
import { loadAuth, isCoach, canAccessWaveLevel } from '@/auth'
import { ElMessage } from 'element-plus'

const activeTab = ref('bind')
// 初始绑定可选设备：由后端按角色收窄（教练仅防滑扶手/缓冲挡垫）
const equipments = ref<Equipment[]>([])
const waveLevels = ref<WaveLevel[]>([])
// 绑定状态列表：由后端按角色收窄（教练仅授权档位上的辅助设备）
const bindingStatus = ref<BindingStatus[]>([])
const adjustRecords = ref<EquipmentAdjustRecord[]>([])

const bindForm = reactive({
  equipmentId: null as number | null,
  waveLevelCode: '',
  bindingType: 'INITIAL'
})

const adjustForm = reactive({
  equipmentId: null as number | null,
  newWaveLevelCode: '',
  adjustReason: '',
  operator: '',
  remark: ''
})

// 档位调整下拉：来自收窄后的绑定状态，教练天然看不到未授权档位上的设备
const boundEquipments = computed(() => bindingStatus.value)

// 模板使用的教练标识（脚本内用 isCoach() 函数）
const coachMode = computed(() => isCoach())

// 档位下拉按角色收窄：教练只显示授权档位
const visibleWaveLevels = computed(() =>
  waveLevels.value.filter((wl) => canAccessWaveLevel(wl.levelCode))
)

const authorizedLevelNames = computed(() =>
  visibleWaveLevels.value.map((wl) => wl.levelName).join('、') || '无'
)

const unauthorizedLevelNames = computed(() =>
  waveLevels.value
    .filter((wl) => !canAccessWaveLevel(wl.levelCode))
    .map((wl) => wl.levelName)
    .join('、') || '无'
)

const fetchEquipments = async () => {
  try {
    const res = await bindingApi.getBindableEquipments()
    equipments.value = res.data
  } catch (error: any) {
    ElMessage.error(error.message || '获取设备失败')
  }
}

const fetchWaveLevels = async () => {
  try {
    const res = await waveLevelApi.getAll()
    waveLevels.value = res.data
  } catch (error) {
    console.error('获取浪高档位失败:', error)
  }
}

// 绑定状态：直接使用后端按角色收窄后的列表，刷新后仍只显示当前角色被授权的设备
const fetchBindingStatus = async () => {
  try {
    const res = await bindingApi.getActiveBindings()
    bindingStatus.value = res.data
  } catch (error: any) {
    ElMessage.error(error.message || '获取绑定状态失败')
  }
}

const fetchAdjustRecords = async () => {
  try {
    const res = await bindingApi.getAdjustRecords()
    adjustRecords.value = res.data
  } catch (error: any) {
    ElMessage.error(error.message || '获取调整记录失败')
  }
}

const getLevelTagType = (code: string) => {
  const types: Record<string, string> = {
    LOW: 'success',
    MEDIUM: 'warning',
    HIGH: 'danger'
  }
  return types[code] || 'info'
}

const doBind = async () => {
  if (!bindForm.equipmentId || !bindForm.waveLevelCode) {
    ElMessage.warning('请填写完整信息')
    return
  }
  // 前端预校验，给即时反馈；真正拦截以后端为准
  if (isCoach() && !canAccessWaveLevel(bindForm.waveLevelCode)) {
    ElMessage.error('没有权限：该浪高档位未授权给当前教练')
    return
  }

  try {
    await bindingApi.bind({
      equipmentId: bindForm.equipmentId,
      waveLevelCode: bindForm.waveLevelCode,
      bindingType: bindForm.bindingType
    })
    ElMessage.success('绑定成功')
    bindForm.equipmentId = null
    bindForm.waveLevelCode = ''
    fetchBindingStatus()
  } catch (error: any) {
    // 越权时后端返回 403，提示“没有权限”
    ElMessage.error(error.message || '绑定失败')
  }
}

const doAdjust = async () => {
  if (!adjustForm.equipmentId || !adjustForm.newWaveLevelCode || !adjustForm.operator) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (isCoach() && !canAccessWaveLevel(adjustForm.newWaveLevelCode)) {
    ElMessage.error('没有权限：目标浪高档位未授权给当前教练')
    return
  }

  try {
    await bindingApi.adjust({
      equipmentId: adjustForm.equipmentId,
      newWaveLevelCode: adjustForm.newWaveLevelCode,
      adjustReason: adjustForm.adjustReason,
      operator: adjustForm.operator,
      remark: adjustForm.remark
    })
    ElMessage.success('调整成功')
    adjustForm.equipmentId = null
    adjustForm.newWaveLevelCode = ''
    adjustForm.adjustReason = ''
    adjustForm.operator = ''
    adjustForm.remark = ''
    fetchBindingStatus()
    fetchAdjustRecords()
  } catch (error: any) {
    // 教练越权改未授权档位绑定时，后端拦截并返回 403“没有权限……”
    ElMessage.error(error.message || '调整失败')
  }
}

onMounted(async () => {
  // 先确认当前角色授权范围（刷新页面后从 localStorage 恢复角色，再向后端取范围）
  await loadAuth()
  await Promise.all([
    fetchWaveLevels(),
    fetchEquipments(),
    fetchBindingStatus(),
    fetchAdjustRecords()
  ])
})
</script>

<style lang="scss">
.binding-page {
  .role-banner {
    margin-bottom: 16px;
  }

  .field-hint {
    font-size: 12px;
    color: #e6a23c;
    line-height: 1.4;
    margin-top: 4px;
  }

  .page-tabs {
    :deep(.el-tabs__content) {
      padding: 20px 0;
    }
  }

  .tab-content {
    display: flex;
    gap: 40px;

    .bind-section {
      flex: 1;

      h3 {
        margin-bottom: 20px;
        font-size: 16px;
        font-weight: bold;
        color: #303133;
      }

      .bind-form {
        background: #fff;
        padding: 24px;
        border-radius: 12px;
        box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
      }
    }
  }

  .status-content,
  .records-content {
    background: #fff;
    padding: 24px;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  }
}
</style>
