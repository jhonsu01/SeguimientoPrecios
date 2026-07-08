// Capa de datos de escritorio: SQLite via sql.js (WASM, sin compilacion nativa).
// El archivo .db se guarda en userData y es 100% compatible con SQLite estandar
// (util para el import/export de la Guia).
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

let SQL = null;
let db = null;
let dbPath = null;

function uuid() {
  return crypto.randomUUID();
}

async function init(userDataPath) {
  const initSqlJs = require('sql.js');
  const sqlJsDir = path.dirname(require.resolve('sql.js')); // .../sql.js/dist
  const wasmBinary = fs.readFileSync(path.join(sqlJsDir, 'sql-wasm.wasm'));
  SQL = await initSqlJs({ wasmBinary });

  dbPath = path.join(userDataPath, 'seguimiento.db');
  if (fs.existsSync(dbPath)) {
    db = new SQL.Database(fs.readFileSync(dbPath));
  } else {
    db = new SQL.Database();
  }
  crearEsquema();
  persistir();
}

function crearEsquema() {
  db.run(`
    CREATE TABLE IF NOT EXISTS productos (
      id TEXT PRIMARY KEY, nombre TEXT NOT NULL, categoria TEXT,
      tipo TEXT, codigoBarras TEXT, unidadMedida TEXT, creadoEn INTEGER
    );
    CREATE TABLE IF NOT EXISTS tiendas (
      id TEXT PRIMARY KEY, nombre TEXT NOT NULL, ubicacion TEXT
    );
    CREATE TABLE IF NOT EXISTS precios (
      id TEXT PRIMARY KEY, productoId TEXT NOT NULL, precio REAL NOT NULL,
      cantidad REAL, tipoPrecio TEXT, tienda TEXT, fecha INTEGER
    );
    CREATE INDEX IF NOT EXISTS idx_precios_producto ON precios(productoId);
  `);
}

function persistir() {
  if (!db || !dbPath) return;
  fs.writeFileSync(dbPath, Buffer.from(db.export()));
}

function all(sql, params = []) {
  const stmt = db.prepare(sql);
  stmt.bind(params);
  const filas = [];
  while (stmt.step()) filas.push(stmt.getAsObject());
  stmt.free();
  return filas;
}

// ---- Productos ----
function productosList() {
  return all('SELECT * FROM productos ORDER BY nombre COLLATE NOCASE ASC');
}

function productoSave(p) {
  const id = p.id || uuid();
  db.run(
    `INSERT INTO productos (id,nombre,categoria,tipo,codigoBarras,unidadMedida,creadoEn)
     VALUES (?,?,?,?,?,?,?)
     ON CONFLICT(id) DO UPDATE SET
       nombre=excluded.nombre, categoria=excluded.categoria, tipo=excluded.tipo,
       codigoBarras=excluded.codigoBarras, unidadMedida=excluded.unidadMedida`,
    [id, p.nombre, p.categoria || 'General', p.tipo || '', p.codigoBarras || null,
     p.unidadMedida || 'unidad', p.creadoEn || Date.now()]
  );
  persistir();
  return id;
}

function productoDelete(id) {
  db.run('DELETE FROM precios WHERE productoId=?', [id]);
  db.run('DELETE FROM productos WHERE id=?', [id]);
  persistir();
}

// ---- Precios ----
function preciosAll() {
  return all('SELECT * FROM precios ORDER BY fecha DESC');
}

function precioSave(p) {
  const id = p.id || uuid();
  db.run(
    `INSERT INTO precios (id,productoId,precio,cantidad,tipoPrecio,tienda,fecha)
     VALUES (?,?,?,?,?,?,?)
     ON CONFLICT(id) DO UPDATE SET
       precio=excluded.precio, cantidad=excluded.cantidad,
       tipoPrecio=excluded.tipoPrecio, tienda=excluded.tienda, fecha=excluded.fecha`,
    [id, p.productoId, p.precio, p.cantidad || 1, p.tipoPrecio || 'unitario',
     p.tienda || '', p.fecha || Date.now()]
  );
  // Alta implicita de tienda
  if (p.tienda && p.tienda.trim()) {
    const existe = all('SELECT 1 FROM tiendas WHERE nombre=? COLLATE NOCASE', [p.tienda.trim()]);
    if (existe.length === 0) {
      db.run('INSERT INTO tiendas (id,nombre,ubicacion) VALUES (?,?,?)',
        [uuid(), p.tienda.trim(), null]);
    }
  }
  persistir();
  return id;
}

function precioDelete(id) {
  db.run('DELETE FROM precios WHERE id=?', [id]);
  persistir();
}

// ---- Tiendas ----
function tiendasList() {
  return all('SELECT * FROM tiendas ORDER BY nombre COLLATE NOCASE ASC');
}

module.exports = {
  init, productosList, productoSave, productoDelete,
  preciosAll, precioSave, precioDelete, tiendasList
};
