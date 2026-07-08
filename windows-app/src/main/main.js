const { app, BrowserWindow, ipcMain, dialog, shell } = require('electron');
const path = require('path');
const db = require('./db');
const config = require('./config');
const ocr = require('./ocr');
const backup = require('./backup');

let mainWindow = null;

function nombreBackup() {
  const d = new Date();
  const p = (n) => String(n).padStart(2, '0');
  return `SeguimientoPrecios-backup-${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}-${p(d.getHours())}${p(d.getMinutes())}.zip`;
}

function crearVentana() {
  mainWindow = new BrowserWindow({
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
  mainWindow.setMenuBarVisibility(false);
  // Permitir camara (para escanear codigo de barras con la webcam)
  mainWindow.webContents.session.setPermissionRequestHandler((wc, permission, callback) => {
    callback(permission === 'media');
  });
  mainWindow.loadFile(path.join(__dirname, '..', 'renderer', 'index.html'));
}

function registrarIpc() {
  ipcMain.handle('productos:list', () => db.productosList());
  ipcMain.handle('producto:save', (_e, p) => db.productoSave(p));
  ipcMain.handle('producto:delete', (_e, id) => db.productoDelete(id));
  ipcMain.handle('precios:all', () => db.preciosAll());
  ipcMain.handle('precio:save', (_e, p) => db.precioSave(p));
  ipcMain.handle('precio:delete', (_e, id) => db.precioDelete(id));
  ipcMain.handle('tiendas:list', () => db.tiendasList());

  ipcMain.handle('alacena:list', () => db.alacenaList());
  ipcMain.handle('alacena:save', (_e, a) => db.alacenaSave(a));

  ipcMain.handle('pin:has', () => config.hasPin());
  ipcMain.handle('pin:verify', (_e, pin) => config.verifyPin(pin));
  ipcMain.handle('pin:set', (_e, pin) => config.setPin(pin));
  ipcMain.handle('pin:clear', () => config.clearPin());

  ipcMain.handle('key:get', () => config.getOpenAiKey());
  ipcMain.handle('key:set', (_e, k) => config.setOpenAiKey(k));

  ipcMain.handle('moneda:get', () => config.getMoneda());
  ipcMain.handle('moneda:set', (_e, code) => config.setMoneda(code));

  ipcMain.handle('open:external', (_e, url) => shell.openExternal(url));

  // OCR: abre dialogo (imagen/PDF/ZIP) y procesa
  ipcMain.handle('ocr:scan', async () => {
    const r = await dialog.showOpenDialog(mainWindow, {
      title: 'Selecciona la factura (imagen, PDF o ZIP)',
      properties: ['openFile'],
      filters: [{ name: 'Facturas', extensions: ['jpg', 'jpeg', 'png', 'webp', 'pdf', 'zip'] }]
    });
    if (r.canceled || !r.filePaths[0]) return { cancelado: true };
    const res = await ocr.run(r.filePaths[0]);
    return { cancelado: false, resultado: res };
  });
  ipcMain.handle('ocr:add', (_e, res) => db.agregarDesdeOcr(res));

  ipcMain.handle('backup:export', async () => {
    const r = await dialog.showSaveDialog(mainWindow, {
      title: 'Exportar copia de seguridad',
      defaultPath: nombreBackup(),
      filters: [{ name: 'ZIP', extensions: ['zip'] }]
    });
    if (r.canceled || !r.filePath) return { ok: false };
    backup.exportar(r.filePath);
    return { ok: true, ruta: r.filePath };
  });
  ipcMain.handle('backup:import', async () => {
    const r = await dialog.showOpenDialog(mainWindow, {
      title: 'Importar copia de seguridad',
      properties: ['openFile'],
      filters: [{ name: 'ZIP', extensions: ['zip'] }]
    });
    if (r.canceled || !r.filePaths[0]) return { ok: false };
    backup.importar(r.filePaths[0]);
    return { ok: true };
  });
}

app.whenReady().then(async () => {
  config.init(app.getPath('userData'));
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
