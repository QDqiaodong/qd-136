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
      <el-table-column prop="equipmentType" label="设备类型" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.equipmentType === '防滑扶手' ? 'primary' : 'success'">
            {{ scope.row.equipmentType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="bufferThickness" label="缓冲厚度(cm)" width="130">
        <template #default="scope">
          {{ scope.row.bufferThickness || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="specification" label="规格" width="150" />
      <el-table-column prop="location" label="位置" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'danger'">
            {{ scope.row.status === 'ACTIVE' ? '正常' : '已删除' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { equipmentApi, type Equipment } from '@/api'
import { ElMessage } from 'element-plus'

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
}
</style>
