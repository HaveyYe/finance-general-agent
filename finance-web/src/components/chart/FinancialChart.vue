<template>
  <div ref="chartRef" class="financial-chart" />
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

const props = defineProps<{
  option: EChartsOption
}>()

const emit = defineEmits<{
  chartClick: [name: string]
  chartDblClick: [name: string]
}>()

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | undefined
let resizeObserver: ResizeObserver | undefined

function render() {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
    chart.on('click', (params) => emit('chartClick', String(params.name || '')))
    chart.on('dblclick', (params) => emit('chartDblClick', String(params.name || '')))
  }
  chart.setOption(props.option, true)
}

onMounted(() => {
  render()
  if (chartRef.value) {
    resizeObserver = new ResizeObserver(() => chart?.resize())
    resizeObserver.observe(chartRef.value)
  }
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
})
</script>

<style scoped>
.financial-chart {
  width: 100%;
  min-height: 280px;
}
</style>
