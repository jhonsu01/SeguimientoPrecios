const { contextBridge, ipcRenderer } = require('electron');

// API segura expuesta al renderer (sin acceso directo a Node).
contextBridge.exposeInMainWorld('api', {
  productosList: () => ipcRenderer.invoke('productos:list'),
  productoSave: (p) => ipcRenderer.invoke('producto:save', p),
  productoDelete: (id) => ipcRenderer.invoke('producto:delete', id),
  preciosAll: () => ipcRenderer.invoke('precios:all'),
  precioSave: (p) => ipcRenderer.invoke('precio:save', p),
  precioDelete: (id) => ipcRenderer.invoke('precio:delete', id),
  tiendasList: () => ipcRenderer.invoke('tiendas:list')
});
