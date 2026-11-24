<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import type { AccessContext, Role } from '../../shared/api/access'
import { roleNames } from '../../shared/api/access'
import { adminRequest, ApiError, dateLabel, organizationTypes } from '../../shared/api/admin'
import type { Organization, OrganizationType, User, Assignment, AuditEvent, Affiliation, Page } from '../../shared/api/admin'
import InvitationAdmin from './InvitationAdmin.vue'
import OrganizationLookup from './OrganizationLookup.vue'

const props = defineProps<{ context: AccessContext }>()
const emit = defineEmits<{ busy: [value: boolean] }>()
type Tab = 'organizations' | 'users' | 'roles' | 'events' | 'invitations'
type Form = '' | 'organization' | 'parent' | 'user' | 'revoke' | 'history'
const isSystem = computed(() => props.context.role === 'SYSTEM_ADMIN')
const tabs: { id: Tab; name: string }[] = [{ id: 'organizations', name: 'Organizacije' }, { id: 'users', name: 'Korisnici' }, { id: 'roles', name: 'Uloge' }, { id: 'invitations', name: 'Pozivi' }, { id: 'events', name: 'Historija promjena' }]
const tab = ref<Tab>('organizations')
const organizations = ref<Organization[]>([])
const users = ref<User[]>([])
const assignments = ref<Assignment[]>([])
const events = ref<AuditEvent[]>([])
const history = ref<Affiliation[]>([])
const search = ref('')
const page = ref(0)
const hasNext = ref(false)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const success = ref('')
const reauthenticate = ref(false)
const form = ref<Form>('')
const editOrganization = ref<Organization>()
const editUser = ref<User>()
const editAssignment = ref<Assignment>()
const fields = reactive({ name: '', type: 'ASSOCIATION' as OrganizationType, email: '', phone: '', parentId: '', subject: '', active: true,
  effectiveFrom: '', reason: '' })
const roleOptions = computed<Role[]>(() => isSystem.value ? ['MINISTRY_ANALYST', 'FEDERATION_ANALYST', 'ASSOCIATION_ADMIN'] : ['OPERATOR', 'WARDEN'])
const parentType = computed<OrganizationType>(() => fields.type === 'FEDERATION' ? 'MINISTRY' : 'FEDERATION')
const zone = Intl.DateTimeFormat().resolvedOptions().timeZone
let requestNumber = 0
let controller: AbortController | undefined

