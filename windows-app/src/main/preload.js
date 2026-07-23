const { contextBridge, ipcRenderer } = require('electron');

// API segura expuesta al renderer (sin acceso directo a Node).
contextBridge.exposeInMainWorld('api', {
  // Productos / precios
  productosList: () => ipcRenderer.invoke('productos:list'),
  productoSave: (p) => ipcRenderer.invoke('producto:save', p),
  productoDelete: (id) => ipcRenderer.invoke('producto:delete', id),
  preciosAll: () => ipcRenderer.invoke('precios:all'),
  precioSave: (p) => ipcRenderer.invoke('precio:save', p),
  precioDelete: (id) => ipcRenderer.invoke('precio:delete', id),
  tiendasList: () => ipcRenderer.invoke('tiendas:list'),

  // Alacena
  alacenaList: () => ipcRenderer.invoke('alacena:list'),
  alacenaSave: (a) => ipcRenderer.invoke('alacena:save', a),

  // PIN
  pinHas: () => ipcRenderer.invoke('pin:has'),
  pinVerify: (pin) => ipcRenderer.invoke('pin:verify', pin),
  pinSet: (pin) => ipcRenderer.invoke('pin:set', pin),
  pinClear: () => ipcRenderer.invoke('pin:clear'),

  // OpenAI
  keyGet: () => ipcRenderer.invoke('key:get'),
  keySet: (k) => ipcRenderer.invoke('key:set', k),
  ocrScan: () => ipcRenderer.invoke('ocr:scan'),
  ocrAdd: (res, sumar) => ipcRenderer.invoke('ocr:add', res, sumar),

  // Moneda
  monedaGet: () => ipcRenderer.invoke('moneda:get'),
  monedaSet: (code) => ipcRenderer.invoke('moneda:set', code),

  // Enlaces externos / portapapeles
  openExternal: (url) => ipcRenderer.invoke('open:external', url),
  clipboardWrite: (text) => ipcRenderer.invoke('clipboard:write', text),

  // Backup
  backupExport: () => ipcRenderer.invoke('backup:export'),
  backupImport: () => ipcRenderer.invoke('backup:import')
});
