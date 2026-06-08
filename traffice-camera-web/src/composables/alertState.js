import { reactive } from 'vue'

// 全局共享预警状态，供 useTrafficAlert 和 videoWall 共用
export const alertState = reactive({
  // Map<cameraId, { name, currentCount, threshold }>
  alerts: new Map()
})
