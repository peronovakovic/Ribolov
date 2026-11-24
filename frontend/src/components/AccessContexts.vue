<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { loadAccessContexts, roleNames } from '../shared/api/access'
import type { AccessContext } from '../shared/api/access'

const contexts = ref<AccessContext[]>([])
const loading = ref(true)
const error = ref('')

async function refresh() {
  loading.value = true
  error.value = ''
  contexts.value = []
  try { contexts.value = await loadAccessContexts() }
  catch (cause) { error.value = cause instanceof Error ? cause.message : 'Pokušajte ponovo.' }
  finally { loading.value = false }
}
onMounted(refresh)
</script>

<template>
  <section class="access-contexts" aria-labelledby="access-heading">
    <h2 id="access-heading">Vaša ovlaštenja</h2>
    <p v-if="loading" role="status">Učitavanje ovlaštenja…</p>
    <div v-else-if="error" role="alert">
      <p>{{ error }}</p>
      <button class="secondary" @click="refresh">Pokušaj ponovo</button>
    </div>
    <p v-else-if="contexts.length === 0">Vaš nalog još nema aktivna poslovna ovlaštenja. Obratite se administratoru.</p>
    <ul v-else class="context-list">
      <li v-for="context in contexts" :key="context.id">
        <strong>{{ roleNames[context.role] }}</strong>
        <span>{{ context.organization?.name ?? (context.role === 'ANGLER' ? 'Vlastiti profil' : 'Sistemski nivo') }}</span>
      </li>
    </ul>
  </section>
</template>
