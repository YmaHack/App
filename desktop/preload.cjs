const { contextBridge, ipcRenderer } = require("electron");
contextBridge.exposeInMainWorld("desktopAPI",{
  getInfo:()=>fetch("http://127.0.0.1:39225/api/info").then(r=>r.json()),
  getState:()=>fetch("http://127.0.0.1:39225/api/state").then(r=>r.json()),
  setState:(days)=>fetch("http://127.0.0.1:39225/api/state",{
    method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({version:1,days})
  }).then(r=>r.json()),
  onStateChanged:(cb)=>ipcRenderer.on("state-changed",()=>cb())
});