function failure(cause: unknown) {
  error.value = cause instanceof Error ? cause.message : 'Radnja nije uspjela.'
  reauthenticate.value = cause instanceof ApiError && cause.reauthenticate
}
async function load() {
  const request = ++requestNumber
  controller?.abort()
  controller = new AbortController()
  loading.value = true; error.value = ''; reauthenticate.value = false
  organizations.value = []; users.value = []; assignments.value = []; events.value = []
  const path = '/' + tab.value + '?page=' + page.value + '&search=' + encodeURIComponent(search.value)
  try {
    const data = await adminRequest<Page<Organization | User | Assignment | AuditEvent>>(props.context.id, path, 'GET', undefined, controller.signal)
    if (request !== requestNumber) return
    hasNext.value = data.hasNext
    if (tab.value === 'organizations') organizations.value = data.items as Organization[]
    else if (tab.value === 'users') users.value = data.items as User[]
    else if (tab.value === 'roles') assignments.value = data.items as Assignment[]
    else events.value = data.items as AuditEvent[]
  } catch (cause) { if (request === requestNumber && !controller.signal.aborted) failure(cause) }
  finally { if (request === requestNumber) loading.value = false }
}
function switchTab(value: Tab) {
  if (saving.value) return
  tab.value = value; page.value = 0; search.value = ''; form.value = ''; success.value = ''; if(value!=='invitations') void load()
}
function clearForm() {
  error.value = ''; success.value = ''; reauthenticate.value = false
  editOrganization.value = undefined; editUser.value = undefined; editAssignment.value = undefined
  Object.assign(fields, { name: '', type: 'ASSOCIATION', email: '', phone: '', parentId: '', subject: '', active: true, effectiveFrom: '', reason: '' })
}
function organizationForm(item?: Organization) {
  clearForm(); form.value = 'organization'; editOrganization.value = item
  if (item) Object.assign(fields, { name: item.name, type: item.type, email: item.contactEmail ?? '', phone: item.contactPhone ?? '', parentId: item.parent?.id ?? '' })
}
function userForm(item: User) {
  clearForm(); form.value = 'user'; editUser.value = item
  if (item) Object.assign(fields, { name: item.displayName, subject: item.subject, active: item.active })
}
function parentForm(item: Organization) {
  clearForm(); editOrganization.value = item; fields.type = item.type; form.value = 'parent'
}
function revokeForm(item: Assignment) {
  clearForm(); editAssignment.value = item; form.value = 'revoke'
}
async function showHistory(item: Organization) {
  clearForm(); form.value = 'history'; editOrganization.value = item; history.value = []
  try { history.value = await adminRequest<Affiliation[]>(props.context.id, '/organizations/' + item.id + '/history') }
  catch (cause) { failure(cause) }
}
function timestamp(value: string) {
  if (!value) return null
  const result = new Date(value)
  if (Number.isNaN(result.getTime())) throw new Error('Datum nije ispravan.')
  return result.toISOString()
}
async function save() {
  if (saving.value) return
  saving.value = true; emit('busy', true); error.value = ''; reauthenticate.value = false; success.value = ''
  try {
    if (form.value === 'organization') {
      const item = editOrganization.value
      await adminRequest(props.context.id, '/organizations' + (item ? '/' + item.id : ''), item ? 'PUT' : 'POST',
        { ...(item ? { version: item.version } : { type: fields.type, parentId: fields.type === 'MINISTRY' ? null : fields.parentId }),
          name: fields.name, contactEmail: fields.email || null, contactPhone: fields.phone || null, reason: fields.reason })
    } else if (form.value === 'parent' && editOrganization.value) {
      await adminRequest(props.context.id, '/organizations/' + editOrganization.value.id + '/parent', 'POST',
        { version: editOrganization.value.version, parentId: fields.parentId, effectiveFrom: timestamp(fields.effectiveFrom), reason: fields.reason })
    } else if (form.value === 'user') {
      const item = editUser.value
      if (!item) throw new Error('Odaberite korisnika.')
      await adminRequest(props.context.id, '/users/' + item.id, 'PUT',
        { version: item.version, active: fields.active, displayName: fields.name, reason: fields.reason })
    } else if (form.value === 'revoke' && editAssignment.value) {
      await adminRequest(props.context.id, '/roles/' + editAssignment.value.id + '/revoke', 'POST',
        { version: editAssignment.value.version, reason: fields.reason })
    }
    form.value = ''; success.value = 'Promjena je sačuvana.'
    await load()
  } catch (cause) { failure(cause) }
  finally { saving.value = false; emit('busy', false) }
}
const actionNames: Record<string, string> = {
  INVITATION_CREATED: 'Kreiranje poziva', INVITATION_REVOKED: 'Opoziv poziva', INVITATION_RESENT: 'Ponovno slanje poziva', INVITATION_ACCEPTED: 'Prihvatanje poziva',
  BOOTSTRAP_ADMIN: 'Početni administrator', ORGANIZATION_CREATED: 'Nova organizacija', ORGANIZATION_UPDATED: 'Izmjena organizacije',
  PARENT_CHANGED: 'Promjena pripadnosti', USER_CONNECTED: 'Povezivanje naloga', USER_UPDATED: 'Izmjena korisnika', ROLE_GRANTED: 'Dodjela uloge', ROLE_REVOKED: 'Opoziv uloge',
}
onMounted(load)
onBeforeUnmount(() => { requestNumber++; controller?.abort() })
</script>

