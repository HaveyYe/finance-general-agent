import { defineStore } from 'pinia'
import { ref } from 'vue'

const STORAGE_KEY = 'finance-agent-settings-v1'

export const useSettingsStore = defineStore('settings', () => {
  const autoApprove = ref(false)
  const knowledgeStatus = ref<'online' | 'offline' | 'unknown'>('unknown')

  function restore() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (raw) {
        const data = JSON.parse(raw) as { autoApprove?: boolean }
        autoApprove.value = data.autoApprove ?? false
      }
    } catch { /* ignore */ }
  }

  function persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ autoApprove: autoApprove.value }))
  }

  function toggleAutoApprove(value: boolean) {
    autoApprove.value = value
    persist()
  }

  async function checkKnowledgeStatus() {
    try {
      const response = await fetch('/knowledge/documents', {
        signal: AbortSignal.timeout(5000),
      })
      knowledgeStatus.value = response.ok ? 'online' : 'offline'
    } catch {
      knowledgeStatus.value = 'offline'
    }
  }

  restore()

  return { autoApprove, knowledgeStatus, toggleAutoApprove, checkKnowledgeStatus }
})
