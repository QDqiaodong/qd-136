import axios from 'axios'

const instance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

instance.interceptors.response.use(
  (response) => {
    if (response.data.code === 200) {
      return response.data
    }
    return Promise.reject(response.data)
  },
  (error) => {
    return Promise.reject(error)
  }
)

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface Equipment {
  id: number
  equipmentCode: string
  equipmentName: string
  equipmentType: string
  bufferThickness: number | null
  specification: string
  location: string
  status: string
  remark: string
  createdAt: string
  updatedAt: string
}

export interface WaveLevel {
  id: number
  levelCode: string
  levelName: string
  description: string
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface EquipmentWaveLevel {
  id: number
  equipmentId: number
  waveLevelId: number
  waveLevelCode: string
  waveLevelName: string
  bindingType: string
  effectiveDate: string
  expireDate: string | null
  createdAt: string
}

export interface EquipmentAdjustRecord {
  id: number
  equipmentId: number
  equipmentCode: string
  equipmentName: string
  previousWaveLevelCode: string | null
  previousWaveLevelName: string | null
  newWaveLevelCode: string
  newWaveLevelName: string
  adjustReason: string
  operator: string
  adjustTime: string
  remark: string
}

export interface EquipmentInfoDTO {
  id: number
  equipmentCode: string
  equipmentName: string
  equipmentType: string
  bufferThickness: number | null
  specification: string
  location: string
  status: string
  waveLevelCode: string
  waveLevelName: string
  bindingTime: string
}

export interface WaveLevelStatisticsDTO {
  waveLevelCode: string
  waveLevelName: string
  description: string
  equipmentCount: number
  equipmentList: EquipmentInfoDTO[]
}

export const equipmentApi = {
  getAll: () => instance.get<Equipment[]>('/equipment'),
  getById: (id: number) => instance.get<Equipment>(`/equipment/${id}`),
  create: (data: Omit<Equipment, 'id' | 'status' | 'createdAt' | 'updatedAt'>) =>
    instance.post<Equipment>('/equipment', data),
  update: (id: number, data: Partial<Equipment>) =>
    instance.put<Equipment>(`/equipment/${id}`, data),
  delete: (id: number) => instance.delete(`/equipment/${id}`),
  getByType: (type: string) => instance.get<Equipment[]>(`/equipment/type/${type}`)
}

export const waveLevelApi = {
  getAll: () => instance.get<WaveLevel[]>('/wave-level'),
  getById: (id: number) => instance.get<WaveLevel>(`/wave-level/${id}`),
  create: (data: Omit<WaveLevel, 'id' | 'createdAt' | 'updatedAt'>) =>
    instance.post<WaveLevel>('/wave-level', data),
  update: (id: number, data: Partial<WaveLevel>) =>
    instance.put<WaveLevel>(`/wave-level/${id}`, data),
  delete: (id: number) => instance.delete(`/wave-level/${id}`)
}

export const bindingApi = {
  bind: (data: { equipmentId: number; waveLevelCode: string; bindingType: string }) =>
    instance.post<EquipmentWaveLevel>('/binding/bind', data),
  adjust: (data: { equipmentId: number; newWaveLevelCode: string; adjustReason: string; operator: string; remark?: string }) =>
    instance.post<EquipmentWaveLevel>('/binding/adjust', data),
  getCurrentBinding: (equipmentId: number) =>
    instance.get<EquipmentWaveLevel>(`/binding/equipment/${equipmentId}`),
  getByWaveLevel: (waveLevelCode: string) =>
    instance.get<EquipmentWaveLevel[]>(`/binding/wave-level/${waveLevelCode}`),
  getBindingHistory: (equipmentId: number) =>
    instance.get<EquipmentWaveLevel[]>(`/binding/history/${equipmentId}`),
  getAdjustRecords: (equipmentId?: number) =>
    instance.get<EquipmentAdjustRecord[]>('/binding/adjust-records', { params: { equipmentId } })
}

export const statisticsApi = {
  getWaveLevelStatistics: (waveLevelCode?: string) =>
    instance.get<WaveLevelStatisticsDTO[]>('/statistics/wave-level', { params: { waveLevelCode } }),
  getOverview: () => instance.get<{ totalEquipment: number; totalWaveLevel: number; totalBinding: number }>('/statistics/overview')
}