<template>
  <div class="admin-console">
    <nav class="admin-tabs" aria-label="Administrativne evidencije">
      <button v-for="item in tabs" :key="item.id" :class="{ active: tab === item.id }" :aria-current="tab === item.id ? 'page' : undefined" :disabled="saving" @click="switchTab(item.id)">{{ item.name }}</button>
    </nav>
    <InvitationAdmin v-if="tab === 'invitations'" :context="context" @busy="saving = $event; emit('busy', $event)" />
    <template v-else>
    <p class="scope-label">{{ roleNames[context.role] }} · {{ context.organization?.name ?? 'Sistemski nivo' }}</p>
    <div v-if="error" class="admin-notice error" role="alert">
      <p>{{ error }}</p><a v-if="reauthenticate" class="primary compact" href="/oauth2/authorization/keycloak">Obnovi prijavu</a>
      <button v-else class="text-button" :disabled="saving" @click="form = ''; load()">Osvježi pregled</button>
    </div>
    <p v-if="success" class="admin-notice" role="status">{{ success }}</p>
    <div class="admin-toolbar">
      <form v-if="tab === 'organizations' || tab === 'users'" class="search-row" @submit.prevent="page = 0; load()">
        <input v-model="search" :aria-label="tab === 'organizations' ? 'Pretraga organizacija' : 'Pretraga korisnika'" placeholder="Pretraži po nazivu ili imenu" maxlength="240" :disabled="saving">
        <button class="secondary compact" :disabled="saving || loading">Pretraži</button>
      </form>
      <button v-if="tab === 'organizations' && isSystem" class="primary compact" :disabled="saving" @click="organizationForm()">Nova organizacija</button>
      <button v-if="tab === 'roles'" class="primary compact" :disabled="saving" @click="switchTab('invitations')">Pozovi korisnika</button>
    </div>

    <section v-if="form" class="editor" aria-label="Uređivanje evidencije">
      <template v-if="form === 'history'">
        <h3>Historija pripadnosti · {{ editOrganization?.name }}</h3>
        <ul class="history-list"><li v-for="item in history" :key="item.id"><strong>{{ item.parent.name }}</strong><span>{{ dateLabel(item.validFrom) }} — {{ dateLabel(item.validUntil) }}</span></li></ul>
        <p v-if="history.length === 0">Nema evidentiranih veza.</p>
        <button class="secondary compact" @click="form = ''">Zatvori</button>
      </template>
      <form v-else @submit.prevent="save">
        <fieldset :disabled="saving">
          <template v-if="form === 'organization'">
            <h3>{{ editOrganization ? 'Uredi organizaciju' : 'Nova organizacija' }}</h3>
            <label>Naziv<input v-model="fields.name" required maxlength="240" autocomplete="organization"></label>
            <label v-if="!editOrganization">Tip<select v-model="fields.type"><option v-for="(label, type) in organizationTypes" :key="type" :value="type">{{ label }}</option></select></label>
            <OrganizationLookup v-if="!editOrganization && fields.type !== 'MINISTRY'" v-model="fields.parentId" :context="context.id" :type="parentType" label="Nadređena organizacija" />
            <div class="form-columns"><label>Email<input v-model="fields.email" type="email" maxlength="254" autocomplete="email"></label><label>Telefon<input v-model="fields.phone" type="tel" maxlength="50" autocomplete="tel"></label></div>
          </template>
          <template v-if="form === 'parent'">
            <h3>Promjena pripadnosti · {{ editOrganization?.name }}</h3>
            <p>Prethodna veza ostaje u historiji.</p>
            <OrganizationLookup v-model="fields.parentId" :context="context.id" :type="parentType" :excluded="editOrganization?.parent?.id" label="Nova nadređena organizacija" />
            <label>Početak važenja ({{ zone }})<input v-model="fields.effectiveFrom" type="datetime-local"><small>Prazno polje znači odmah.</small></label>
          </template>
          <template v-if="form === 'user'">
            <h3>Uredi korisnika</h3>
            <label>Ime za prikaz<input v-model="fields.name" required maxlength="240" autocomplete="name"></label>
            <label v-if="editUser" class="checkbox"><input v-model="fields.active" type="checkbox"> Nalog je aktivan</label>
          </template>
          <template v-if="form === 'revoke'">
            <h3>Opoziv uloge</h3><p><strong>{{ editAssignment?.user.displayName }}</strong> · {{ editAssignment ? roleNames[editAssignment.role] : '' }} · {{ editAssignment?.organization?.name }}</p>
            <p>Ovlaštenje prestaje nakon potvrde. Historijski zapis ostaje sačuvan.</p>
          </template>
          <label>Razlog promjene<textarea v-model="fields.reason" required maxlength="1000" rows="2"></textarea></label>
          <div class="editor-actions"><button class="primary compact">{{ saving ? 'Čuvanje…' : form === 'revoke' ? 'Potvrdi opoziv' : 'Sačuvaj' }}</button><button type="button" class="secondary compact" @click="form = ''">Odustani</button></div>
        </fieldset>
      </form>
    </section>

    <p v-if="loading" role="status">Učitavanje podataka…</p>
    <div v-else class="table-scroll">
      <table v-if="tab === 'organizations'"><caption class="sr-only">Organizacije dostupne u izabranoj ulozi</caption><thead><tr><th>Naziv</th><th>Tip</th><th v-if="isSystem">Nadređena organizacija</th><th>Kontakt</th><th>Radnje</th></tr></thead><tbody>
        <tr v-for="item in organizations" :key="item.id"><td><strong>{{ item.name }}</strong><small v-if="item.status !== 'ACTIVE'">Arhivirano</small></td><td>{{ organizationTypes[item.type] }}</td><td v-if="isSystem">{{ item.parent?.name ?? '—' }}</td><td>{{ item.contactEmail ?? '—' }}<small>{{ item.contactPhone }}</small></td><td class="row-actions">
          <button v-if="item.status === 'ACTIVE'" class="text-button" :disabled="saving" @click="organizationForm(item)">Uredi</button>
          <button v-if="isSystem && item.type !== 'MINISTRY' && item.status === 'ACTIVE'" class="text-button" :disabled="saving" @click="parentForm(item)">Promijeni pripadnost</button>
          <button v-if="isSystem && item.type !== 'MINISTRY'" class="text-button" :disabled="saving" @click="showHistory(item)">Historija</button>
        </td></tr><tr v-if="!organizations.length"><td :colspan="isSystem ? 5 : 4">Nema organizacija za ovaj pregled.</td></tr>
      </tbody></table>
      <table v-if="tab === 'users'"><caption class="sr-only">Korisnički nalozi</caption><thead><tr><th>Korisnik</th><th>Status</th><th v-if="isSystem">Radnje</th></tr></thead><tbody>
        <tr v-for="item in users" :key="item.id"><td><strong>{{ item.displayName }}</strong></td><td>{{ item.active ? 'Aktivan' : 'Neaktivan' }}</td><td v-if="isSystem"><button class="text-button" :disabled="saving" @click="userForm(item)">Uredi</button></td></tr>
        <tr v-if="!users.length"><td :colspan="isSystem ? 3 : 2">Nema korisnika za ovaj pregled.</td></tr>
      </tbody></table>
      <table v-if="tab === 'roles'"><caption class="sr-only">Dodjele uloga</caption><thead><tr><th>Korisnik i uloga</th><th>Organizacija</th><th>Važenje</th><th>Radnje</th></tr></thead><tbody>
        <tr v-for="item in assignments" :key="item.id"><td><strong>{{ item.user.displayName }}</strong><small>{{ roleNames[item.role] }}</small></td><td>{{ item.organization?.name ?? 'Sistemski / lični nivo' }}</td><td>{{ dateLabel(item.validFrom) }}<small>{{ item.revokedAt ? 'Opozvano: ' + dateLabel(item.revokedAt) : dateLabel(item.validUntil) }}</small></td><td>
          <button v-if="!item.revokedAt && roleOptions.includes(item.role)" class="text-button" :disabled="saving" @click="revokeForm(item)">Opozovi</button>
        </td></tr><tr v-if="!assignments.length"><td colspan="4">Nema dodjela za ovaj pregled.</td></tr>
      </tbody></table>
      <table v-if="tab === 'events'"><caption class="sr-only">Historija administrativnih promjena</caption><thead><tr><th>Vrijeme i radnja</th><th>Odgovorna osoba</th><th>Evidencija</th><th>Razlog</th></tr></thead><tbody>
        <tr v-for="item in events" :key="item.id"><td>{{ dateLabel(item.occurredAt) }}<small>{{ actionNames[item.action] ?? item.action }}</small></td><td>{{ item.actor }}</td><td>{{ item.organization ?? item.targetUser ?? 'Sistemski nivo' }}<small v-if="item.role">{{ roleNames[item.role] }}</small></td><td>{{ item.reason }}</td></tr>
        <tr v-if="!events.length"><td colspan="4">Nema zabilježenih promjena.</td></tr>
      </tbody></table>
    </div>
    <div class="pagination"><button class="secondary compact" :disabled="page === 0 || loading || saving" @click="page--; load()">Prethodna</button><span>Stranica {{ page + 1 }}</span><button class="secondary compact" :disabled="!hasNext || loading || saving" @click="page++; load()">Sljedeća</button></div>
    </template>
  </div>
</template>
