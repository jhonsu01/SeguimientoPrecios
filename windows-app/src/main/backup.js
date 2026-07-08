// Import/Export de la base de datos: ZIP con el SQLite + un JSON de respaldo.
// El import usa SIEMPRE backup.json (portable) para funcionar entre Windows y Android.
const fs = require('fs');
const AdmZip = require('adm-zip');
const db = require('./db');

function exportar(zipPath) {
  const zip = new AdmZip();
  const respaldo = {
    version: 3,
    app: 'SeguimientoPrecios',
    productos: db.productosList(),
    precios: db.preciosAll(),
    tiendas: db.tiendasList(),
    alacena: db.alacenaList()
  };
  zip.addFile('backup.json', Buffer.from(JSON.stringify(respaldo, null, 2), 'utf8'));
  const dbPath = db.getDbPath();
  if (fs.existsSync(dbPath)) zip.addLocalFile(dbPath); // "seguimiento.db" (bonus, mismo equipo)
  zip.writeZip(zipPath);
}

function importar(zipPath) {
  const zip = new AdmZip(zipPath);
  const entry = zip.getEntry('backup.json');
  if (!entry) throw new Error('El ZIP no contiene backup.json (respaldo no valido).');
  const data = JSON.parse(entry.getData().toString('utf8'));
  db.restoreFromJson(data);
}

module.exports = { exportar, importar };
