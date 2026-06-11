<template>
  <div class="page-container">
    <ManagerRepair v-if="isManagePage" />
    <StudentRepair v-else />
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import StudentRepair from './StudentRepair.vue'
import ManagerRepair from './ManagerRepair.vue'

const route = useRoute()
const isManagePage = ref(false)

const checkPath = () => {
  isManagePage.value = route.path.includes('/manage')
}

onMounted(() => {
  checkPath()
})

watch(() => route.path, () => {
  checkPath()
})
</script>

<style scoped>
.page-container {
  width: 100%;
}
</style>
