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
import { statisticsApi, bindingApi, type EquipmentAdjustRecord } from '@/api'

const overview = ref({ totalEquipment: 0, totalWaveLevel: 0, totalBinding: 0 })
const adjustRecords = ref<EquipmentAdjustRecord[]>([])
const chartRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

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
