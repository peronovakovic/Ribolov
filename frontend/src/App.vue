<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue'
import InvitationAcceptance from './components/InvitationAcceptance.vue'
import { captureInvitation } from './shared/api/invitations'
import AdminWorkspace from './components/admin/AdminWorkspace.vue'
import { loadSession, logout } from './shared/api/session'
import type { SessionView } from './shared/api/session'

const invitationToken = ref(captureInvitation())
function invitationFinished() { invitationToken.value = ''; void refresh() }
const session = ref<SessionView | null>(null)
const loading = ref(true)
const error = ref('')

async function refresh() {
  loading.value = true
  error.value = ''
  try { session.value = await loadSession() }
  catch (cause) { error.value = cause instanceof Error ? cause.message : 'Pokušajte ponovo.' }
  finally { loading.value = false }
}
async function signOut() {
  error.value = ''
  try { await logout() }
  catch (cause) { error.value = cause instanceof Error ? cause.message : 'Odjava nije uspjela.' }
}
function invitationLinkChanged() { invitationToken.value = captureInvitation() }
onMounted(() => { window.addEventListener('hashchange', invitationLinkChanged); void refresh() })
onBeforeUnmount(() => window.removeEventListener('hashchange', invitationLinkChanged))
</script>

<template>
  <div class="page">
    <header class="header">
      <a class="brand" href="/" aria-label="Ribolov početna">Ribolov<span>Republika Srpska</span></a>
      <button v-if="session" class="secondary" @click="signOut">Odjava</button>
    </header>
    <main>
      <p class="eyebrow">Portal sportskog ribolova</p>
      <h1>{{ session ? 'Dobro došli' : 'Vaš prostor za ribolov' }}</h1>
      <p class="intro">Članstvo, dozvole i ribolovna područja na jednom mjestu.</p>
      <div v-if="loading" role="status" class="notice">Provjera prijave…</div>
      <div v-else-if="error" role="alert" class="notice">
        <p>{{ error }}</p><button class="secondary" @click="refresh">Pokušaj ponovo</button>
      </div>
      <section v-else-if="session" aria-label="Korisnički pristup" class="notice">
        <p>Prijavljeni ste{{ session.displayName ? ' kao ' + session.displayName : '' }}.</p>
        <p>Izaberite ulogu za pristup dostupnim evidencijama.</p>
      </section>
      <a v-else class="primary" href="/oauth2/authorization/keycloak">Prijavite se</a>
      <InvitationAcceptance v-if="invitationToken && !loading" :token="invitationToken" :signed-in="!!session" @finished="invitationFinished" />
      <AdminWorkspace v-if="session && !loading && !error && !invitationToken" />
      <div class="areas" aria-label="Oblasti portala">
        <article><span>01</span><h2>Članstvo</h2><p>Evidencija članstva i članske kartice.</p></article>
        <article><span>02</span><h2>Dozvole</h2><p>Zahtjevi i pregled ribolovnih dozvola.</p></article>
        <article><span>03</span><h2>Područja</h2><p>Vode, reviri i pravila ribolova.</p></article>
      </div>
    </main>
    <footer>Ribolov · Portal u pripremi</footer>
  </div>
</template>