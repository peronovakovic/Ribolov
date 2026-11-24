<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { invitationRequest, clearInvitation } from '../shared/api/invitations'
import { ApiError, dateLabel } from '../shared/api/admin'
import type { Role } from '../shared/api/access'
import { roleNames } from '../shared/api/access'
const props=defineProps<{token:string;signedIn:boolean}>()
const emit=defineEmits<{finished:[]}>()
const details=ref<{role:Role;organization:string;expiresAt:string;validUntil:string|null}>(),error=ref(''),busy=ref(false),reauth=ref(false)
function fail(e:unknown){error.value=e instanceof Error?e.message:'Poziv nije dostupan.';reauth.value=e instanceof ApiError&&e.reauthenticate}
async function preview(){busy.value=true;error.value='';try{details.value=await invitationRequest('preview',props.token)}catch(e){fail(e)}finally{busy.value=false}}
async function accept(){if(busy.value)return;busy.value=true;error.value='';try{await invitationRequest('accept',props.token);clearInvitation();emit('finished')}catch(e){fail(e)}finally{busy.value=false}}
function cancel(){clearInvitation();emit('finished')}
onMounted(()=>{if(props.signedIn)void preview()})
</script>
<template>
<section class="workspace" aria-labelledby="invitation-heading">
<h2 id="invitation-heading">Poziv za poslovnu ulogu</h2>
<p v-if="!signedIn">Prijavite se ili napravite nalog s adresom na koju je poslan poziv. Potrebni su potvrđena adresa e-pošte i drugi faktor prijave.</p>
<a v-if="!signedIn" class="primary compact" href="/oauth2/authorization/keycloak">Prijavi se za prihvatanje</a>
<div v-if="error" class="admin-notice error" role="alert"><p>{{error}}</p><a v-if="reauth" href="/oauth2/authorization/keycloak">Obnovi prijavu</a><button v-else class="text-button" :disabled="busy" @click="preview">Provjeri ponovo</button></div>
<p v-if="busy" role="status">Provjera poziva…</p>
<template v-if="details"><p><strong>{{roleNames[details.role]}}</strong> · {{details.organization}}</p><p>Poziv važi do {{dateLabel(details.expiresAt)}}. Ovlaštenje: {{details.validUntil?'do '+dateLabel(details.validUntil):'bez krajnjeg roka'}}.</p><button class="primary compact" :disabled="busy" @click="accept">Prihvati poziv</button></template>
<button class="secondary compact" :disabled="busy" @click="cancel">Zatvori poziv</button>
</section>
</template>
