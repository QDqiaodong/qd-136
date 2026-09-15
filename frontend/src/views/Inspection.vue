<template>
  <div class="inspection-page">
    <el-result
      v-if="denied"
      icon="error"
      title="无访问权限"
      sub-title="开浪前点检盘点是馆长开浪前的职责，浪道教练账号不可见、不可操作。请在右上角切换为馆长视角。"
    />
    <template v-else>
    <el-alert
      class="page-banner"
      type="error"
      :closable="false"
      show-icon
      title="开浪前点检盘点（今晚）：入浪防滑扶手记握把打滑次数，入浪缓冲挡垫记挡垫移位厘米。同一台设备复点只更新不新增，合计只算一次。"
    />

    <!-- 今晚两个合计：改任意一条数字后立即重算 -->
    <div class="summary-card">
      <div class="summary-item slip">
        <div class="summary-icon"><i class="el-icon-warning-outline"></i></div>
        <div class="summary-info">
          <div class="summary-value">{{ summary.totalSlipCount }} <span class="unit">次</span></div>
          <div class="summary-label">今晚握把打滑合计</div>
        </div>
      </div>
      <div class="summary-item shift">
        <div class="summary-icon"><i class="el-icon-place"></i></div>
        <div class="summary-info">
          <div class="summary-value">{{ summary.totalShiftCm }} <span class="unit">cm</span></div>
          <div class="summary-label">今晚挡垫移位合计</div>
        </div>
      </div>
      <div class="summary-item count">
        <div class="summary-icon"><i class="el-icon-s-finance"></i></div>
        <div class="summary-info">
          <div class="summary-value">{{ summary.inspectedCount }} <span class="unit">台</span></div>
          <div class="summary-label">今晚已点设备（去重）</div>
        </div>
      </div>
    </div>

    <div class="content-grid">
      <!-- 录入 / 复点 -->
      <el-card class="entry-card" shadow="never">
        <template #header>
          <span class="card-title">点检录入</span>
        </template>
        <el-form :model="form" label-width="110px">
          <el-form-item label="点检设备" required>
            <el-select
              v-model="form.equipmentId"
              placeholder="选择防滑扶手 / 缓冲挡垫"
              filterable
              style="width: 100%"
              @change="onEquipmentChange"
            >
              <el-option
                v-for="eq in equipments"
                :key="eq.id"
                :label="`${eq.equipmentCode} - ${eq.equipmentName}（${eq.equipmentType}）`"
                :value="eq.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item v-if="selectedEquipment" label="设备类型">
            <el-tag :type="selectedEquipment.equipmentType === HANDRAIL ? 'primary' : 'success'">
              {{ selectedEquipment.equipmentType }}
            </el-tag>
            <span v-if="isReInspection" class="reinspect-hint">
              今晚已点过，再点将覆盖原数字（合计仍只算这一台）
            </span>
          </el-form-item>

          <!-- 防滑扶手：握把打滑次数 -->
          <el-form-item v-if="showSlip" label="打滑次数" required>
            <el-input-number v-model="form.slipCount" :min="0" :step="1" controls-position="right" />
            <span class="field-unit">次</span>
          </el-form-item>

          <!-- 缓冲挡垫：移位厘米 -->
          <el-form-item v-if="showShift" label="挡垫移位" required>
            <el-input-number v-model="form.shiftCm" :min="0" :step="0.5" :precision="1" controls-position="right" />
            <span class="field-unit">厘米</span>
          </el-form-item>

          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="异常描述 / 处理情况（选填）" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="doSubmit">
              {{ isReInspection ? '复点并更新' : '记入职检' }}
            </el-button>
            <el-button @click="resetForm">清空</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 现场对账 -->
      <el-card class="reconcile-card" shadow="never">
        <template #header>
          <span class="card-title">现场对账</span>
        </template>
        <el-form label-width="150px">
          <el-form-item label="现场打滑合计（次）">
            <el-input-number v-model="onSiteSlip" :min="0" :step="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="现场移位合计（cm）">
            <el-input-number v-model="onSiteShift" :min="0" :step="0.5" :precision="1" controls-position="right" />
          </el-form-item>
          <el-form-item>
            <el-button type="warning" @click="doReconcile">对账</el-button>
          </el-form-item>
        </el-form>

        <div v-if="reconcile" class="reconcile-result">
          <el-alert
            :title="reconcile.balanced ? '账实相符，可以开浪' : '账实不符，请复核现场计数'"
            :type="reconcile.balanced ? 'success' : 'error'"
            :closable="false"
            show-icon
            class="reconcile-alert"
          />
          <el-descriptions :column="1" border size="small" class="reconcile-desc">
            <el-descriptions-item>
              打滑：页上 {{ reconcile.sheetSlipCount }} 次 / 现场 {{ reconcile.onSiteSlipCount }} 次
              <el-tag :type="reconcile.slipMatched ? 'success' : 'danger'" size="small" class="diff-tag">
                {{ reconcile.slipMatched ? '相符' : `差 ${formatDiff(reconcile.slipDiff)}` }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item>
              移位：页上 {{ reconcile.sheetShiftCm }} cm / 现场 {{ reconcile.onSiteShiftCm }} cm
              <el-tag :type="reconcile.shiftMatched ? 'success' : 'danger'" size="small" class="diff-tag">
                {{ reconcile.shiftMatched ? '相符' : `差 ${formatDiff(reconcile.shiftDiff)} cm` }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-card>
    </div>

    <!-- 今晚点检台账 -->
    <el-card class="ledger-card" shadow="never">
      <template #header>
        <div class="ledger-header">
          <span class="card-title">今晚点检台账（{{ today }}）</span>
          <el-button text type="primary" :loading="loading" @click="fetchAll">
            <i class="el-icon-refresh"></i> 刷新
          </el-button>
        </div>
      </template>
      <el-table :data="records" border stripe v-loading="loading">
        <el-table-column prop="equipmentCode" label="设备编号" width="130" />
        <el-table-column prop="equipmentName" label="设备名称" min-width="170" />
        <el-table-column label="设备类型" width="110">
          <template #default="scope">
            <el-tag :type="scope.row.equipmentType === HANDRAIL ? 'primary' : 'success'">
              {{ scope.row.equipmentType }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 数字改完即保存，两个合计马上跟上 -->
        <el-table-column label="握把打滑（次）" width="160">
          <template #default="scope">
            <el-input-number
              :model-value="scope.row.slipCount"
              :min="0"
              :step="1"
              :disabled="scope.row.equipmentType !== HANDRAIL"
              size="small"
              controls-position="right"
              @change="(v: number) => onCellEdit(scope.row, 'slipCount', v)"
            />
          </template>
        </el-table-column>
        <el-table-column label="挡垫移位（cm）" width="160">
          <template #default="scope">
            <el-input-number
              :model-value="scope.row.shiftCm"
              :min="0"
              :step="0.5"
              :precision="1"
              :disabled="scope.row.equipmentType !== BUFFER_PAD"
              size="small"
              controls-position="right"
              @change="(v: number) => onCellEdit(scope.row, 'shiftCm', v)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140">
          <template #default="scope">{{ scope.row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="scope">
            <el-popconfirm title="撤回该条点检记录？合计将随之减少" @confirm="doDelete(scope.row.id)">
              <template #reference>
                <el-button text type="danger" size="small">撤回</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="今晚还没有点检记录，开浪前请先逐台点检" :image-size="80" />
        </template>
      </el-table>
    </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  inspectionApi,
  type Equipment,
  type PreWaveInspection,
  type PreWaveInspectionSummary,
  type PreWaveReconcileResult
} from '@/api'
import { loadAuth, isCoach } from '@/auth'

const HANDRAIL = '防滑扶手'
const BUFFER_PAD = '缓冲挡垫'

const today = new Date().toISOString().slice(0, 10)

const loading = ref(false)
const submitting = ref(false)
const denied = ref(false)
const equipments = ref<Equipment[]>([])
const records = ref<PreWaveInspection[]>([])
const reconcile = ref<PreWaveReconcileResult | null>(null)
const onSiteSlip = ref(0)
const onSiteShift = ref(0)

const summary = reactive<PreWaveInspectionSummary>({
  inspectionDate: today,
  inspectedCount: 0,
  totalSlipCount: 0,
  totalShiftCm: 0
})

const form = reactive({
  equipmentId: null as number | null,
  slipCount: 0,
  shiftCm: 0,
  remark: ''
})

const selectedEquipment = computed(() =>
  equipments.value.find((e) => e.id === form.equipmentId) ?? null
)

// 只有防滑扶手记打滑；只有缓冲挡垫记移位
const showSlip = computed(() => selectedEquipment.value?.equipmentType === HANDRAIL)
const showShift = computed(() => selectedEquipment.value?.equipmentType === BUFFER_PAD)

// 同一台设备今晚是否已点过（台账里有它）
const isReInspection = computed(() =>
  form.equipmentId != null && records.value.some((r) => r.equipmentId === form.equipmentId)
)

const fetchEquipments = async () => {
  const res = await inspectionApi.getInspectableEquipments()
  equipments.value = res.data
}

const fetchRecords = async () => {
  const res = await inspectionApi.listToday()
  records.value = res.data
}

const fetchSummary = async () => {
  const res = await inspectionApi.getSummary()
  Object.assign(summary, res.data)
}

const fetchAll = async () => {
  loading.value = true
  try {
    // 明细与合计各自独立请求后端；合计始终由服务端按去重后的数据重算
    await Promise.all([fetchRecords(), fetchSummary()])
  } catch (error: any) {
    ElMessage.error(error.message || '加载点检数据失败')
  } finally {
    loading.value = false
  }
}

const onEquipmentChange = () => {
  // 复点时把这台设备今晚已有的数字带进表单，方便在原值上修改
  const existing = records.value.find((r) => r.equipmentId === form.equipmentId)
  form.slipCount = existing ? existing.slipCount : 0
  form.shiftCm = existing ? Number(existing.shiftCm) : 0
  form.remark = existing?.remark || ''
}

const resetForm = () => {
  form.equipmentId = null
  form.slipCount = 0
  form.shiftCm = 0
  form.remark = ''
}

const doSubmit = async () => {
  if (form.equipmentId == null) {
    ElMessage.warning('请先选择要点检的设备')
    return
  }
  submitting.value = true
  try {
    const res = await inspectionApi.submit({
      equipmentId: form.equipmentId,
      slipCount: showSlip.value ? form.slipCount : 0,
      shiftCm: showShift.value ? form.shiftCm : 0,
      remark: form.remark || undefined
    })
    ElMessage.success(res.data.reInspected ? '复点成功：已更新该设备记录，合计仍只算这一台' : '点检已记')
    resetForm()
    await fetchAll()
    // 数字可能变化，现场对账结果失效，需要重新对账
    reconcile.value = null
  } catch (error: any) {
    ElMessage.error(error.message || '点检提交失败')
  } finally {
    submitting.value = false
  }
}

// 台账内直接改一条数字：以同设备同晚“更新”方式提交，随后刷新合计
const onCellEdit = async (
  row: PreWaveInspection,
  field: 'slipCount' | 'shiftCm',
  value: number
) => {
  const safeValue = value ?? 0
  try {
    await inspectionApi.submit({
      equipmentId: row.equipmentId,
      slipCount: field === 'slipCount' ? safeValue : row.slipCount,
      shiftCm: field === 'shiftCm' ? safeValue : Number(row.shiftCm),
      remark: row.remark || undefined
    })
    ElMessage.success('数字已更新，合计已同步')
    await fetchAll()
    reconcile.value = null
  } catch (error: any) {
    ElMessage.error(error.message || '更新失败')
    await fetchRecords()
  }
}

const doDelete = async (id: number) => {
  try {
    await inspectionApi.remove(id)
    ElMessage.success('已撤回，合计已同步')
    await fetchAll()
    reconcile.value = null
  } catch (error: any) {
    ElMessage.error(error.message || '撤回失败')
  }
}

const formatDiff = (v: number) => (v > 0 ? `+${v}` : `${v}`)

const doReconcile = async () => {
  try {
    const res = await inspectionApi.reconcile({
      onSiteSlipCount: onSiteSlip.value,
      onSiteShiftCm: onSiteShift.value
    })
    reconcile.value = res.data
    if (res.data.balanced) {
      ElMessage.success('账实相符，可以开浪')
    } else {
      ElMessage.warning('账实不符，请复核现场计数')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '对账失败')
  }
}

onMounted(async () => {
  await loadAuth()
  if (isCoach()) {
    // 后端同样以 403 兜底，前端仅提前收窄展示
    denied.value = true
    return
  }
  await fetchEquipments()
  await fetchAll()
})
</script>

<style lang="scss">
.inspection-page {
  display: flex;
  flex-direction: column;
  gap: 18px;

  .page-banner {
    margin: 0;
  }

  .card-title {
    font-size: 16px;
    font-weight: bold;
    color: #303133;
  }

  .summary-card {
    display: flex;
    gap: 20px;

    .summary-item {
      flex: 1;
      background: #fff;
      border-radius: 12px;
      padding: 22px;
      display: flex;
      align-items: center;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

      .summary-icon {
        width: 52px;
        height: 52px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 26px;
        color: #fff;
        margin-right: 16px;
      }

      .summary-value {
        font-size: 30px;
        font-weight: bold;
        color: #303133;
        line-height: 1.1;

        .unit {
          font-size: 14px;
          font-weight: normal;
          color: #909399;
        }
      }

      .summary-label {
        font-size: 14px;
        color: #909399;
        margin-top: 6px;
      }

      &.slip .summary-icon {
        background: linear-gradient(135deg, #f6d365 0%, #f56c6c 100%);
      }

      &.shift .summary-icon {
        background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
      }

      &.count .summary-icon {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      }
    }
  }

  .content-grid {
    display: grid;
    grid-template-columns: 1.2fr 1fr;
    gap: 20px;

    .entry-card,
    .reconcile-card {
      border-radius: 12px;

      :deep(.el-card__body) {
        padding-top: 12px;
      }
    }
  }

  .field-unit {
    margin-left: 10px;
    color: #909399;
    font-size: 13px;
  }

  .reinspect-hint {
    margin-left: 12px;
    font-size: 12px;
    color: #e6a23c;
  }

  .reconcile-result {
    margin-top: 8px;

    .reconcile-alert {
      margin-bottom: 12px;
    }

    .diff-tag {
      margin-left: 8px;
    }
  }

  .ledger-card {
    border-radius: 12px;

    .ledger-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
  }
}
</style>
