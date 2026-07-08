const { app, BrowserWindow, ipcMain, dialog } = require('electron');
const path = require('path');
const db = require('./db');
const config = require('./config');
const ocr = require('./ocr');
const backup = require('./backup');

let mainWindow = null;

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
  mainWindow.loadFile(path.join(__dirname, '..', 'renderer', 'index.html'));
}

function registrarIpc() {
  // Productos / precios
  ipcMain.handle('productos:list', () => db.productosList());
  ipcMain.handle('producto:save', (_e, p) => db.productoSave(p));
  ipcMain.handle('producto:delete', (_e, id) => db.productoDelete(id));
  ipcMain.handle('precios:all', () => db.preciosAll());
  ipcMain.handle('precio:save', (_e, p) => db.precioSave(p));
  ipcMain.handle('precio:delete', (_e, id) => db.precioDelete(id));
  ipcMain.handle('tiendas:list', () => db.tiendasList());

  // Alacena
  ipcMain.handle('alacena:list', () => db.alacenaList());
  ipcMain.handle('alacena:save', (_e, a) => db.alacenaSave(a));

  // Seguridad (PIN)
  ipcMain.handle('pin:has', () => config.hasPin());
  ipcMain.handle('pin:verify', (_e, pin) => config.verifyPin(pin));
  ipcMain.handle('pin:set', (_e, pin) => config.setPin(pin));
  ipcMain.handle('pin:clear', () => config.clearPin());

  // OpenAI key
  ipcMain.handle('key:get', () => config.getOpenAiKey());
  ipcMain.handle('key:set', (_e, k) => config.setOpenAiKey(k));

  // OCR: abre dialogo de imagen y procesa
  ipcMain.handle('ocr:scan', async () => {
    const r = await dialog.showOpenDialog(mainWindow, {
      title: 'Selecciona la imagen de la factura',
      properties: ['openFile'],
      filters: [{ name: 'Imagenes', extensions: ['jpg', 'jpeg', 'png', 'webp'] }]
    });
    if (r.canceled || !r.filePaths[0]) return { cancelado: true };
    const res = await ocr.run(r.filePaths[0]);
    return { cancelado: false, resultado: res };
  });
  ipcMain.handle('ocr:add', (_e, res) => db.agregarDesdeOcr(res));

  // Backup
  ipcMain.handle('backup:export', async () => {
    const r = await dialog.showSaveDialog(mainWindow, {
      title: 'Exportar copia de seguridad',
      defaultPath: 'SeguimientoPrecios-backup.zip',
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
