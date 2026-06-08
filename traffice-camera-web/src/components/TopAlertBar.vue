<template>
  <Transition name="slide">
    <div v-if="alertList.length > 0" class="alert-bar-wrapper">
      <div class="alert-bar-scroll">
        <div class="alert-bar-track">
          <div
            v-for="item in alertList"
            :key="item.id"
            class="alert-chip"
          >
            <span class="alert-icon">&#9888;</span>
            <span class="alert-device">{{ item.name }}</span>
            <span class="alert-info">当前 <strong>{{ item.currentCount }}</strong> / 阈值 <strong>{{ item.threshold }}</strong></span>
            <span class="alert-dot"></span>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { computed } from 'vue'
import { alertState } from '@/composables/alertState'

const alertList = computed(() => {
  const list = []
  alertState.alerts.forEach((val, key) => {
    list.push({ id: key, ...val })
  })
  return list
})
</script>

<style scoped>
.alert-bar-wrapper {
  height: 38px;
  background: linear-gradient(90deg, #2d1a1a 0%, #3d1a1a 50%, #2d1a1a 100%);
  border-bottom: 1px solid #ff3d71;
  overflow: hidden;
  flex-shrink: 0;
}

.alert-bar-scroll {
  height: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
}
.alert-bar-scroll::-webkit-scrollbar {
  height: 3px;
}
.alert-bar-scroll::-webkit-scrollbar-thumb {
  background: rgba(255, 61, 113, 0.3);
  border-radius: 2px;
}

.alert-bar-track {
  display: inline-flex;
  align-items: center;
  height: 100%;
  gap: 8px;
  padding: 0 14px;
}

.alert-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 26px;
  padding: 0 14px;
  border-radius: 13px;
  background: rgba(255, 61, 113, 0.12);
  border: 1px solid rgba(255, 61, 113, 0.35);
  color: #ffa1b5;
  font-size: 12px;
  flex-shrink: 0;
}

.alert-icon {
  font-size: 14px;
  color: #ff3d71;
  line-height: 1;
}

.alert-device {
  color: #ff3d71;
  font-weight: 600;
}

.alert-info {
  color: #e0a0b0;
}
.alert-info strong {
  color: #ff6b8a;
}

.alert-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ff3d71;
  animation: pulse-dot 1s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.3; transform: scale(1.5); }
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
}
.slide-enter-from,
.slide-leave-to {
  height: 0;
  opacity: 0;
}
</style>
