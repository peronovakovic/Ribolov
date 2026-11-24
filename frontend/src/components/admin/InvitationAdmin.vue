<script setup lang="ts">
import { computed, onMounted, ref, reactive, onBeforeUnmount } from 'vue'
import type { AccessContext, Role } from '../../shared/api/access'
import { roleNames } from '../../shared/api/access'
import { adminRequest, ApiError, dateLabel } from '../../shared/api/admin'
import type { Page, OrganizationType } from '../../shared/api/admin'
import OrganizationLookup from './OrganizationLookup.vue'
interface Invitation { id:string;version:number;email:string;role:Role;organization:string;organizationId:string;expiresAt:string;status:string;sentAt:string|null;attempts:number;deliveryFailure:string|null }
const props=defineProps<{context:AccessContext}>()
const emit=defineEmits<{busy:[value:boolean]}>()
const isSystem=computed(()=>props.context.role==='SYSTEM_ADMIN')
const roles=computed<Role[]>(()=>isSystem.value?['MINISTRY_ANALYST','FEDERATION_ANALYST','ASSOCIATION_ADMIN']:['OPERATOR','WARDEN'])
const orgType=computed<OrganizationType>(()=>fields.role==='MINISTRY_ANALYST'?'MINISTRY':fields.role==='FEDERATION_ANALYST'?'FEDERATION':'ASSOCIATION')
const rows=ref<Invitation[]>([]), page=ref(0), hasNext=ref(false), error=ref(''),success=ref(''),busy=ref(false),loading=ref(false),reauth=ref(false)
const form=ref<''|'create'|'revoke'|'retry'>(''),selected=ref<Invitation>()
const fields=reactive({email:'',role:roles.value[0]!,organizationId:props.context.organization?.id??'',validUntil:'',reason:''})
const statuses:Record<string,string>={PENDING:'Na čekanju',ACCEPTED:'Prihvaćen',REVOKED:'Opozvan',EXPIRED:'Istekao'}
let controller:AbortController|undefined
function fail(e:unknown){error.value=e instanceof Error?e.message:'Radnja nije uspjela.';reauth.value=e instanceof ApiError&&e.reauthenticate}
async function load(){
  controller?.abort();controller=new AbortController();const current=controller;loading.value=true;error.value=''
  try{const data=await adminRequest<Page<Invitation>>(props.context.id,'/invitations?page='+page.value,'GET',undefined,current.signal);if(current.signal.aborted)return;rows.value=data.items;hasNext.value=data.hasNext}
  catch(e){if(!current.signal.aborted)fail(e)}finally{if(!current.signal.aborted)loading.value=false}
}
function open(action:'create'|'revoke'|'retry',item?:Invitation){form.value=action;selected.value=item;error.value='';success.value='';reauth.value=false;fields.reason='';if(action==='create'){fields.email='';fields.validUntil='';fields.organizationId=props.context.organization?.id??''}}
async function save(){
  if(busy.value)return;busy.value=true;emit('busy',true);error.value='';reauth.value=false
  try{
    const path=form.value==='create'?'/invitations':'/invitations/'+selected.value!.id+'/'+form.value
    const body=form.value==='create'?{email:fields.email,role:fields.role,organizationId:fields.organizationId,validUntil:fields.validUntil?new Date(fields.validUntil).toISOString():null,reason:fields.reason}:{version:selected.value!.version,reason:fields.reason}
    await adminRequest(props.context.id,path,'POST',body);form.value='';success.value='Promjena je sačuvana. Slanje poruke obrađuje se u pozadini.';await load()
  }catch(e){fail(e)}finally{busy.value=false;emit('busy',false)}
}
onMounted(load);onBeforeUnmount(()=>controller?.abort())
</script>
<template>
<section aria-label="Pozivi korisnicima">
  <div v-if="error" class="admin-notice error" role="alert"><p>{{error}}</p><a v-if="reauth" href="/oauth2/authorization/keycloak">Obnovi prijavu</a><button v-else class="text-button" :disabled="busy" @click="form='';load()">Osvježi pozive</button></div>
  <p v-if="success" role="status" class="admin-notice">{{success}}</p>
  <div class="admin-toolbar"><p>Poziv važi 48 sati. Uloga se aktivira tek nakon potvrđene prijave i prihvatanja.</p><button class="primary compact" :disabled="busy" @click="open('create')">Pozovi korisnika</button></div>
  <form v-if="form" class="editor" @submit.prevent="save"><fieldset :disabled="busy">
    <h3>{{form==='create'?'Novi poziv':form==='revoke'?'Opozovi poziv':'Ponovo pošalji poziv'}}</h3>
    <template v-if="form==='create'">
      <label>Adresa e-pošte<input v-model="fields.email" type="email" required maxlength="254" autocomplete="off"></label>
      <label>Uloga<select v-model="fields.role"><option v-for="role in roles" :key="role" :value="role">{{roleNames[role]}}</option></select></label>
      <OrganizationLookup v-if="isSystem" v-model="fields.organizationId" :context="context.id" :type="orgType" label="Organizacija"/>
      <p v-else>Organizacija: <strong>{{context.organization?.name}}</strong></p>
      <label>Kraj ovlaštenja ({{Intl.DateTimeFormat().resolvedOptions().timeZone}})<input v-model="fields.validUntil" type="datetime-local"><small>Opciono; mora biti poslije isteka poziva. Prazno znači bez krajnjeg roka.</small></label>
    </template>
    <p v-else>{{selected?.email}} · {{selected?roleNames[selected.role]:''}} · {{selected?.organization}}<br>Ponovno slanje zadržava izvornu vezu i rok važenja.</p>
    <label>Razlog promjene<textarea v-model="fields.reason" required maxlength="1000" rows="2"></textarea></label>
    <div class="editor-actions"><button class="primary compact">{{busy?'Čuvanje…':form==='create'?'Pošalji poziv':'Potvrdi'}}</button><button type="button" class="secondary compact" @click="form=''">Odustani</button></div>
  </fieldset></form>
  <p v-if="loading" role="status">Učitavanje poziva…</p>
  <div v-else class="table-scroll"><table><caption class="sr-only">Pozivi korisnicima</caption><thead><tr><th>Adresa i uloga</th><th>Organizacija</th><th>Status</th><th>Dostava i rok</th><th>Radnje</th></tr></thead><tbody>
    <tr v-for="item in rows" :key="item.id"><td>{{item.email}}<small>{{roleNames[item.role]}}</small></td><td>{{item.organization}}</td><td>{{statuses[item.status]??item.status}}</td><td>{{item.sentAt?'Predato pošti: '+dateLabel(item.sentAt):item.deliveryFailure?(item.attempts>=5?'Dostava nije uspjela; potreban ponovni pokušaj':'Dostava se ponavlja'):'Čeka slanje'}}<small>Važi do {{dateLabel(item.expiresAt)}}</small></td><td class="row-actions"><template v-if="item.status==='PENDING'&&roles.includes(item.role)"><button class="text-button" :disabled="busy" @click="open('retry',item)">Pošalji ponovo</button><button class="text-button" :disabled="busy" @click="open('revoke',item)">Opozovi poziv</button></template></td></tr>
    <tr v-if="!rows.length"><td colspan="5">Nema poziva za ovaj pregled.</td></tr>
  </tbody></table></div>
  <div class="pagination"><button class="secondary compact" :disabled="!page||busy||loading" @click="page--;load()">Prethodna</button><span>Stranica {{page+1}}</span><button class="secondary compact" :disabled="!hasNext||busy||loading" @click="page++;load()">Sljedeća</button></div>
</section>
</template>
