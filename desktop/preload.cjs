const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("desktopAPI",{
  getInfo:()=>ipcRenderer.invoke("get-info"),
  getState:()=>ipcRenderer.invoke("get-state"),
  setState:(days)=>ipcRenderer.invoke("set-state",days),
  exportBackup:()=>ipcRenderer.invoke("export-backup"),
  importBackup:()=>ipcRenderer.invoke("import-backup"),
  replaceState:(days)=>ipcRenderer.invoke("replace-state",days),
  onStateChanged:(cb)=>ipcRenderer.on("state-changed",()=>cb())
});
