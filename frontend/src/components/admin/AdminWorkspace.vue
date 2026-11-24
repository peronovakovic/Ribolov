<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { loadAccessContexts, roleNames } from '../../shared/api/access'
import type { AccessContext } from '../../shared/api/access'
import AdminConsole from './AdminConsole.vue'
const contexts = ref<AccessContext[]>([])
const selectedId = ref('')
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const administrative = computed(() => contexts.value.filter(c => c.role === 'SYSTEM_ADMIN' || c.role === 'ASSOCIATION_ADMIN'))
const selected = computed(() => administrative.value.find(c => c.id === selectedId.value))
async function refresh() {
  if (busy.value) return
  loading.value = true
  error.value = ''
  try {
    contexts.value = await loadAccessContexts()
    if (!administrative.value.some(c => c.id === selectedId.value)) selectedId.value = administrative.value.length === 1 ? administrative.value[0]!.id : ''
  } catch (cause) {
    contexts.value = []
    selectedId.value = ''
    error.value = cause instanceof Error ? cause.message : 'Ovlaštenja nisu dostupna.'
  } finally { loading.value = false }
}
onMounted(refresh)
</script>
<template>
  <section class="workspace" aria-labelledby="workspace-heading">
    <div class="section-heading"><h2 id="workspace-heading">{{ administrative.length ? 'Administracija' : 'Vaša ovlaštenja' }}</h2>
      <button class="text-button" :disabled="loading || busy" @click="refresh">Osvježi ovlaštenja</button>
    </div>
    <p v-if="loading" role="status">Provjera ovlaštenja…</p>
    <p v-else-if="error" role="alert">{{ error }}</p>
    <template v-else-if="administrative.length">
      <label class="context-picker">Aktivna uloga
        <select v-model="selectedId" :disabled="busy"><option value="">Odaberite ulogu</option>
          <option v-for="context in administrative" :key="context.id" :value="context.id">{{ roleNames[context.role] }}{{ context.organization ? ' · ' + context.organization.name : '' }}</option>
        </select>
      </label>
      <AdminConsole v-if="selected" :key="selected.id" :context="selected" @busy="busy = $event" />
      <p v-else>Odaberite ulogu za rad s evidencijama.</p>
    </template>
    <ul v-else-if="contexts.length" class="context-list">
      <li v-for="context in contexts" :key="context.id"><strong>{{ roleNames[context.role] }}</strong><span>{{ context.organization?.name ?? 'Vlastiti profil' }}</span></li>
    </ul>
    <p v-else>Vaš nalog još nema aktivna poslovna ovlaštenja. Obratite se administratoru.</p>
  </section>
</template>
