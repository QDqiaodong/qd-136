<template>
  <div class="dashboard">
    <div class="stats-card">
      <div class="stat-item">
        <div class="stat-icon equipment-icon">
          <i class="el-icon-s-tools"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ overview.totalEquipment }}</div>
          <div class="stat-label">设备总数</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon level-icon">
          <i class="el-icon-sunny"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ overview.totalWaveLevel }}</div>
          <div class="stat-label">浪高档位数</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon binding-icon">
          <i class="el-icon-link"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ overview.totalBinding }}</div>
          <div class="stat-label">已绑定设备</div>
        </div>
      </div>
    </div>

    <!-- 馆长交接班：今晚开了哪些浪高档、当班教练是谁 -->
    <div class="wave-open-card">
      <div class="card-header">
        <span class="card-title">今晚开浪档位（{{ today }}）</span>
        <el-tag v-if="savedAt" type="success" size="small" effect="plain">
          已记录 · {{ savedAt }}
        </el-tag>
      </div>
      <div class="wave-open-body">
        <div class="wave-open-row">
          <span class="row-label">开浪档位</span>
          <el-checkbox-group v-model="openedLevels" class="level-checkbox-group">
            <el-checkbox
              v-for="level in waveLevels"
              :key="level.levelCode"
              :value="level.levelCode"
            >
              {{ level.levelName }}
            </el-checkbox>
          </el-checkbox-group>
        </div>
        <div class="wave-open-row">
          <span class="row-label">当班教练</span>
          <el-input
            v-model="coachName"
            class="coach-input"
            placeholder="请填写当班教练姓名"
            clearable
            maxlength="20"
          />
          <el-button type="primary" :loading="saving" @click="saveWaveOpen">保存记录</el-button>
        </div>
      </div>
    </div>

    <div class="chart-card">
      <div class="card-header">
        <span class="card-title">浪高档位设备分布</span>
      </div>
      <div ref="chartRef" class="chart-container"></div>
    </div>

    <div class="recent-records">
      <div class="card-header">
        <span class="card-title">最近调整记录</span>
      </div>
      <el-table :data="adjustRecords" border>
        <el-table-column prop="equipmentCode" label="设备编号" width="120" />
        <el-table-column prop="equipmentName" label="设备名称" width="150" />
        <el-table-column prop="previousWaveLevelName" label="原档位" width="100" />
        <el-table-column prop="newWaveLevelName" label="新档位" width="100" />
        <el-table-column prop="adjustReason" label="调整原因" />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="adjustTime" label="调整时间" width="180" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { statisticsApi, bindingApi, waveLevelApi, type EquipmentAdjustRecord, type WaveLevel } from '@/api'

const overview = ref({ totalEquipment: 0, totalWaveLevel: 0, totalBinding: 0 })
const adjustRecords = ref<EquipmentAdjustRecord[]>([])
const chartRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

// ===== 今晚开浪档位 / 当班教练（交接班记录，按晚存浏览器，关页再开仍在） =====
const WAVE_OPEN_STORAGE_KEY = 'surf.wave-open'

const today = new Date().toISOString().slice(0, 10)
const waveLevels = ref<WaveLevel[]>([])
const openedLevels = ref<string[]>([])
const coachName = ref('')
const saving = ref(false)
const savedAt = ref('')

interface WaveOpenRecord {
  date: string
  levelCodes: string[]
  coachName: string
  savedAt: string
}

const fetchWaveLevels = async () => {
  try {
    const res = await waveLevelApi.getAll()
    waveLevels.value = [...res.data].sort((a, b) => a.sortOrder - b.sortOrder)
  } catch (error) {
    console.error('获取浪高档位失败:', error)
  }
}

// 只读回当晚那条记录；隔天打开不会带出昨晚的勾选与教练
const loadWaveOpenRecord = () => {
  const raw = localStorage.getItem(WAVE_OPEN_STORAGE_KEY)
  if (!raw) return
  try {
    const record = JSON.parse(raw) as WaveOpenRecord
    if (record.date !== today) return
    openedLevels.value = Array.isArray(record.levelCodes) ? record.levelCodes : []
    coachName.value = record.coachName || ''
    savedAt.value = record.savedAt || ''
  } catch {
    // 本地记录损坏时忽略，按未记录处理
  }
}

