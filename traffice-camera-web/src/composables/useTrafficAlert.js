import { onMounted, onUnmounted } from 'vue'
import { listCamera } from '@/api/traffic/camera'
import { alertState } from './alertState'

export function useTrafficAlert() {
  let pollTimer = null

  const fetchCounts = async () => {
    try {
      const res = await fetch('/ai/traffic_counts')
      if (!res.ok) return null
      return await res.json()
    } catch {
      return null
    }
  }

  const fetchCameras = async () => {
    try {
      const res = await listCamera({ pageNum: 1, pageSize: 500 })
      return res.data.rows || []
    } catch {
      return []
    }
  }

  const checkAlert = async () => {
    if (!localStorage.getItem('accessToken')) return
    const [counts, cameras] = await Promise.all([fetchCounts(), fetchCameras()])
    if (!counts || !cameras.length) return

    const thresholdMap = {}
    cameras.forEach(cam => {
      const t = cam.threshold
      if (t != null && t > 0) {
        thresholdMap[cam.cameraId] = { threshold: t, name: cam.cameraName }
      }
    })

    counts.forEach(item => {
      const camId = item.camera_id
      const cfg = thresholdMap[camId]
      if (!cfg) return

      const currentCount = item.curr_min_count
      if (currentCount > cfg.threshold) {
        alertState.alerts.set(camId, {
          name: cfg.name,
          currentCount,
          threshold: cfg.threshold
        })
      } else {
        alertState.alerts.delete(camId)
      }
    })
  }

  const start = (intervalMs = 8000) => {
    stop()
    checkAlert()
    pollTimer = setInterval(checkAlert, intervalMs)
  }

  const stop = () => {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  onMounted(() => start())
  onUnmounted(() => stop())

  return { start, stop }
}
