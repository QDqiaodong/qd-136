<template>
  <div class="wave-level-page">
    <div class="page-header">
      <div class="page-tip">
        维护浪高档位：代号保存后不可改，列表按显示顺序排列。
      </div>
      <el-button type="success" @click="openAddDialog">新增档位</el-button>
    </div>

    <el-table :data="waveLevels" border>
      <el-table-column prop="sortOrder" label="显示顺序" width="100" align="center" />
      <el-table-column prop="levelCode" label="档位代号" width="140" />
      <el-table-column prop="levelName" label="名称" width="140">
        <template #default="scope">
          <el-tag type="primary">{{ scope.row.levelName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="说明" show-overflow-tooltip />
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button size="small" @click="openEditDialog(scope.row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      :title="isEdit ? '编辑档位' : '新增档位'"
      v-model="dialogVisible"
      width="500px"
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="档位代号" prop="levelCode">
          <el-input
            v-model="formData.levelCode"
            :disabled="isEdit"
            maxlength="20"
            placeholder="如 LOW、MEDIUM，保存后不可改"
          />
        </el-form-item>
        <el-form-item label="名称" prop="levelName">
          <el-input v-model="formData.levelName" maxlength="50" placeholder="如 低浪" />
        </el-form-item>
        <el-form-item label="显示顺序" prop="sortOrder">
          <el-input-number
            v-model="formData.sortOrder"
            :min="0"
            :precision="0"
            controls-position="right"
            class="sort-input"
          />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-textarea
            v-model="formData.description"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="这个档位适合什么浪况、什么人用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveWaveLevel">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick } from 'vue'
import { waveLevelApi, type WaveLevel } from '@/api'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

const waveLevels = ref<WaveLevel[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive({
  levelCode: '',
  levelName: '',
  sortOrder: null as number | null,
  description: ''
})

// 行内红字：缺哪项，哪一项下面立刻标出来
const rules: FormRules = {
  levelCode: [{ required: true, message: '请填写档位代号', trigger: 'blur' }],
  levelName: [{ required: true, message: '请填写名称', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '请填写显示顺序', trigger: ['blur', 'change'] }],
  description: [{ required: true, message: '请填写说明', trigger: 'blur' }]
}

let editId: number | null = null

/** 列表固定按显示顺序升序（后端已排好，这里再兜底一次） */
const sortByOrder = (list: WaveLevel[]) =>
  [...list].sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id)

const fetchWaveLevels = async () => {
  try {
    const res = await waveLevelApi.getAll()
    waveLevels.value = sortByOrder(res.data)
  } catch (error) {
    console.error('获取浪高档位失败:', error)
    ElMessage.error('获取浪高档位失败')
  }
}

const resetAndOpen = (data: typeof formData, edit: boolean, id: number | null) => {
  isEdit.value = edit
  editId = id
  Object.assign(formData, data)
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const openAddDialog = () => {
  resetAndOpen({ levelCode: '', levelName: '', sortOrder: null, description: '' }, false, null)
}

const openEditDialog = (waveLevel: WaveLevel) => {
  resetAndOpen(
    {
      levelCode: waveLevel.levelCode,
      levelName: waveLevel.levelName,
      sortOrder: waveLevel.sortOrder,
      description: waveLevel.description
    },
    true,
    waveLevel.id
  )
}

const saveWaveLevel = async () => {
  // 缺哪项点哪项：先汇总缺失项弹消息，再触发表单校验让行内红字亮出来
  const missing: string[] = []
  if (!isEdit.value && !formData.levelCode.trim()) missing.push('档位代号')
  if (!formData.levelName.trim()) missing.push('名称')
  if (formData.sortOrder === null || formData.sortOrder === undefined) missing.push('显示顺序')
  if (!formData.description.trim()) missing.push('说明')
  if (missing.length > 0) {
    formRef.value?.validate().catch(() => {})
    ElMessage.error(`缺少必填项：${missing.join('、')}，请补全后再保存`)
    return
  }

  // 代号查重（不区分大小写）：写明代号被哪个档位占了；后端还会再拦一次
  if (!isEdit.value) {
    const code = formData.levelCode.trim()
    const occupant = waveLevels.value.find(
      (wl) => wl.levelCode.toLowerCase() === code.toLowerCase()
    )
    if (occupant) {
      ElMessage.error(`档位代号 ${code} 已被档位「${occupant.levelName}」占用，请更换代号`)
      return
    }
  }

  const payload = {
    levelCode: formData.levelCode.trim(),
    levelName: formData.levelName.trim(),
    sortOrder: formData.sortOrder,
    description: formData.description.trim()
  }
  try {
    if (isEdit.value && editId !== null) {
      await waveLevelApi.update(editId, payload)
      ElMessage.success('档位更新成功')
    } else {
      await waveLevelApi.create(payload)
      ElMessage.success('档位添加成功')
    }
    dialogVisible.value = false
    fetchWaveLevels()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  }
}

fetchWaveLevels()
</script>

<style lang="scss">
.wave-level-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .page-tip {
      color: #909399;
      font-size: 13px;
    }
  }

  .sort-input {
    width: 160px;
  }
}
</style>
