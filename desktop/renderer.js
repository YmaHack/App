(() => {
  const $ = (id) => document.getElementById(id);
  const hebrewFmt = new Intl.DateTimeFormat("he-IL-u-ca-hebrew",{day:"numeric",month:"long",year:"numeric"});
  const hebrewDayFmt = new Intl.DateTimeFormat("he-IL-u-ca-hebrew",{day:"numeric"});
  const hebrewMonthFmt = new Intl.DateTimeFormat("he-IL-u-ca-hebrew",{month:"long"});
  const hebrewYearFmt = new Intl.DateTimeFormat("he-IL-u-ca-hebrew",{year:"numeric"});
  const gregFmt = new Intl.DateTimeFormat("he-IL",{day:"2-digit",month:"2-digit",year:"numeric"});
  const gregShortFmt = new Intl.DateTimeFormat("he-IL",{day:"numeric",month:"numeric"});
  const quotes = [
    "אל תחכה למוטיבציה. תתחיל, והמוטיבציה תדביק אותך.","גם צעד קטן הוא תנועה קדימה.",
    "אתה לא צריך לנצח את כל החיים היום. רק את היום הזה.","רצף נבנה מימים אמיתיים, לא מימים מושלמים.",
    "הצלחה היא היכולת לחזור למסלול פעם נוספת.","היום אפשר לבחור מחדש."
  ];
  const themes=[
    {name:"אוקיינוס 🌊",accent:"#3670dc",darkBg:"#0b1220",darkCard:"#182334",lightBg:"#f2f7fc",lightCard:"#ffffff"},
    {name:"יער 🌿",accent:"#2e8f63",darkBg:"#08140f",darkCard:"#16271f",lightBg:"#f1f8f4",lightCard:"#ffffff"},
    {name:"לבנדר 💜",accent:"#7e5cd6",darkBg:"#120d1e",darkCard:"#231c31",lightBg:"#f7f4fc",lightCard:"#ffffff"},
    {name:"שקיעה 🌅",accent:"#d45b65",darkBg:"#1c0c11",darkCard:"#2f1c22",lightBg:"#fcf5f6",lightCard:"#ffffff"},
    {name:"ענבר ✨",accent:"#d18e2a",darkBg:"#191208",darkCard:"#2e2414",lightBg:"#fcf8ef",lightCard:"#ffffff"}
  ];
  let days={},cursor=new Date(),themeIndex=0,dark=true,timer;
  const pad=n=>String(n).padStart(2,"0");
  const key=d=>`${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`;
  const cloneDate=d=>new Date(d.getFullYear(),d.getMonth(),d.getDate(),12);
  const addDays=(d,n)=>new Date(d.getFullYear(),d.getMonth(),d.getDate()+n,12);
  const visibleEntry=d=>{const e=days[key(d)];return e&&!e.deleted?e:null;};
  function hp(d){const p={};for(const x of hebrewFmt.formatToParts(d))p[x.type]=x.value;return p;}
  const hebrewNumericDayFmt = new Intl.DateTimeFormat("en-US-u-ca-hebrew",{day:"numeric"});
  const hebrewNumericYearFmt = new Intl.DateTimeFormat("en-US-u-ca-hebrew",{year:"numeric"});
  function hebrewNumber(value){
    let n=Number(String(value).replace(/[^0-9]/g,""));
    if(!Number.isFinite(n)||n<=0)return "";
    const hundreds=["","ק","ר","ש","ת"],units=["","א","ב","ג","ד","ה","ו","ז","ח","ט"];
    let out="";
    while(n>=400){out+="ת";n-=400;}
    if(n>=100){out+=hundreds[Math.min(4,Math.floor(n/100))];n%=100;}
    if(n>=90){out+="צ";n-=90;}else if(n>=80){out+="פ";n-=80;}else if(n>=70){out+="ע";n-=70;}else if(n>=60){out+="ס";n-=60;}else if(n>=50){out+="נ";n-=50;}else if(n>=40){out+="מ";n-=40;}else if(n>=30){out+="ל";n-=30;}else if(n>=20){out+="כ";n-=20;}
    if(n===16)return out+"טז";
    if(n===15)return out+"טו";
    if(n>0)out+=units[n];
    if(out.length===1)return out+"׳";
    return out.slice(0,-1)+"״"+out.slice(-1);
  }
  const hebrewDay=d=>hebrewNumber(hebrewNumericDayFmt.format(d)),hebrewMonth=d=>hebrewMonthFmt.format(d),hebrewYear=d=>hebrewNumber(Number(hebrewNumericYearFmt.format(d).replace(/[^0-9]/g,""))%5000);
  const hebrewTitle=d=>`${hebrewMonth(d)} ${hebrewYear(d)}`;
  const greg=d=>gregFmt.format(d), shortGreg=d=>gregShortFmt.format(d);
  const sameHebrewMonth=(a,b)=>{const pa=hp(a),pb=hp(b);return pa.month===pb.month&&pa.year===pb.year;};
  function startHebrewMonth(date){
    let d=cloneDate(date);
    for(let i=0;i<40;i++){
      const raw=Number(hebrewNumericDayFmt.format(d).replace(/[^0-9]/g,""));
      if(raw===1)return d;
      d=addDays(d,-1);
    }
    return d;
  }
  function endHebrewMonth(date){
    let d=startHebrewMonth(date),n=addDays(d,1);
    for(let i=0;i<40&&sameHebrewMonth(n,d);i++){d=n;n=addDays(n,1);}
    return d;
  }
  function themeApply(){
    document.body.classList.toggle("light",!dark);
    const t=themes[themeIndex]||themes[0];
    document.documentElement.style.setProperty("--accent",t.accent);
    document.documentElement.style.setProperty("--dark-bg",t.darkBg);
    document.documentElement.style.setProperty("--dark-card",t.darkCard);
    document.documentElement.style.setProperty("--light-bg",t.lightBg);
    document.documentElement.style.setProperty("--light-card",t.lightCard);
    document.documentElement.style.setProperty("--bg",dark?t.darkBg:t.lightBg);
    document.documentElement.style.setProperty("--card",dark?t.darkCard:t.lightCard);
    document.documentElement.style.setProperty("--accent-soft",t.accent+"22");
  }
  function successCount(){return Object.values(days).filter(e=>e&&!e.deleted&&e.success).length;}
  function currentStreak(){
    let d=cloneDate(new Date());
    if(!visibleEntry(d)?.success)d=addDays(d,-1);
    let n=0;while(visibleEntry(d)?.success){n++;d=addDays(d,-1);}return n;
  }
  function bestStreak(){
    const ks=Object.keys(days).sort();let best=0,n=0,prev=null;
    for(const k of ks){
      const e=days[k];if(!e||e.deleted||!e.success){n=0;prev=null;continue;}
      const d=new Date(k+"T12:00:00");
      if(prev&&key(addDays(prev,1))!==k)n=0;
      n++;best=Math.max(best,n);prev=d;
    }return best;
  }
  function avgScore(){const a=Object.values(days).filter(e=>e&&!e.deleted&&Number(e.score)>=1&&Number(e.score)<=10);return a.length?a.reduce((s,e)=>s+Number(e.score),0)/a.length:null;}
  function setScreen(name){
    ["home","calendar","stats","settings"].forEach(id=>$ (id).classList.toggle("hidden",id!==name));
    document.querySelectorAll(".nav-item").forEach(b=>b.classList.toggle("active",b.dataset.screen===name));
    if(name==="home")renderHome();if(name==="calendar")renderCalendar();if(name==="stats")renderStats();if(name==="settings")renderSettings();
  }
  function renderHome(){
    const today=cloneDate(new Date()),e=visibleEntry(today),streak=currentStreak(),idx=Math.floor(Date.now()/86400000)%quotes.length;
    $("home").innerHTML=`
      <div class="hero"><div><div class="eyebrow">${greg(today)}</div>
      <h2>${streak>=3?`כבר ${streak} ימים ברצף. 🔥`:"צעד קטן. רצף גדול. ✨"}</h2>
      <p>${e?(e.success?"הוכחת לעצמך שאתה יכול לבחור שוב. ✅":"יום אחד לא מגדיר אותך. מחר מתחילים שוב. 💪"):"היום לא צריך להיות מושלם — רק אמיתי."}</p></div>
      <div class="hero-art"><div class="sun">☀️</div><div class="mount">⛰️</div><div class="tree">🌲</div></div></div>
      <div class="stats-row"><div class="stat"><b>🔥 ${streak}</b><span>רצף</span></div><div class="stat"><b>✅ ${successCount()}</b><span>הצלחות</span></div><div class="stat"><b>🏆 ${bestStreak()}</b><span>שיא</span></div></div>
      <div class="card"><div class="card-label">היום 📅</div>
      <h3>${new Intl.DateTimeFormat("he-IL",{weekday:"long",day:"numeric",month:"long"}).format(today)}</h3>
      <div class="hebrew-big">${hebrewFmt.format(today)}</div><div class="muted">${greg(today)}</div>
      <div class="status ${e?(e.success?"ok":"bad"):""}>${e?(e.success?`הצלחת היום ✅ • ציון ⭐ ${e.score}/10`:`לא הצלחת הפעם 🤍 • ציון ⭐ ${e.score}/10`):"עדיין לא עודכן"}</div>
      <button class="primary" id="updateToday">עדכן היום</button></div>
      <div class="card quote-card"><div class="card-label">✦ משפט מוטיבציה</div><div class="quote-row">
      <div class="scene"><span>🌄</span><span>🌲</span></div><div class="quote">${quotes[idx]}</div></div><div class="muted">גם מחר מחכה לך יום חדש. 🌿</div></div>
    `;
    $("updateToday").onclick=()=>openEditor(today);
  }
  function renderCalendar(){
    const today=cloneDate(new Date()),start=startHebrewMonth(cursor),end=endHebrewMonth(cursor),selectedCurrent=sameHebrewMonth(today,cursor);
    const gridStart=addDays(start,-start.getDay()),gridEnd=addDays(end,6-end.getDay());
    let cells="";
    for(let d=cloneDate(gridStart);d<=gridEnd;d=addDays(d,1)){
      const inside=sameHebrewMonth(d,cursor),e=inside?visibleEntry(d):null,isToday=inside&&key(d)===key(today);
      const cls=[inside?"":"faded",isToday?"today":"",e?.success?"success":"",e&&!e.success?"failure":""].join(" ");
      cells+=`<button class="day-cell ${cls}" ${inside?`data-date="${key(d)}"`:"disabled"}><b>${hebrewDay(d)}</b><span>${shortGreg(d)}</span><small>${e?(e.success?"✓":"×"):""}</small></button>`;
    }
    $("calendar").innerHTML=`
      <div class="section-head"><div><div class="eyebrow">לוח עברי־לועזי</div><h2>${hebrewTitle(cursor)}</h2><div class="muted">${greg(start)} → ${greg(end)}</div></div>
      <div class="month-actions"><button class="secondary" id="prevMonth">‹</button><button class="secondary" id="nextMonth">›</button>${selectedCurrent?"":'<button class="secondary" id="backToday">חזרה להיום 📅</button>'}</div></div>
      <div class="card calendar-card"><div class="week-head">${["א","ב","ג","ד","ה","ו","ש"].map(x=>`<span>${x}</span>`).join("")}</div>
      <div class="calendar-grid">${cells}</div><div class="legend">החודש העברי הוא הקובע • לועזי קטן • היום במסגרת כחולה • כחול = הצלחה ✅ • אדום = לא הצלחתי ❌</div></div>
    `;
    $("prevMonth").onclick=()=>{cursor=addDays(start,-1);renderCalendar();};
    $("nextMonth").onclick=()=>{cursor=addDays(end,1);renderCalendar();};
    if($("backToday"))$("backToday").onclick=()=>{cursor=cloneDate(new Date());renderCalendar();};
    document.querySelectorAll(".day-cell[data-date]").forEach(btn=>btn.onclick=()=>openEditor(new Date(btn.dataset.date+"T12:00:00")));
  }
  function renderStats(){
    const avg=avgScore();
    $("stats").innerHTML=`
      <div class="section-head"><div><div class="eyebrow">הנתונים שלך</div><h2>המסע שלך 💪</h2></div></div>
      <div class="big-grid"><div class="big-stat"><span>🔥 רצף נוכחי</span><b>${currentStreak()}</b></div><div class="big-stat"><span>🏆 שיא אישי</span><b>${bestStreak()}</b></div>
      <div class="big-stat"><span>✅ ימים שהצלחת</span><b>${successCount()}</b></div><div class="big-stat"><span>⭐ ממוצע ציון</span><b>${avg==null?"—":avg.toFixed(1)+"/10"}</b></div></div>
      <div class="card tip"><div class="card-label">עידוד</div><div class="tip-text">${currentStreak()>0?"אתה כבר בתוך רצף. עוד יום אחד יכול לחזק את ההרגל. 🔥":"גם התחלה מחדש היא הצלחה. היום אפשר להתחיל. 🌱"}</div></div>
    `;
  }
  function renderSettings(){
    $("settings").innerHTML=`
      <div class="section-head"><div><div class="eyebrow">התאמה אישית</div><h2>המראה והסנכרון ⚙️</h2></div></div>
      <div class="card settings-card"><div class="card-label">ערכת נושא 🎨</div>
      <div class="theme-grid">${themes.map((t,i)=>`<button class="theme-option ${themeIndex===i?"selected":""}" data-theme="${i}"><span class="dot" style="background:${t.accent}"></span>${t.name}</button>`).join("")}</div>
      <label class="setting-row"><span><b>מצב כהה 🌙</b><small>מראה נקי ומודרני</small></span><input id="darkToggle" type="checkbox" ${dark?"checked":""}></label><hr>
      <div class="card-label">סנכרון עם הטלפון 📱</div>
      <p class="muted">התוכנה פותחת שרת מקומי ברשת הביתית. הזן באפליקציית Android כתובת מהשורות הבאות. אין צורך בחשבון ענן.</p>
      <div id="addresses" class="addresses">טוען כתובת…</div>
      <button class="secondary" id="copyAddress">העתק כתובת ראשית 📋</button>
      <button class="primary" id="syncNow">רענן וסנכרן עכשיו 🔄</button><hr>
      <div class="card-label">גיבוי הנתונים 💾</div>
      <div class="button-row"><button class="secondary" id="exportBtn">הוצא גיבוי לימים 📤</button><label class="secondary file-btn">שחזר גיבוי 📥<input id="importFile" type="file" accept="application/json"></label></div></div>
    `;
    document.querySelectorAll(".theme-option").forEach(b=>b.onclick=()=>{themeIndex=Number(b.dataset.theme);themeApply();renderSettings();saveLocalPrefs();});
    $("darkToggle").onchange=e=>{dark=e.target.checked;themeApply();saveLocalPrefs();};
    $("syncNow").onclick=async()=>{try{await window.desktopAPI.setState(days);setSyncPill("● מסונכרן ✅","ok");renderAll();}catch{setSyncPill("● הסנכרון נכשל","bad");}};
    $("copyAddress").onclick=async()=>{try{const inf=await window.desktopAPI.getInfo(),first=inf.addresses?.[0];if(first){await navigator.clipboard.writeText(first);setSyncPill("● הכתובת הועתקה","ok");}}catch{}};
    loadAddresses();$("exportBtn").onclick=exportBackup;$("importFile").onchange=importBackup;
  }
  async function loadAddresses(){
    try{
      const info=await window.desktopAPI.getInfo();
      const addresses=info.addresses||[];
      if(addresses.length===0){
        $("addresses").innerHTML="<code>אין כתובת רשת זמינה. בדוק שה-Wi‑Fi/Ethernet מחובר.</code><code>מחשב מקומי: "+(info.localhost||"http://127.0.0.1:39225")+"</code>";
        setSyncPill("● המחשב מוכן, אין כתובת LAN","bad");
      }else{
        $("addresses").innerHTML=addresses.map(a=>"<code>"+a+"</code>").join("");
        setSyncPill("● כתובת הסנכרון מוכנה ✅","ok");
      }
    }catch(e){
      $("addresses").innerHTML="<code>שגיאה בקריאת כתובת הסנכרון.</code><code>localhost: http://127.0.0.1:39225</code>";
      setSyncPill("● תקלה מקומית בסנכרון","bad");
    }
  }
  const setSyncPill=(t,c="")=>{$("syncPill").textContent=t;$("syncPill").className="sync-pill "+c;};
  function saveLocalPrefs(){localStorage.setItem("challenge-desktop-prefs",JSON.stringify({themeIndex,dark}));}
  function loadLocalPrefs(){try{const p=JSON.parse(localStorage.getItem("challenge-desktop-prefs")||"{}");if(Number.isInteger(p.themeIndex))themeIndex=Math.max(0,Math.min(4,p.themeIndex));if(typeof p.dark==="boolean")dark=p.dark;}catch{}themeApply();}
  function closeModal(){$("modal").classList.add("hidden");$("modalCard").innerHTML="";}
  $("modal").addEventListener("click",e=>{if(e.target.dataset.closeModal)closeModal();});
  function renderAll(){const v=document.querySelector(".screen:not(.hidden)")?.id||"home";setScreen(v);}
  document.querySelectorAll(".nav-item").forEach(b=>b.onclick=()=>setScreen(b.dataset.screen));
  $("settingsBtn").onclick=()=>setScreen("settings");loadLocalPrefs();
  window.desktopAPI?.onStateChanged(()=>{clearTimeout(timer);timer=setTimeout(async()=>{await loadState();renderAll();},150);});
  (async()=>{await loadState();renderHome();setInterval(async()=>{await loadState();const a=document.querySelector(".screen:not(.hidden)")?.id;if(a==="home"||a==="calendar"||a==="stats")renderAll();},3000);})();
})()  function importBackup(e){
    const file=e.target.files?.[0];if(!file)return;
    const reader=new FileReader();
    reader.onload=async()=>{
      try{
        const parsed=JSON.parse(reader.result);
        const imported=(parsed&&parsed.days&&typeof parsed.days==="object")?parsed.days:parsed;
        if(!imported||typeof imported!=="object"||Array.isArray(imported))throw new Error("bad");
        const normalized={}; let offset=0;
        for(const [k,v] of Object.entries(imported)){
          if(!/^\\d{4}-\\d{2}-\\d{2}$/.test(k)||!v||typeof v!=="object")continue;
          normalized[k]={...v,updatedAt:Number(v.updatedAt)||Date.now()+offset++,deleted:v.deleted===true};
        }
        if(Object.keys(normalized).length===0)throw new Error("empty");
        if(!confirm("השחזור יחליף את נתוני המעקב במחשב בגיבוי. להמשיך?"))return;
        const r=await window.desktopAPI.replaceState(normalized);
        days=r?.days||normalized;
        setSyncPill("● הגיבוי שוחזר ✅","ok");
        renderAll();
      }catch(err){alert("קובץ הגיבוי לא תקין או שאין בו ימי מעקב.");}
      finally{e.target.value="";}
    };
    reader.readAsText(file);
  }

  function renderAll(){const v=document.querySelector(".screen:not(.hidden)")?.id||"home";setScreen(v);}
  document.querySelectorAll(".nav-item").forEach(b=>b.onclick=()=>setScreen(b.dataset.screen));
  $("settingsBtn").onclick=()=>setScreen("settings");loadLocalPrefs();
  window.desktopAPI?.onStateChanged(()=>{clearTimeout(timer);timer=setTimeout(async()=>{await loadState();renderAll();},150);});
  (async()=>{await loadState();renderHome();setInterval(async()=>{await loadState();const a=document.querySelector(".screen:not(.hidden)")?.id;if(a==="home"||a==="calendar"||a==="stats")renderAll();},3000);})();
})();