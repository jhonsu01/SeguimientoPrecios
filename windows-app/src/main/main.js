const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const db = require('./db');

function crearVentana() {
  const win = new BrowserWindow({
    width: 1120,
    height: 760,
    minWidth: 900,
    minHeight: 600,
    backgroundColor: '#0F172A',
    icon: path.join(__dirname, '..', '..', 'build', 'icon.ico'),
    title: 'Seguimiento Precios',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false
    }
  });
  win.setMenuBarVisibility(false);
  win.loadFile(path.join(__dirname, '..', 'renderer', 'index.html'));
}

function registrarIpc() {
  ipcMain.handle('productos:list', () => db.productosList());
  ipcMain.handle('producto:save', (_e, p) => db.productoSave(p));
  ipcMain.handle('producto:delete', (_e, id) => db.productoDelete(id));
  ipcMain.handle('precios:all', () => db.preciosAll());
  ipcMain.handle('precio:save', (_e, p) => db.precioSave(p));
  ipcMain.handle('precio:delete', (_e, id) => db.precioDelete(id));
  ipcMain.handle('tiendas:list', () => db.tiendasList());
}

app.whenReady().then(async () => {
  await db.init(app.getPath('userData'));
  registrarIpc();
  crearVentana();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) crearVentana();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
