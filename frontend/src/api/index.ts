import axios from 'axios'

const ROLE_STORAGE_KEY = 'surf.current-role'

const instance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 每个请求都带上当前角色头，后端据此做最终鉴权与范围收窄
instance.interceptors.request.use((config) => {
  const role = localStorage.getItem(ROLE_STORAGE_KEY) || 'DIRECTOR'
  config.headers = config.headers || {}
  config.headers['X-Role'] = role
  return config
})

instance.interceptors.response.use(
  (response) => {
    if (response.data.code === 200) {
      return response.data
    }
    return Promise.reject(response.data)
  },
  (error) => {
    // HTTP 错误（如 403）时，把后端 ApiResponse 的 message 透传给调用方
    const body = error.response?.data
    if (body && typeof body === 'object' && 'message' in body) {
      return Promise.reject(body)
    }
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
  /** 是否仍在借（有未归还领用记录），由后端按领用台账回填 */
  borrowed?: boolean
  /** 当前借用人（当班教练），未在借为 null */
  borrowedBy?: string | null
  /** 领用时间，未在借为 null */
  borrowedAt?: string | null
  /** 当前在借领用记录 id，供发起归还 */
  activeLoanId?: number | null
}

/** 领用台账一条记录：returnedAt 为 null 表示仍在借 */
export interface EquipmentLoan {
  id: number
  equipmentId: number
  equipmentCode: string
  equipmentName: string
  borrowedBy: string
  borrowedAt: string
  returnedAt: string | null
  returnedBy: string | null
  remark: string | null
  createdAt: string
}

export interface WaveLevel {
  id: number
  levelCode: string
  levelName: string
  description: string
  /** 缓冲挡垫绑定到本档位所需的最小缓冲厚度，null 表示不限制 */
  minBufferThickness: number | null
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

export interface BindingStatus {
  equipmentId: number
  equipmentCode: string
  equipmentName: string
  equipmentType: string
  bufferThickness: number | null
  location: string
  bindingId: number
  waveLevelCode: string
  waveLevelName: string
  bindingType: string
  effectiveDate: string
}

export interface AuthMe {
  role: 'DIRECTOR' | 'COACH'
  roleName: string
  authorizedWaveLevelCodes: string[]
  auxiliaryEquipmentTypes: string[]
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

// ===== 设备领用 / 归还 =====
// 在借状态以后端领用台账为唯一依据：未还清时 borrow 返回 400 拦截，归还后才可再借

export const equipmentLoanApi = {
  /** 登记领用（borrowerName 为当班教练姓名）；未还清时后端拒绝 */
  borrow: (data: { equipmentId: number; borrowerName: string; remark?: string }) =>
    instance.post<EquipmentLoan>('/equipment-loan/borrow', data),
  /** 登记归还，设备随后可再次领用 */
  returnEquipment: (equipmentId: number, operatorName?: string) =>
    instance.post<EquipmentLoan>(`/equipment-loan/return/${equipmentId}`, { operatorName }),
  /** 当前全部在借记录 */
  getActiveLoans: () => instance.get<EquipmentLoan[]>('/equipment-loan/active'),
  /** 某台设备的领用 / 归还流水 */
  getHistory: (equipmentId: number) =>
    instance.get<EquipmentLoan[]>(`/equipment-loan/history/${equipmentId}`)
}

export interface WaveLevelSavePayload {
  /** 档位代号，仅新增时生效；编辑时代号不可改 */
  levelCode?: string
  levelName: string
  sortOrder: number | null
  description: string
}

export const waveLevelApi = {
  getAll: () => instance.get<WaveLevel[]>('/wave-level'),
  getById: (id: number) => instance.get<WaveLevel>(`/wave-level/${id}`),
  create: (data: WaveLevelSavePayload) =>
    instance.post<WaveLevel>('/wave-level', data),
  update: (id: number, data: WaveLevelSavePayload) =>
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
    instance.get<EquipmentAdjustRecord[]>('/binding/adjust-records', { params: { equipmentId } }),
  /** 服务端按角色收窄后的“绑定状态”列表：教练仅见授权档位上的防滑扶手/缓冲挡垫 */
  getActiveBindings: () =>
    instance.get<BindingStatus[]>('/binding/active-bindings'),
  /** 服务端按角色收窄后的可绑定设备下拉数据 */
  getBindableEquipments: () =>
    instance.get<Equipment[]>('/binding/bindable-equipments')
}

export const authApi = {
  /** 显式指定角色获取授权范围（角色来自本地持久化，通过 X-Role 头传后端） */
  getMe: (_role?: 'DIRECTOR' | 'COACH') =>
    instance.get<AuthMe>('/auth/me')
}

export const statisticsApi = {
  getWaveLevelStatistics: (waveLevelCode?: string) =>
    instance.get<WaveLevelStatisticsDTO[]>('/statistics/wave-level', { params: { waveLevelCode } }),
  getOverview: () => instance.get<{ totalEquipment: number; totalWaveLevel: number; totalBinding: number }>('/statistics/overview')
}

// ===== 开浪前点检盘点（馆长） =====

export interface PreWaveInspection {
  id: number
  inspectionDate: string
  equipmentId: number
  equipmentCode: string
  equipmentName: string
  equipmentType: string
  /** 握把打滑次数（次） */
  slipCount: number
  /** 挡垫移位（厘米） */
  shiftCm: number
  remark: string | null
  updatedAt: string
}

export interface PreWaveInspectionSummary {
  inspectionDate: string
  inspectedCount: number
  /** 今晚握把打滑次数合计 */
  totalSlipCount: number
  /** 今晚挡垫移位合计（厘米） */
  totalShiftCm: number
}

export interface PreWaveReconcileResult {
  sheetSlipCount: number
  onSiteSlipCount: number
  slipDiff: number
  slipMatched: boolean
  sheetShiftCm: number
  onSiteShiftCm: number
  shiftDiff: number
  shiftMatched: boolean
  balanced: boolean
}

export interface PreWaveInspectionSubmitPayload {
  equipmentId: number
  slipCount: number
  shiftCm: number
  remark?: string
}

export interface PreWaveInspectionUpsertResult {
  /** true 表示同一台设备今晚已点过，本次为复点覆盖（合计只算一次） */
  reInspected: boolean
}

export const inspectionApi = {
  /** 今晚点检明细 */
  listToday: () => instance.get<PreWaveInspection[]>('/inspection/today'),
  /** 今晚可点检的入浪辅助设备 */
  getInspectableEquipments: () => instance.get<Equipment[]>('/inspection/equipments'),
  /** 今晚打滑合计、移位合计 */
  getSummary: () => instance.get<PreWaveInspectionSummary>('/inspection/summary'),
  /** 录入 / 复点（同设备同晚只保留一条） */
  submit: (data: PreWaveInspectionSubmitPayload) =>
    instance.post<PreWaveInspectionUpsertResult>('/inspection', data),
  /** 撤回一条点检记录 */
  remove: (id: number) => instance.delete<void>(`/inspection/${id}`),
  /** 与现场计数对账 */
  reconcile: (data: { onSiteSlipCount: number; onSiteShiftCm: number }) =>
    instance.post<PreWaveReconcileResult>('/inspection/reconcile', data)
}
