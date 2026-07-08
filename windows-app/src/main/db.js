// Capa de datos de escritorio: SQLite via sql.js (WASM, sin compilacion nativa).
// El archivo .db se guarda en userData y es 100% compatible con SQLite estandar.
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

let SQL = null;
let db = null;
let dbPath = null;

function uuid() {
  return crypto.randomUUID();
}

function getDbPath() {
  return dbPath;
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

/** Recarga la BD desde disco (tras un import). */
function recargar() {
  db = new SQL.Database(fs.readFileSync(dbPath));
  crearEsquema();
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
    CREATE TABLE IF NOT EXISTS alacena (
      productoId TEXT PRIMARY KEY, cantidadActual REAL, cantidadMinima REAL
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
  db.run('DELETE FROM alacena WHERE productoId=?', [id]);
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
  registrarTienda(p.tienda);
  persistir();
  return id;
}

function precioDelete(id) {
  db.run('DELETE FROM precios WHERE id=?', [id]);
  persistir();
}

// ---- Tiendas ----
function registrarTienda(nombre) {
  if (nombre && nombre.trim()) {
    const existe = all('SELECT 1 FROM tiendas WHERE nombre=? COLLATE NOCASE', [nombre.trim()]);
    if (existe.length === 0) {
      db.run('INSERT INTO tiendas (id,nombre,ubicacion) VALUES (?,?,?)', [uuid(), nombre.trim(), null]);
    }
  }
}

function tiendasList() {
  return all('SELECT * FROM tiendas ORDER BY nombre COLLATE NOCASE ASC');
}

// ---- Alacena ----
function alacenaList() {
  return all('SELECT * FROM alacena');
}

function alacenaSave(a) {
  db.run(
    `INSERT INTO alacena (productoId,cantidadActual,cantidadMinima) VALUES (?,?,?)
     ON CONFLICT(productoId) DO UPDATE SET
       cantidadActual=excluded.cantidadActual, cantidadMinima=excluded.cantidadMinima`,
    [a.productoId, a.cantidadActual || 0, a.cantidadMinima != null ? a.cantidadMinima : 1]
  );
  persistir();
}

// ---- OCR: alta masiva ----
function agregarDesdeOcr(res) {
  const productos = productosList();
  (res.productos || []).forEach((item) => {
    if (!item.nombre) return;
    let prod = productos.find((x) => x.nombre.toLowerCase() === String(item.nombre).toLowerCase());
    if (!prod) {
      const id = productoSave({
        nombre: item.nombre,
        unidadMedida: item.unidad || 'unidad'
      });
      prod = { id };
      productos.push({ id, nombre: item.nombre });
    }
    precioSave({
      productoId: prod.id,
      precio: Number(item.precio) || 0,
      cantidad: Number(item.cantidad) || 1,
      tienda: res.tienda || ''
    });
  });
}

// ---- Restaurar desde un respaldo JSON (import multiplataforma) ----
function restoreFromJson(data) {
  db.run('DELETE FROM precios');
  db.run('DELETE FROM alacena');
  db.run('DELETE FROM productos');
  db.run('DELETE FROM tiendas');
  (data.productos || []).forEach((p) => {
    db.run(
      'INSERT OR REPLACE INTO productos (id,nombre,categoria,tipo,codigoBarras,unidadMedida,creadoEn) VALUES (?,?,?,?,?,?,?)',
      [p.id, p.nombre, p.categoria || 'General', p.tipo || '', p.codigoBarras || null, p.unidadMedida || 'unidad', p.creadoEn || Date.now()]
    );
  });
  (data.tiendas || []).forEach((t) => {
    db.run('INSERT OR REPLACE INTO tiendas (id,nombre,ubicacion) VALUES (?,?,?)',
      [t.id, t.nombre, t.ubicacion || null]);
  });
  (data.precios || []).forEach((p) => {
    db.run(
      'INSERT OR REPLACE INTO precios (id,productoId,precio,cantidad,tipoPrecio,tienda,fecha) VALUES (?,?,?,?,?,?,?)',
      [p.id, p.productoId, p.precio, p.cantidad || 1, p.tipoPrecio || 'unitario', p.tienda || '', p.fecha || Date.now()]
    );
  });
  (data.alacena || []).forEach((a) => {
    db.run('INSERT OR REPLACE INTO alacena (productoId,cantidadActual,cantidadMinima) VALUES (?,?,?)',
      [a.productoId, a.cantidadActual || 0, a.cantidadMinima != null ? a.cantidadMinima : 1]);
  });
  persistir();
}

module.exports = {
  init, recargar, getDbPath,
  productosList, productoSave, productoDelete,
  preciosAll, precioSave, precioDelete, tiendasList,
  alacenaList, alacenaSave, agregarDesdeOcr, restoreFromJson
};
