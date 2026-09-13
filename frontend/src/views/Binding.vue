<template>
  <div class="binding-page">
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
                  <el-select v-model="bindForm.waveLevelCode">
                    <el-option
                      v-for="wl in waveLevels"
                      :key="wl.levelCode"
                      :label="`${wl.levelCode} - ${wl.levelName}`"
                      :value="wl.levelCode"
                    />
                  </el-select>
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
                      :key="eq.id"
                      :label="`${eq.equipmentCode} - ${eq.equipmentName} (当前: ${getEquipmentWaveLevel(eq.id)})`"
                      :value="eq.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="新浪高档位" required>
                  <el-select v-model="adjustForm.newWaveLevelCode">
                    <el-option
                      v-for="wl in waveLevels"
                      :key="wl.levelCode"
                      :label="`${wl.levelCode} - ${wl.levelName}`"
                      :value="wl.levelCode"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="调整原因">
                  <el-textarea v-model="adjustForm.adjustReason" :rows="3" />
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
              <el-table-column prop="equipmentType" label="设备类型" width="120" />
              <el-table-column prop="waveLevelCode" label="档位编码" width="100" />
              <el-table-column prop="waveLevelName" label="档位名称" width="100">
                <template #default="scope">
                  <el-tag :type="getLevelTagType(scope.row.waveLevelCode)">
                    {{ scope.row.waveLevelName }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="bindingType" label="绑定类型" width="100" />
              <el-table-column prop="effectiveDate" label="生效时间" width="180" />
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
import { ref, reactive, onMounted } from 'vue'
import { equipmentApi, waveLevelApi, bindingApi, type Equipment, type WaveLevel, type EquipmentWaveLevel, type EquipmentAdjustRecord } from '@/api'
import { ElMessage } from 'element-plus'

const activeTab = ref('bind')
const equipments = ref<Equipment[]>([])
const waveLevels = ref<WaveLevel[]>([])
const bindingStatus = ref<(Equipment & EquipmentWaveLevel)[]>([])
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

const boundEquipments = ref<{ id: number; equipmentCode: string; equipmentName: string }[]>([])
const equipmentWaveLevelMap = ref<Map<number, string>>(new Map())

const fetchEquipments = async () => {
  try {
    const res = await equipmentApi.getAll()
    equipments.value = res.data
  } catch (error) {
    console.error('获取设备失败:', error)
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

const fetchBindingStatus = async () => {
  try {
    const res = await equipmentApi.getAll()
    const allEquipments = res.data
    
    const bindingList: (Equipment & EquipmentWaveLevel)[] = []
    
    for (const eq of allEquipments) {
      try {
        const bindingRes = await bindingApi.getCurrentBinding(eq.id)
        if (bindingRes.data) {
          bindingList.push({ ...eq, ...bindingRes.data })
          equipmentWaveLevelMap.value.set(eq.id, bindingRes.data.waveLevelName)
        }
      } catch {
        continue
      }
    }
    
    bindingStatus.value = bindingList
    boundEquipments.value = bindingList.map(b => ({ id: b.id, equipmentCode: b.equipmentCode, equipmentName: b.equipmentName }))
  } catch (error) {
    console.error('获取绑定状态失败:', error)
  }
}

const fetchAdjustRecords = async () => {
  try {
    const res = await bindingApi.getAdjustRecords()
    adjustRecords.value = res.data
  } catch (error) {
    console.error('获取调整记录失败:', error)
  }
}

const getEquipmentWaveLevel = (equipmentId: number) => {
  return equipmentWaveLevelMap.value.get(equipmentId) || '未绑定'
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
    ElMessage.error(error.message || '绑定失败')
  }
}

const doAdjust = async () => {
  if (!adjustForm.equipmentId || !adjustForm.newWaveLevelCode || !adjustForm.operator) {
    ElMessage.warning('请填写完整信息')
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
    ElMessage.error(error.message || '调整失败')
  }
}

onMounted(() => {
  fetchEquipments()
  fetchWaveLevels()
  fetchBindingStatus()
  fetchAdjustRecords()
})
</script>

<style lang="scss">
.binding-page {
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
