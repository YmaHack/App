const { app, BrowserWindow, ipcMain } = require("electron");
const path = require("path");
const fs = require("fs");
const http = require("http");
const os = require("os");

const PORT = 39225;
const APP_VERSION = 1;
let server;
let mainWindow;

function statePath(){ return path.join(app.getPath("userData"),"state.json"); }
function defaultState(){ return {version:APP_VERSION,updatedAt:Date.now(),days:{}}; }
function readState(){
  try{
    const p=statePath();
    if(!fs.existsSync(p)) return defaultState();
    const x=JSON.parse(fs.readFileSync(p,"utf8"));
    return x.days && typeof x.days==="object" ? {version:APP_VERSION,updatedAt:x.updatedAt||0,days:x.days} : defaultState();
  }catch{return defaultState();}
}
let state=readState();

function saveState(){
  const p=statePath(), tmp=p+".tmp";
  fs.writeFileSync(tmp,JSON.stringify(state,null,2),"utf8");
  fs.renameSync(tmp,p);
}
function normalizeEntry(entry){
  if(!entry || typeof entry!=="object") return null;
  const out={...entry};
  if(!Number.isFinite(Number(out.updatedAt))) out.updatedAt=1;
  if(out.deleted!==true) out.deleted=false;
  return out;
}
function mergeDays(localDays,remoteDays){
  const merged={...(localDays||{})};
  for(const [key,raw] of Object.entries(remoteDays||{})){
    const r=normalizeEntry(raw); if(!r) continue;
    const l=normalizeEntry(merged[key]);
    const rt=Number(r.updatedAt)||1, lt=l?(Number(l.updatedAt)||1):0;
    if(!l || rt>=lt) merged[key]=r;
  }
  return merged;
}
function persist(){
  state.updatedAt=Date.now();
  saveState();
  if(mainWindow && !mainWindow.isDestroyed()) mainWindow.webContents.send("state-changed");
}
function localIPv4Addresses(){
  const out=[];
  for(const items of Object.values(os.networkInterfaces())){
    for(const info of items||[]){
      const family=info && info.family;
      if(info && (family==="IPv4" || family===4) && !info.internal && info.address) out.push(info.address);
    }
  }
  return [...new Set(out)];
}

function syncInfo(){
  const addresses=localIPv4Addresses();
  return {
    name:"אתגר יומי",
    version:APP_VERSION,
    port:PORT,
    addresses:addresses.map(ip=>"http://"+ip+":"+PORT),
    localhost:"http://127.0.0.1:"+PORT
  };
}
function sendJson(res,code,data){
  const body=JSON.stringify(data);
  res.writeHead(code,{
    "Content-Type":"application/json; charset=utf-8",
    "Access-Control-Allow-Origin":"*",
    "Access-Control-Allow-Methods":"GET,POST,OPTIONS",
    "Access-Control-Allow-Headers":"Content-Type",
    "Cache-Control":"no-store"
  });
  res.end(body);
}
function startSyncServer(){
  server=http.createServer((req,res)=>{
    if(req.method==="OPTIONS"){
      res.writeHead(204,{"Access-Control-Allow-Origin":"*","Access-Control-Allow-Methods":"GET,POST,OPTIONS","Access-Control-Allow-Headers":"Content-Type"});
      return res.end();
    }
    const url=new URL(req.url,"http://127.0.0.1:"+PORT);
    if(req.method==="GET" && url.pathname==="/api/info"){
      return sendJson(res,200,syncInfo());
    }
    if(req.method==="GET" && url.pathname==="/api/state") return sendJson(res,200,state);
    if(req.method==="POST" && url.pathname==="/api/state"){
      let body="";
      req.on("data",chunk=>{body+=chunk;if(body.length>3000000)req.destroy();});
      req.on("end",()=>{
        try{
          const incoming=JSON.parse(body||"{}");
          state.days=mergeDays(state.days,incoming.days||{});
          persist();
          return sendJson(res,200,{version:APP_VERSION,updatedAt:state.updatedAt,days:state.days});
        }catch{return sendJson(res,400,{error:"invalid-json"});}
      });
      return;
    }
    sendJson(res,404,{error:"not-found"});
  });
  server.listen(PORT,"0.0.0.0");
}
ipcMain.handle("get-info",()=>syncInfo());
ipcMain.handle("get-state",()=>state);
ipcMain.handle("set-state",(_,days)=>{
  state.days=mergeDays(state.days,days||{});
  persist();
  return state;
});
ipcMain.handle("replace-state",(_,days)=>{
  state.days=(days && typeof days==="object")?days:{};
  persist();
  return state;
});

function createWindow(){
  mainWindow=new BrowserWindow({
    width:1180,height:820,minWidth:980,minHeight:680,
    backgroundColor:"#0b1220",title:"אתגר יומי",
    webPreferences:{preload:path.join(__dirname,"preload.cjs"),contextIsolation:true,nodeIntegration:false}
  });
  mainWindow.loadFile(path.join(__dirname,"index.html"));
}
app.whenReady().then(()=>{
  startSyncServer(); createWindow();
  app.on("activate",()=>{if(BrowserWindow.getAllWindows().length===0)createWindow();});
});
app.on("window-all-closed",()=>{if(process.platform!=="darwin")app.quit();});
app.on("before-quit",()=>{if(server)server.close();});