const saveWaveOpen = () => {
  if (openedLevels.value.length === 0) {
    ElMessage.warning('请先勾选今晚开浪的档位')
    return
  }
  const name = coachName.value.trim()
  if (!name) {
    // 教练名空着不能保存
    ElMessage.warning('请填写当班教练姓名后再保存')
    return
  }
  saving.value = true
  const now = new Date()
  const time = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
  const record: WaveOpenRecord = {
    date: today,
    levelCodes: openedLevels.value,
    coachName: name,
    savedAt: time
  }
  localStorage.setItem(WAVE_OPEN_STORAGE_KEY, JSON.stringify(record))
  coachName.value = name
  savedAt.value = time
  saving.value = false
  const levelNames = waveLevels.value
    .filter((l) => openedLevels.value.includes(l.levelCode))
    .map((l) => l.levelName)
    .join('、')
  ElMessage.success(`已记录今晚开浪档位（${levelNames}），当班教练：${name}`)
}

const fetchOverview = async () => {
  try {
    const res = await statisticsApi.getOverview()
    overview.value = res.data
  } catch (error) {
    console.error('获取概览数据失败:', error)
  }
}

const fetchAdjustRecords = async () => {
  try {
    const res = await bindingApi.getAdjustRecords()
    adjustRecords.value = res.data.slice(0, 10)
  } catch (error) {
    console.error('获取调整记录失败:', error)
  }
}

const initChart = async () => {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value)
  
  try {
    const res = await statisticsApi.getWaveLevelStatistics()
    const statistics = res.data
    
    const option: echarts.EChartsOption = {
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)'
      },
      legend: {
        bottom: 0,
        data: statistics.map(s => s.waveLevelName)
      },
      series: [
        {
          name: '设备分布',
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['50%', '45%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: true,
            formatter: '{b}\n{c}台'
          },
          data: statistics.map((s, index) => ({
            value: s.equipmentCount,
            name: s.waveLevelName,
            itemStyle: {
              color: ['#409eff', '#67c23a', '#f56c6c'][index]
            }
          }))
        }
      ]
    }
    
    chartInstance.setOption(option)
  } catch (error) {
    console.error('初始化图表失败:', error)
  }
}

onMounted(() => {
  fetchOverview()
  fetchAdjustRecords()
  fetchWaveLevels()
  loadWaveOpenRecord()
  nextTick(() => {
    initChart()
  })
  
  window.addEventListener('resize', () => {
    chartInstance?.resize()
  })
})
</script>

<style lang="scss">
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-card {
  display: flex;
  gap: 20px;
  
  .stat-item {
    flex: 1;
    background: #fff;
    border-radius: 12px;
    padding: 24px;
    display: flex;
    align-items: center;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    
    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 16px;
      font-size: 28px;
      color: #fff;
    }
    
    .equipment-icon {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }
    
    .level-icon {
      background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    }
    
    .binding-icon {
      background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
    }
    
    .stat-info {
      .stat-value {
        font-size: 32px;
        font-weight: bold;
        color: #303133;
      }
      
      .stat-label {
        font-size: 14px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }
}

.chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

  .card-header {
    margin-bottom: 20px;

    .card-title {
      font-size: 16px;
      font-weight: bold;
      color: #303133;
    }
  }

  .chart-container {
    height: 300px;
  }
}

.wave-open-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    .card-title {
      font-size: 16px;
      font-weight: bold;
      color: #303133;
    }
  }

  .wave-open-body {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .wave-open-row {
    display: flex;
    align-items: center;

    .row-label {
      width: 70px;
      flex-shrink: 0;
      font-size: 14px;
      color: #606266;
    }

    .level-checkbox-group {
      display: flex;
      gap: 8px;
    }

    .coach-input {
      width: 220px;
      margin-right: 12px;
    }
  }
}

.recent-records {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  
  .card-header {
    margin-bottom: 20px;
    
    .card-title {
      font-size: 16px;
      font-weight: bold;
      color: #303133;
    }
  }
}
</style>
