// Import/Export de la base de datos: ZIP con el SQLite + un JSON de respaldo.
const fs = require('fs');
const AdmZip = require('adm-zip');
const db = require('./db');

function exportar(zipPath) {
  const zip = new AdmZip();
  const dbPath = db.getDbPath();
  if (fs.existsSync(dbPath)) {
    zip.addLocalFile(dbPath); // se guarda como "seguimiento.db"
  }
  const respaldo = {
    version: 2,
    productos: db.productosList(),
    precios: db.preciosAll(),
    tiendas: db.tiendasList(),
    alacena: db.alacenaList()
  };
  zip.addFile('backup.json', Buffer.from(JSON.stringify(respaldo, null, 2), 'utf8'));
  zip.writeZip(zipPath);
}

function importar(zipPath) {
  const zip = new AdmZip(zipPath);
  const entry = zip.getEntry('seguimiento.db');
  if (!entry) throw new Error('El ZIP no contiene seguimiento.db');
  fs.writeFileSync(db.getDbPath(), entry.getData());
  db.recargar();
}

module.exports = { exportar, importar };
