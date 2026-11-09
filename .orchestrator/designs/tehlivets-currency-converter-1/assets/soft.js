const $=s=>document.querySelector(s);
const $$=s=>[...document.querySelectorAll(s)];
const scope=document.body.dataset.product;
const key=n=>`vtehsol-soft-r6:${scope}:${n}`;
const read=(n,f)=>{try{return JSON.parse(localStorage.getItem(key(n)))??f}catch{return f}};
const save=(n,v)=>{try{localStorage.setItem(key(n),JSON.stringify(v))}catch{notice('This device cannot save changes.')}};
const uk=scope==='quiz-show';
const notice=t=>{$('#toast').textContent=t;clearTimeout(window.noticeTimeout);window.noticeTimeout=setTimeout(()=>$('#toast').textContent='',3500)};
const escapeText=t=>String(t).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const go=t=>location.href=t;
$('.navtoggle')?.addEventListener('click',e=>{const on=document.body.classList.toggle('menuopen');e.currentTarget.setAttribute('aria-expanded',String(on));e.currentTarget.textContent=on?'×':'☰'});
$$('[data-open]').forEach(b=>b.addEventListener('click',()=>document.getElementById(b.dataset.open)?.showModal()));
$$('[data-close]').forEach(b=>b.addEventListener('click',()=>b.closest('dialog').close()));
$$('dialog').forEach(d=>d.addEventListener('click',e=>{if(e.target===d){const r=d.getBoundingClientRect();if(e.clientX<r.left||e.clientX>r.right||e.clientY<r.top||e.clientY>r.bottom)d.close()}}));
$$('[data-notice]').forEach(b=>b.addEventListener('click',()=>notice(b.dataset.notice)));
async function copy(t){try{await navigator.clipboard.writeText(t);notice(uk?'Скопійовано.':'Copied.')}catch{notice(uk?'Оберіть і скопіюйте посилання.':'Select the text and copy it.')}}
$$('[data-copy]').forEach(b=>b.addEventListener('click',()=>copy(document.getElementById(b.dataset.copy)?.value||'')));
$$('[data-copy-text]').forEach(b=>b.addEventListener('click',()=>copy(b.dataset.copyText)));
$$('[data-toggle-label]').forEach(b=>b.addEventListener('click',()=>{const t=b.firstChild.textContent;b.firstChild.textContent=b.dataset.toggleLabel;b.dataset.toggleLabel=t}));
$$('[data-complete]').forEach(b=>b.addEventListener('click',()=>{b.disabled=true;b.firstChild.textContent=uk?'Збережено':'Completed';notice(uk?'Збережено.':'Marked complete.')}));
$$('[data-increment]').forEach(b=>b.addEventListener('click',()=>{b.firstChild.textContent='✓ +1';b.disabled=true;save('score',(read('score',12))+1);notice('Бал додано.')}));
$$('[data-day]').forEach((b,i)=>{const old=read('habit-'+i,null);if(old!==null){b.classList.toggle('done',old);b.setAttribute('aria-pressed',String(old))}b.addEventListener('click',()=>{const v=b.classList.toggle('done');b.setAttribute('aria-pressed',String(v));save('habit-'+i,v)})});
$$('input.check').forEach((b,i)=>{const k=location.pathname.split('/').pop()+'-check-'+i;const v=read(k,null);if(v!==null)b.checked=v;b.addEventListener('change',()=>save(k,b.checked))});
let remaining=25*60,running=false,tick;
if($('#timer')){const raw=$('#timer').textContent.split(':').map(Number);remaining=raw[0]*60+raw[1];const rhythm=read('rhythm',null);if(rhythm&&location.pathname.endsWith('focus.html'))remaining=Number(rhythm.work)*60}
const initial=remaining;
const renderTime=()=>{if($('#timer')){$('#timer').textContent=String(Math.floor(remaining/60)).padStart(2,'0')+':'+String(remaining%60).padStart(2,'0');const ring=$('#timer').closest('.dial');if(ring)ring.style.background='conic-gradient(var(--strong) '+(remaining/initial*360)+'deg,#dfe6df 0deg)'}};
renderTime();
$('[data-timer]')?.addEventListener('click',e=>{running=!running;e.currentTarget.firstChild.textContent=running?(uk?'Пауза':'Pause'):(uk?'Продовжити':'Continue');$('#timer-status').textContent=running?(uk?'Час іде':'One thing at a time'):(uk?'Пауза':'Take your time');clearInterval(tick);if(running)tick=setInterval(()=>{remaining=Math.max(0,remaining-1);renderTime();if(!remaining){clearInterval(tick);running=false;notice(uk?'Час вийшов.':'Your interval is complete.');if(!uk)go('break.html')}},1000)});
$('[data-reset]')?.addEventListener('click',()=>{clearInterval(tick);running=false;remaining=initial;renderTime();$('[data-timer]').firstChild.textContent='Start focus';$('#timer-status').textContent='Ready when you are'});
let water=read('water',1250),waterHistory=[];
function renderWater(){if(!$('#water-total'))return;const goal=read('watergoal',2000);$('#water-total').textContent=water.toLocaleString('en');$('#water-total').nextElementSibling.textContent='/ '+goal.toLocaleString('en')+' ml';$('#water-progress').style.width=Math.min(100,water/goal*100)+'%'}
renderWater();
$$('[data-water]').forEach(b=>b.addEventListener('click',()=>{waterHistory.push(water);water+=Number(b.dataset.water);save('water',water);renderWater();notice('Sip kept.')}));
$('[data-water-undo]')?.addEventListener('click',()=>{if(waterHistory.length){water=waterHistory.pop();save('water',water);renderWater();notice('Last sip removed.')}else notice('No sip to undo in this visit.')});
function filter(){const q=($('#search')?.value||'').toLowerCase();const category=$('#category')?.value||'All categories';let n=0;$$('[data-search-item]').forEach(e=>{const visible=e.textContent.toLowerCase().includes(q)&&(category==='All categories'||e.dataset.category===category);e.hidden=!visible;if(visible)n++});if($('#search-status'))$('#search-status').textContent=n?`${n} patterns to explore.`:'No patterns match. Try a different word or category.'}
$('#search')?.addEventListener('input',filter);$('#category')?.addEventListener('change',filter);
$('[data-spin]')?.addEventListener('click',e=>{const w=$('#wheel');w.classList.remove('spinning');requestAnimationFrame(()=>w.classList.add('spinning'));e.currentTarget.disabled=true;setTimeout(()=>{w.querySelector('span').textContent='Запитання 1';e.currentTarget.disabled=false},1650)});
let audio,beatTimer,beat=0,taps=[];
const bpm=()=>Math.max(30,Math.min(240,Number($('#bpm')?.value)||72));
if($('#bpm')){$('#bpm').min='30';$('#bpm').max='240';$('#bpm').addEventListener('input',()=>$('#bpm-display').innerHTML=bpm()+' <small>BPM</small>')}
function pulse(){const beatNodes=$$('.beat');beatNodes.forEach((e,i)=>e.classList.toggle('active',i===beat%4));if(audio){const osc=audio.createOscillator(),gain=audio.createGain();osc.frequency.value=beat%4===0?880:580;gain.gain.setValueAtTime(.08,audio.currentTime);gain.gain.exponentialRampToValueAtTime(.001,audio.currentTime+.05);osc.connect(gain);gain.connect(audio.destination);osc.start();osc.stop(audio.currentTime+.06)}beat++;const sub=$('#subdivision')?.value;beatTimer=setTimeout(pulse,60000/bpm()/(sub==='Eighth notes'?2:sub==='Triplets'?3:1))}
$('[data-metronome]')?.addEventListener('click',e=>{if(beatTimer){clearTimeout(beatTimer);beatTimer=null;e.currentTarget.firstChild.textContent='Start pulse'}else{try{audio ||= new (window.AudioContext||window.webkitAudioContext)();audio.resume()}catch{notice('Audio is unavailable. You can follow the pulse.')}pulse();e.currentTarget.firstChild.textContent='Stop pulse'}});
$('[data-tap]')?.addEventListener('click',()=>{const t=performance.now();if(taps.length&&t-taps.at(-1)>2500)taps=[];taps.push(t);taps=taps.slice(-5);if(taps.length>1){const value=Math.round(60000/((taps.at(-1)-taps[0])/(taps.length-1)));$('#bpm').value=Math.min(240,Math.max(30,value));$('#bpm').dispatchEvent(new Event('input'))}});
let scrollTimer;
$('[data-autoscroll]')?.addEventListener('click',e=>{if(scrollTimer){cancelAnimationFrame(scrollTimer);scrollTimer=null;e.currentTarget.firstChild.textContent='Start auto-scroll';return}e.currentTarget.firstChild.textContent='Stop auto-scroll';const step=()=>{window.scrollBy(0,.5);if(window.innerHeight+window.scrollY<document.body.scrollHeight-2)scrollTimer=requestAnimationFrame(step);else{scrollTimer=null;e.currentTarget.firstChild.textContent='Start auto-scroll'}};scrollTimer=requestAnimationFrame(step)});
const notes=['C','C#','D','D#','E','F','F#','G','G#','A','A#','B'];
function transpose(t,n){return t.replace(/\b([A-G](?:#|b)?)(m|maj|min|sus|dim|aug|add|[0-9])?/g,(m,note,suffix='')=>{const aliases={Db:'C#',Eb:'D#',Gb:'F#',Ab:'G#',Bb:'A#'};const i=notes.indexOf(aliases[note]||note);return i<0?m:notes[(i+n%12+12)%12]+suffix})}
$('[data-transpose]')?.addEventListener('click',()=>{const n=notes.indexOf($('#song-key').value)-7;$$('.chord').forEach((e,i)=>e.textContent=transpose(['G','D','Em','C','G','D','Em','C','Em','C'][i]||'G',n));notice('Your reading is updated.')});
$$('[data-chord]').forEach(b=>b.addEventListener('click',()=>{const e=document.createElement('div');e.className='chord';e.textContent=b.dataset.chord;$('#progression-chords').append(e)}));
$('[data-clear-chords]')?.addEventListener('click',()=>$('#progression-chords').replaceChildren());
const defaultRates=[{from:'USD',to:'PLN',rate:'0.123',date:'2026-11-09T12:00'},{from:'PLN',to:'EUR',rate:'0.123',date:'2026-11-09T12:00'}];
const rates=()=>read('rates',defaultRates);
function decimal(t){if(!/^\d+(\.\d+)?$/.test(String(t)))throw Error('Use a positive number with a dot for decimals.');const parts=String(t).split('.');return {n:BigInt(parts.join('')),s:(parts[1]||'').length}}
function multiply(a,b){return {n:a.n*b.n,s:a.s+b.s}}
function decimalText(d){let s=d.n.toString().padStart(d.s+1,'0');if(d.s)s=s.slice(0,-d.s)+'.'+s.slice(-d.s);return s.includes('.')?s.replace(/0+$/,'').replace(/\.$/,''):s}
function convert(v){let amount=decimal(v.amount);if(amount.n<=0n)throw Error('Enter an amount greater than zero.');if(v.from===v.to)throw Error('Choose two different currencies.');const route=(v.from==='PLN'||v.to==='PLN')?[v.from,v.to]:[v.from,'PLN',v.to];const used=[],steps=[{amount:decimalText(amount),currency:v.from}];for(let i=0;i<route.length-1;i++){const pair=rates().filter(r=>r.from===route[i]&&r.to===route[i+1]).sort((a,b)=>Math.abs(new Date(a.date)-new Date(v['conversion-date']))-Math.abs(new Date(b.date)-new Date(v['conversion-date'])));if(!pair.length)return {missing:true,from:route[i],to:route[i+1]};const rate=pair[0];amount=multiply(amount,decimal(rate.rate));used.push(rate);steps.push({amount:decimalText(amount),currency:route[i+1]})}return {amount:decimalText(amount),currency:v.to,source:v.amount+' '+v.from,date:v['conversion-date'],steps,used}}
if($('#rate-rows'))$('#rate-rows').innerHTML=rates().map(r=>'<tr><td>'+escapeText(r.from+' → '+r.to)+'</td><td>'+escapeText(r.rate)+'</td><td>'+escapeText(r.date.replace('T',' '))+'</td></tr>').join('');
const result=read('conversion',null);
if(result&&!result.missing&&$('#currency-result')){$('#currency-result').innerHTML=escapeText(result.amount)+' <small>'+escapeText(result.currency)+'</small>';$('#currency-summary').textContent=result.source+' · '+result.date.replace('T',' ');$('#currency-path').innerHTML=result.steps.map(s=>'<span class="node">'+escapeText(s.amount+' '+s.currency)+'</span>').join('<span aria-hidden="true">→</span>');$('#currency-rates').innerHTML=result.used.map(r=>'<div class="row"><div><strong>'+escapeText(r.from+' → '+r.to)+'</strong><p>'+escapeText(r.date.replace('T',' '))+'</p></div><strong>'+escapeText(r.rate)+'</strong></div>').join('')}
if($('#calorie-result'))$('#calorie-result').innerHTML=Number(read('calories',2100)).toLocaleString('en')+' <small>kcal / day</small>';
if($('#afford-result'))$('#afford-result').innerHTML=Number(read('afford',4970)).toLocaleString('en')+' <small>PLN</small>';
if($('#picker-result')){const choices=[['Transactional Outbox','outbox'],['Circuit Breaker','circuit-breaker'],['CQRS','cqrs'],['Saga','saga']];const r=choices[read('picker',0)];$('#picker-result').innerHTML='<a class="listlink" href="pattern-'+r[1]+'.html"><h3>'+r[0]+' ↗</h3><p>Explore its fit, limits, and related patterns.</p></a>'}
const existing=read('entries',[]).filter(x=>x.target===location.pathname.split('/').pop());
if(existing.length){const host=$('#added-items')||$('main .core');if(host){const list=document.createElement('div');for(const e of existing){const r=document.createElement('div');r.className='row';const text=document.createElement('strong');text.textContent=e.name;r.append(text);if(e.amount){const amount=document.createElement('small');amount.textContent=e.amount+' '+(e.currency||'');r.append(amount)}list.append(r)}host.prepend(list)}}
$$('form').forEach(f=>f.addEventListener('submit',e=>{e.preventDefault();if(!f.reportValidity())return;const values=Object.fromEntries(new FormData(f));const err=f.querySelector('.form-error');if(err)err.textContent='';try{if(values.password&&values.password.length<10)throw Error('Use at least 10 characters for your password.');if(values.confirm&&values.password!==values.confirm)throw Error('The passwords do not match.');for(const input of f.querySelectorAll('input[type=number]'))if(Number(input.value)<0)throw Error('Use zero or a positive number.');const kind=f.dataset.kind;
if(kind==='ask'){const entries=read('entries',[]);entries.push({name:values.name,target:'inbox.html'});save('entries',entries);go('ask-result.html');return}
if(kind==='currency'){const r=convert(values);save('conversion',r);go(r.missing?'missing-rate.html':'result.html');return}
if(kind==='rate'){if(values['rate-from']===values['rate-to'])throw Error('Choose two different currencies.');if(decimal(values.rate).n<=0n)throw Error('Use a rate greater than zero.');const all=rates();all.push({from:values['rate-from'],to:values['rate-to'],rate:values.rate,date:values['rate-date']});save('rates',all)}
if(kind==='rhythm'){for(const v of Object.values(values))if(Number(v)<1||Number(v)>180)throw Error('Use intervals between 1 and 180.');save('rhythm',values)}
if(kind==='watergoal'){if(Number(values.goal)<100||Number(values.goal)>10000)throw Error('Use a goal between 100 and 10,000 ml.');save('watergoal',Number(values.goal))}
if(kind==='calorie'){const age=Number(values.age),w=Number(values.weight),h=Number(values.height);if(age<1||age>120||w<10||w>500||h<50||h>260)throw Error('Check your age, weight, and height.');const base=10*w+6.25*h-5*age+(values.sex==='Male'?5:-161);const factor={'Sedentary':1.2,'Lightly active':1.375,'Moderately active':1.55,'Very active':1.725}[values.activity];save('calories',Math.round(base*factor))}
if(kind==='afford')save('afford',5870-Number(values.amount));
if(kind==='picker'){const choices=['A saved change must publish a message','A failing dependency affects everything','Reads and writes need different models','A workflow crosses service boundaries'];save('picker',Math.max(0,choices.indexOf(values.concern)))}
if(kind==='tone'){const tempo=Number(values.tempo);if(tempo<40||tempo>220)throw Error('Use a tempo between 40 and 220 BPM.');const factor=values.delay==='Dotted eighth'?.75:values.delay==='Eighth'?.5:1;$('#tone-output').textContent=values.tone+' · Delay '+Math.round(60000/tempo*factor)+' ms · Start with a clean amp and adjust by ear.';notice('Your starting tone is ready.');return}
if(kind==='chart'){$('#chart-output').textContent=transpose(values.chart,Number(values.semitones));notice('Chart transposed.');return}
if(kind==='upload'){finishMediaUpload(f);return}
if(['trash','release','payment','goal','revoke'].includes(kind)){f.closest('dialog')?.close();notice(kind==='trash'?(uk?'Видалено.':'Removed.'):kind==='release'?'Reservation released.':kind==='payment'?'Payment recorded.':'Amount added.');if(kind==='release')go('dashboard.html');if(kind==='trash')go(uk?'lobby.html':scope==='gtd-saas'?'dashboard.html':'inbox.html');return}
if(values.name&&!values.password&&!values.email){const entries=read('entries',[]);entries.push({name:values.name,amount:values.amount||'',currency:values.currency||'',target:f.dataset.target});save('entries',entries)}
const submit=f.querySelector('[type=submit]');if(submit){submit.disabled=true;submit.firstChild.textContent=uk?'Зберігаємо…':'Saving…'}setTimeout(()=>go(f.dataset.target||'saved.html'),180)
}catch(error){if(err)err.textContent=error.message;else notice(error.message)}}));
$$('[data-export]').forEach(b=>b.addEventListener('click',()=>{const entries=read('entries',[]);const content=b.dataset.export==='json'?JSON.stringify(entries,null,2):'name,amount,currency\n'+entries.map(e=>[e.name,e.amount,e.currency].map(v=>'"'+String(v||'').replace(/"/g,'""')+'"').join(',')).join('\n');const url=URL.createObjectURL(new Blob([content],{type:b.dataset.export==='json'?'application/json':'text/csv'}));const a=document.createElement('a');a.href=url;a.download='my-activity.'+b.dataset.export;a.click();setTimeout(()=>URL.revokeObjectURL(url),500)}));
function filterActions(){const c=$('#filter-context')?.value,e=$('#filter-energy')?.value;let count=0;$$('[data-filter-row]').forEach(r=>{r.hidden=(c!=='All contexts'&&r.dataset.context!==c)||(e!=='All energy levels'&&r.dataset.energy!==e);if(!r.hidden)count++});if($('#task-filter-status'))$('#task-filter-status').hidden=count>0}
$('#filter-context')?.addEventListener('change',filterActions);$('#filter-energy')?.addEventListener('change',filterActions);

const uploadFields=$$('[data-upload]');
const uploadWords=uk?{ready:'Готово до додавання',choose:'Обрати файл',replace:'Обрати інший файл',required:'Оберіть файл для завантаження.',empty:'Цей файл порожній. Оберіть інший.',type:'Оберіть підтримуваний аудіо- або відеофайл.',large:'Файл завеликий. Максимальний розмір — 200 МБ.',adding:'Завантажуємо',done:'Файл додано',removed:'Файл прибрано.',preview:'Перегляд недоступний. Файл можна додати.'}:{ready:'Ready to add',choose:'Choose a photo',replace:'Choose another photo',required:'Choose a photo to continue.',empty:'This file is empty. Choose another photo.',type:'Choose a JPG, PNG, WebP or HEIC photo.',large:'This photo is too large. Use a file up to 10 MB.',adding:'Adding photo',done:'Photo added',removed:'Photo removed.',preview:'Preview unavailable. You can still add this photo.'};
const uploadModels=new Map();
function paintUpload(field,file,error=''){
const input=field.querySelector('input[type=file]'),model=uploadModels.get(field);
if(model.url){URL.revokeObjectURL(model.url);model.url=null}
model.file=error?null:file;model.error=error;field.dataset.state=error?'error':file?'ready':'empty';
const row=field.querySelector('.upload-selected'),message=field.querySelector('.upload-error'),status=field.querySelector('[data-file-status]'),image=field.querySelector('img');
row.hidden=!file;message.hidden=!error;message.textContent=error;input.setAttribute('aria-invalid',String(Boolean(error)));field.querySelector('[data-file-pick]').setAttribute('aria-invalid',String(Boolean(error)));image.hidden=true;image.removeAttribute('src');
field.querySelector('[data-file-pick]').firstChild.textContent=file?uploadWords.replace:uploadWords.choose;
field.querySelector('.upload-progress').hidden=true;
if(file){field.querySelector('[data-file-name]').textContent=file.name;field.querySelector('[data-file-size]').textContent=file.size>=1048576?(file.size/1048576).toFixed(1)+' MB':Math.max(.1,file.size/1024).toFixed(1)+' KB';status.textContent=error?'':uploadWords.ready;
if(!error&&file.type.startsWith('image/')){model.url=URL.createObjectURL(file);image.onload=()=>image.hidden=false;image.onerror=()=>{image.hidden=true;status.textContent=uploadWords.preview};image.src=model.url;}}
field.querySelector('.upload-live').textContent=error||file?.name||uploadWords.removed;
}
function chooseUpload(field,incoming){
const file=incoming[0];if(!file)return;
const input=field.querySelector('input[type=file]'),accepted=input.accept.split(',');let error='';
if(!file.size)error=uploadWords.empty;else if(!accepted.includes(file.type.toLowerCase()))error=uploadWords.type;else if(file.size>Number(field.dataset.maxBytes))error=uploadWords.large;
const transfer=new DataTransfer();if(!error)transfer.items.add(file);input.files=transfer.files;paintUpload(field,file,error);
}
for(const field of uploadFields){
const input=field.querySelector('input[type=file]'),pick=field.querySelector('[data-file-pick]'),zone=field.querySelector('[data-dropzone]');uploadModels.set(field,{file:null,url:null,error:''});
pick.addEventListener('click',()=>input.click());input.addEventListener('change',()=>chooseUpload(field,[...input.files]));
zone.addEventListener('dragover',e=>{e.preventDefault();field.dataset.dragging='true'});
zone.addEventListener('dragleave',e=>{if(!zone.contains(e.relatedTarget))field.dataset.dragging='false'});
zone.addEventListener('drop',e=>{e.preventDefault();field.dataset.dragging='false';if(field.dataset.state!=='uploading')chooseUpload(field,[...e.dataTransfer.files])});
field.querySelector('[data-file-remove]').addEventListener('click',()=>{if(field.dataset.state==='uploading')return;input.value='';paintUpload(field,null);pick.focus()});
input.form?.addEventListener('reset',()=>queueMicrotask(()=>paintUpload(field,null)));
}
async function animateFileUpload(field){
field.dataset.state='uploading';const progress=field.querySelector('.upload-progress');progress.hidden=false;field.querySelector('[data-file-pick]').disabled=true;field.querySelector('[data-file-remove]').disabled=true;
for(const percent of [12,38,68,100]){await new Promise(r=>setTimeout(r,180));progress.setAttribute('aria-valuenow',String(percent));progress.querySelector('span').style.transform='scaleX('+percent/100+')';field.querySelector('[data-file-status]').textContent=uploadWords.adding+' · '+percent+'%'}
field.dataset.state='complete';field.querySelector('[data-file-status]').textContent=uploadWords.done;field.querySelector('.upload-live').textContent=uploadWords.done;field.querySelector('[data-file-pick]').disabled=false;field.querySelector('[data-file-remove]').disabled=false;
}
const uploadForms=[...new Set(uploadFields.map(f=>f.querySelector('input').form).filter(Boolean))];
for(const form of uploadForms){form.addEventListener('submit',async e=>{
if(form.dataset.uploadReady==='true'){delete form.dataset.uploadReady;return}
const fields=uploadFields.filter(f=>f.querySelector('input').form===form);const invalid=fields.find(f=>uploadModels.get(f).error||(f.dataset.required==='true'&&!uploadModels.get(f).file));
if(invalid){e.preventDefault();e.stopImmediatePropagation();const m=uploadModels.get(invalid);if(!m.error){m.error=uploadWords.required;invalid.querySelector('.upload-error').textContent=m.error;invalid.querySelector('.upload-error').hidden=false;invalid.querySelector('input').setAttribute('aria-invalid','true')}invalid.querySelector('[data-file-pick]').focus();return}
const selected=fields.filter(f=>uploadModels.get(f).file);if(!selected.length)return;
e.preventDefault();e.stopImmediatePropagation();if(form.dataset.uploading==='true')return;form.dataset.uploading='true';const submit=form.querySelector('[type=submit]');if(submit)submit.disabled=true;
await Promise.all(selected.map(animateFileUpload));form.dataset.uploadReady='true';delete form.dataset.uploading;if(submit)submit.disabled=false;form.requestSubmit();
},true)}
function finishMediaUpload(form){const input=form.querySelector('input[type=file]'),file=input.files[0];if(!file)throw Error(uploadWords.required);const card=document.createElement('div');card.className='uploaded-asset';const mark=document.createElement('span');mark.className='mark';mark.textContent='✓';mark.setAttribute('aria-hidden','true');const content=document.createElement('div');const name=document.createElement('strong');name.textContent=file.name;const caption=document.createElement('p');caption.className='small';caption.textContent=uploadWords.done;content.append(name,caption);card.append(mark,content);$('#uploaded-media').append(card);$('#uploaded-media')?.parentElement.querySelector('.empty')?.remove();form.reset();notice(uploadWords.done)}
window.addEventListener('pagehide',()=>{for(const model of uploadModels.values())if(model.url)URL.revokeObjectURL(model.url)});
