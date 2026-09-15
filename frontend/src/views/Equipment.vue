<template>
  <div class="equipment-page">
    <div class="page-header">
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索设备编号或名称"
          prefix-icon="el-icon-search"
          class="search-input"
          @keyup.enter="fetchEquipments"
        />
        <el-select
          v-model="searchType"
          placeholder="设备类型"
          class="type-select"
        >
          <el-option label="全部" value="" />
          <el-option label="防滑扶手" value="防滑扶手" />
          <el-option label="缓冲挡垫" value="缓冲挡垫" />
        </el-select>
        <el-button type="primary" @click="fetchEquipments">搜索</el-button>
      </div>
      <el-button type="success" @click="openAddDialog">添加设备</el-button>
    </div>

    <el-table :data="equipments" border>
      <el-table-column prop="equipmentCode" label="设备编号" width="120" />
      <el-table-column prop="equipmentName" label="设备名称" width="150" />
      <el-table-column prop="equipmentType" label="设备类型" width="110">
        <template #default="scope">
          <el-tag :type="scope.row.equipmentType === '防滑扶手' ? 'primary' : 'success'">
            {{ scope.row.equipmentType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="bufferThickness" label="缓冲厚度(cm)" width="120">
        <template #default="scope">
          {{ scope.row.bufferThickness || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="specification" label="规格" width="100" />
      <el-table-column prop="location" label="位置" width="120" />
      <el-table-column label="领用状态" width="230">
        <template #default="scope">
          <el-tag v-if="scope.row.borrowed" type="warning" effect="dark">
            在借 · {{ scope.row.borrowedBy }}
          </el-tag>
          <el-tag v-else type="info">空闲</el-tag>
          <div v-if="scope.row.borrowed && scope.row.borrowedAt" class="loan-time">
            领用时间：{{ formatTime(scope.row.borrowedAt) }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="在册状态" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'danger'">
            {{ scope.row.status === 'ACTIVE' ? '正常' : '已删除' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170">
        <template #default="scope">{{ formatTime(scope.row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="270" fixed="right">
        <template #default="scope">
          <el-button
            v-if="!scope.row.borrowed"
            size="small"
            type="primary"
            :disabled="scope.row.status !== 'ACTIVE'"
            @click="openBorrowDialog(scope.row)"
          >领用</el-button>
          <el-button
            v-else
            size="small"
            type="warning"
            @click="confirmReturn(scope.row)"
          >归还</el-button>
          <el-button size="small" @click="openEditDialog(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteEquipment(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      :title="isEdit ? '编辑设备' : '添加设备'"
      v-model="dialogVisible"
      width="500px"
    >
      <el-form :model="formData" label-width="100px">
        <el-form-item label="设备编号" :required="!isEdit">
          <el-input v-model="formData.equipmentCode" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="设备名称" required>
          <el-input v-model="formData.equipmentName" />
        </el-form-item>
        <el-form-item label="设备类型" required>
          <el-select v-model="formData.equipmentType">
            <el-option label="防滑扶手" value="防滑扶手" />
            <el-option label="缓冲挡垫" value="缓冲挡垫" />
          </el-select>
        </el-form-item>
        <el-form-item label="缓冲厚度(cm)">
          <el-input-number v-model="formData.bufferThickness" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="formData.specification" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="formData.location" />
        </el-form-item>
        <el-form-item label="备注">
          <el-textarea v-model="formData.remark" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEquipment">确定</el-button>
      </template>
    </el-dialog>

    <!-- 领用登记：当班教练姓名必填；未还清时后端拒绝，报错写明已借给哪位教练 -->
    <el-dialog title="设备领用" v-model="borrowDialogVisible" width="460px">
      <el-form :model="borrowForm" label-width="110px">
        <el-form-item label="设备">
          <span>{{ borrowForm.equipmentCode }} - {{ borrowForm.equipmentName }}</span>
        </el-form-item>
        <el-form-item label="当班教练" required>
          <el-input
            v-model="borrowForm.borrowerName"
            placeholder="请输入领用的当班教练姓名"
            maxlength="50"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-textarea v-model="borrowForm.remark" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="borrowDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="borrowSubmitting" @click="submitBorrow">确认领用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { equipmentApi, equipmentLoanApi, type Equipment } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const equipments = ref<Equipment[]>([])
const searchKeyword = ref('')
const searchType = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)

const formData = reactive({
  equipmentCode: '',
  equipmentName: '',
  equipmentType: '防滑扶手',
  bufferThickness: null as number | null,
  specification: '',
  location: '',
  remark: ''
})

let editId: number | null = null

const formatTime = (value: string | null | undefined) => {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

const fetchEquipments = async () => {
  try {
    let res
    if (searchType.value) {
      res = await equipmentApi.getByType(searchType.value)
    } else {
      res = await equipmentApi.getAll()
    }

    let data = res.data
    if (searchKeyword.value) {
      const keyword = searchKeyword.value.toLowerCase()
      data = data.filter(
        (e: Equipment) =>
          e.equipmentCode.toLowerCase().includes(keyword) ||
          e.equipmentName.toLowerCase().includes(keyword)
      )
    }
    // 在借标记完全来自后端台账：关页再开后仍在借的标在借，已还清的不标
    equipments.value = data
  } catch (error) {
    console.error('获取设备列表失败:', error)
    ElMessage.error('获取设备列表失败')
  }
}

const openAddDialog = () => {
  isEdit.value = false
  editId = null
  Object.assign(formData, {
    equipmentCode: '',
    equipmentName: '',
    equipmentType: '防滑扶手',
    bufferThickness: null,
    specification: '',
    location: '',
    remark: ''
  })
  dialogVisible.value = true
}

const openEditDialog = (equipment: Equipment) => {
  isEdit.value = true
  editId = equipment.id
  Object.assign(formData, {
    equipmentCode: equipment.equipmentCode,
    equipmentName: equipment.equipmentName,
    equipmentType: equipment.equipmentType,
    bufferThickness: equipment.bufferThickness,
    specification: equipment.specification,
    location: equipment.location,
    remark: equipment.remark
  })
  dialogVisible.value = true
}

const saveEquipment = async () => {
  try {
    if (isEdit.value && editId) {
      await equipmentApi.update(editId, formData)
      ElMessage.success('设备更新成功')
    } else {
      await equipmentApi.create(formData)
      ElMessage.success('设备添加成功')
    }
    dialogVisible.value = false
    fetchEquipments()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  }
}

const deleteEquipment = async (id: number) => {
  try {
    await equipmentApi.delete(id)
    ElMessage.success('设备删除成功')
    fetchEquipments()
  } catch (error: any) {
    ElMessage.error(error.message || '删除失败')
  }
}

// ===== 领用 / 归还 =====

const borrowDialogVisible = ref(false)
const borrowSubmitting = ref(false)
const borrowForm = reactive({
  equipmentId: 0,
  equipmentCode: '',
  equipmentName: '',
  borrowerName: '',
  remark: ''
})

const openBorrowDialog = (equipment: Equipment) => {
  // 打开前以台账口径再确认一次，避免拿着过期名单重复领用
  if (equipment.borrowed) {
    ElMessage.warning(`该设备已借给 ${equipment.borrowedBy}，尚未归还，不能重复领用`)
    return
  }
  Object.assign(borrowForm, {
    equipmentId: equipment.id,
    equipmentCode: equipment.equipmentCode,
    equipmentName: equipment.equipmentName,
    borrowerName: '',
    remark: ''
  })
  borrowDialogVisible.value = true
}

const submitBorrow = async () => {
  if (!borrowForm.borrowerName.trim()) {
    ElMessage.warning('请填写当班教练姓名')
    return
  }
  borrowSubmitting.value = true
  try {
    await equipmentLoanApi.borrow({
      equipmentId: borrowForm.equipmentId,
      borrowerName: borrowForm.borrowerName.trim(),
      remark: borrowForm.remark.trim() || undefined
    })
    ElMessage.success(`领用成功，已登记借给 ${borrowForm.borrowerName.trim()}`)
    borrowDialogVisible.value = false
    fetchEquipments()
  } catch (error: any) {
    // 未还清等拦截以后端为准，错误信息里写明已借给哪位教练
    ElMessage.error(error.message || '领用失败')
  } finally {
    borrowSubmitting.value = false
  }
}

const confirmReturn = async (equipment: Equipment) => {
  try {
    await ElMessageBox.confirm(
      `确认 ${equipment.equipmentCode}（${equipment.equipmentName}）已由 ${equipment.borrowedBy} 归还？归还后设备才可再次领用。`,
      '设备归还',
      { confirmButtonText: '确认归还', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await equipmentLoanApi.returnEquipment(equipment.id, equipment.borrowedBy || undefined)
    ElMessage.success('归还成功，设备已回到空闲')
    fetchEquipments()
  } catch (error: any) {
    ElMessage.error(error.message || '归还失败')
  }
}

fetchEquipments()
</script>

<style lang="scss">
.equipment-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .search-bar {
      display: flex;
      gap: 12px;

      .search-input {
        width: 300px;
      }

      .type-select {
        width: 150px;
      }
    }
  }

  .loan-time {
    margin-top: 4px;
    font-size: 12px;
    color: #909399;
    line-height: 1.4;
  }
}
</style>
