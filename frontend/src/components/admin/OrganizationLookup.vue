<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { adminRequest } from '../../shared/api/admin'
import type { Organization, OrganizationType, Page } from '../../shared/api/admin'
const props = defineProps<{ context: string; type: OrganizationType; modelValue: string; label: string; excluded?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const search = ref('')
const options = ref<Organization[]>([])
const loading = ref(false)
const error = ref('')
const hasNext = ref(false)
const page = ref(0)
let controller: AbortController | undefined
let requestNumber = 0
const visible = computed(() => options.value.filter(o => o.type === props.type && o.status === 'ACTIVE' && o.id !== props.excluded))
async function load(reset = true) {
  const request = ++requestNumber
  controller?.abort()
  controller = new AbortController()
  if (reset) { page.value = 0; options.value = [] }
  loading.value = true
  error.value = ''
  try {
    const result = await adminRequest<Page<Organization>>(props.context, '/organizations?search=' + encodeURIComponent(search.value) + '&page=' + page.value, 'GET', undefined, controller.signal)
    if (request !== requestNumber) return
    options.value = reset ? result.items : [...options.value, ...result.items]
    hasNext.value = result.hasNext
  } catch (cause) {
    if (request !== requestNumber || controller.signal.aborted) return
    error.value = cause instanceof Error ? cause.message : 'Pretraga nije uspjela.'
  } finally { if (request === requestNumber) loading.value = false }
}
function more() { page.value++; void load(false) }
watch(() => [props.context, props.type], () => { search.value = ''; emit('update:modelValue', ''); void load() }, { immediate: true })
onBeforeUnmount(() => { requestNumber++; controller?.abort() })
</script>
<template>
  <div class="lookup">
    <label>{{ label }}
      <select :value="modelValue" required @change="emit('update:modelValue', ($event.target as HTMLSelectElement).value)">
        <option value="">Odaberite organizaciju</option>
        <option v-for="item in visible" :key="item.id" :value="item.id">{{ item.name }}</option>
      </select>
    </label>
    <div class="search-row">
      <input v-model="search" aria-label="Naziv organizacije za pretragu" placeholder="Pretraži po nazivu" maxlength="240" @keydown.enter.prevent="load()">
      <button type="button" class="secondary compact" :disabled="loading" @click="load()">Pretraži</button>
    </div>
    <p v-if="error" class="error-text" role="alert">{{ error }}</p>
    <span v-if="loading" role="status">Učitavanje…</span>
    <button v-else-if="hasNext" type="button" class="text-button" @click="more">Učitaj još organizacija</button>
  </div>
</template>
