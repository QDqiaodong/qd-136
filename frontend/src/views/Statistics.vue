<template>
  <div class="statistics-page">
    <div class="filter-bar">
      <el-select v-model="filterWaveLevel" placeholder="筛选档位">
        <el-option label="全部档位" value="" />
        <el-option
          v-for="wl in waveLevels"
          :key="wl.levelCode"
          :label="wl.levelName"
          :value="wl.levelCode"
        />
      </el-select>
      <el-button type="primary" @click="fetchStatistics">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <div class="statistics-cards">
      <div
        v-for="stat in statistics"
        :key="stat.waveLevelCode"
        class="stat-card"
        :class="getLevelCardClass(stat.waveLevelCode)"
      >
        <div class="card-header">
          <div class="level-info">
            <span class="level-name">{{ stat.waveLevelName }}</span>
            <span class="level-code">{{ stat.waveLevelCode }}</span>
          </div>
          <div class="equipment-count">
            <span class="count-value">{{ stat.equipmentCount }}</span>
            <span class="count-label">台设备</span>
          </div>
        </div>
        <div class="card-description">{{ stat.description }}</div>
        <div class="card-body">
          <el-table :data="stat.equipmentList" border>
            <el-table-column prop="equipmentCode" label="设备编号" width="120" />
            <el-table-column prop="equipmentName" label="设备名称" width="150" />
            <el-table-column prop="equipmentType" label="类型" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.equipmentType === '防滑扶手' ? 'primary' : 'success'" size="small">
                  {{ scope.row.equipmentType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="bufferThickness" label="厚度(cm)" width="100">
              <template #default="scope">
                {{ scope.row.bufferThickness || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="location" label="位置" width="120" />
            <el-table-column prop="bindingTime" label="绑定时间" width="180" />
          </el-table>
          <div v-if="stat.equipmentList.length === 0" class="empty-tip">
            <i class="el-icon-info"></i>
            <span>暂无配套设备</span>
          </div>
        </div>
      </div>
    </div>

    <div class="chart-section">
      <div class="card-header">
        <span class="card-title">浪高档位设备数量对比</span>
      </div>
      <div ref="barChartRef" class="chart-container"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { statisticsApi, waveLevelApi, type WaveLevelStatisticsDTO, type WaveLevel } from '@/api'

const filterWaveLevel = ref('')
const waveLevels = ref<WaveLevel[]>([])
const statistics = ref<WaveLevelStatisticsDTO[]>([])
const barChartRef = ref<HTMLElement | null>(null)
let barChartInstance: echarts.ECharts | null = null

const fetchWaveLevels = async () => {
  try {
    const res = await waveLevelApi.getAll()
    waveLevels.value = res.data
  } catch (error) {
    console.error('获取浪高档位失败:', error)
  }
}

const fetchStatistics = async () => {
  try {
    const res = await statisticsApi.getWaveLevelStatistics(filterWaveLevel.value || undefined)
    statistics.value = res.data
    updateBarChart()
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const resetFilter = () => {
  filterWaveLevel.value = ''
  fetchStatistics()
}

const getLevelCardClass = (code: string) => {
  const classes: Record<string, string> = {
    LOW: 'level-low',
    MEDIUM: 'level-medium',
    HIGH: 'level-high'
  }
  return classes[code] || ''
}

const updateBarChart = () => {
  if (!barChartInstance) return
  
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: statistics.value.map(s => s.waveLevelName),
      axisLabel: {
        fontSize: 14
      }
    },
    yAxis: {
      type: 'value',
      name: '设备数量(台)',
      nameTextStyle: {
        fontSize: 14
      }
    },
    series: [
      {
        name: '设备数量',
        type: 'bar',
        barWidth: '50%',
        data: statistics.value.map((s, index) => ({
          value: s.equipmentCount,
          itemStyle: {
            color: ['#67c23a', '#e6a23c', '#f56c6c'][index],
            borderRadius: [8, 8, 0, 0]
          }
        })),
        label: {
          show: true,
          position: 'top',
          fontSize: 14,
          fontWeight: 'bold'
        }
      }
    ]
  }
  
  barChartInstance.setOption(option)
}

const initBarChart = () => {
  if (!barChartRef.value) return
  barChartInstance = echarts.init(barChartRef.value)
}

onMounted(() => {
  fetchWaveLevels()
  fetchStatistics()
  nextTick(() => {
    initBarChart()
  })
  
  window.addEventListener('resize', () => {
    barChartInstance?.resize()
  })
})
</script>

<style lang="scss">
.statistics-page {
  .filter-bar {
    display: flex;
    gap: 12px;
    margin-bottom: 20px;
    
    :deep(.el-select) {
      width: 200px;
    }
  }
  
  .statistics-cards {
    display: flex;
    flex-direction: column;
    gap: 20px;
    margin-bottom: 20px;
    
    .stat-card {
      background: #fff;
      border-radius: 12px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
      overflow: hidden;
      
      &.level-low {
        .card-header {
          background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
        }
      }
      
      &.level-medium {
        .card-header {
          background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
        }
      }
      
      &.level-high {
        .card-header {
          background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
        }
      }
      
      .card-header {
        padding: 20px 24px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        color: #fff;
        
        .level-info {
          display: flex;
          flex-direction: column;
          
          .level-name {
            font-size: 20px;
            font-weight: bold;
          }
          
          .level-code {
            font-size: 12px;
            opacity: 0.8;
            margin-top: 4px;
          }
        }
        
        .equipment-count {
          text-align: right;
          
          .count-value {
            font-size: 48px;
            font-weight: bold;
            line-height: 1;
          }
          
          .count-label {
            font-size: 14px;
            opacity: 0.8;
          }
        }
      }
      
      .card-description {
        padding: 12px 24px;
        background: #f8f9fa;
        font-size: 14px;
        color: #606266;
      }
      
      .card-body {
        padding: 20px 24px;
        
        .empty-tip {
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 40px;
          color: #909399;
          
          i {
            margin-right: 8px;
          }
        }
      }
    }
  }
  
  .chart-section {
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
}
</style>
