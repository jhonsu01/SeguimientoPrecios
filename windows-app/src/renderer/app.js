'use strict';

const state = {
  productos: [], precios: [], tiendas: [], alacena: [],
  view: 'dashboard', detalleId: null
};

const UNIDADES = ['unidad', 'ml', 'L', 'g', 'kg', 'lb'];
const TIPOS = ['unitario', 'promocion'];
const CATEGORIAS = ['General', 'Alimentos', 'Bebidas', 'Aseo', 'Hogar', 'Tecnologia', 'Otros'];

const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));

// Fallback SOLO para previsualizacion en navegador (sin Electron).
if (!window.api) {
  const D = 86400000;
  const mem = {
    productos: [
      { id: 'p1', nombre: 'Leche entera 1L', categoria: 'Bebidas', tipo: 'Alqueria', codigoBarras: null, unidadMedida: 'L', creadoEn: Date.now() },
      { id: 'p2', nombre: 'Arroz Diana', categoria: 'Alimentos', tipo: '', codigoBarras: null, unidadMedida: 'kg', creadoEn: Date.now() }
    ],
    precios: [
      { id: 'x1', productoId: 'p1', precio: 3800, cantidad: 1, tipoPrecio: 'unitario', tienda: 'Exito', fecha: Date.now() - D * 20 },
      { id: 'x2', productoId: 'p1', precio: 4100, cantidad: 1, tipoPrecio: 'unitario', tienda: 'Exito', fecha: Date.now() - D * 10 },
      { id: 'x3', productoId: 'p1', precio: 3950, cantidad: 1, tipoPrecio: 'promocion', tienda: 'D1', fecha: Date.now() - D * 3 },
      { id: 'x4', productoId: 'p2', precio: 5200, cantidad: 1, tipoPrecio: 'unitario', tienda: 'D1', fecha: Date.now() - D * 15 },
      { id: 'x5', productoId: 'p2', precio: 4900, cantidad: 1, tipoPrecio: 'promocion', tienda: 'Ara', fecha: Date.now() - D * 2 }
    ],
    tiendas: [{ id: 't1', nombre: 'Exito' }, { id: 't2', nombre: 'D1' }, { id: 't3', nombre: 'Ara' }],
    alacena: [{ productoId: 'p1', cantidadActual: 0, cantidadMinima: 1 }]
  };
  const uid = () => 'id' + Math.random().toString(36).slice(2);
  window.api = {
    productosList: async () => mem.productos,
    productoSave: async (p) => { if (!p.id) { p.id = uid(); mem.productos.push(p); } else { Object.assign(mem.productos.find(x => x.id === p.id), p); } return p.id; },
    productoDelete: async (id) => { mem.productos = mem.productos.filter(x => x.id !== id); mem.precios = mem.precios.filter(x => x.productoId !== id); mem.alacena = mem.alacena.filter(x => x.productoId !== id); },
    preciosAll: async () => mem.precios,
    precioSave: async (p) => { p.id = p.id || uid(); mem.precios.push(p); if (p.tienda && !mem.tiendas.find(t => t.nombre.toLowerCase() === p.tienda.toLowerCase())) mem.tiendas.push({ id: uid(), nombre: p.tienda }); return p.id; },
    precioDelete: async (id) => { mem.precios = mem.precios.filter(x => x.id !== id); },
    tiendasList: async () => mem.tiendas,
    alacenaList: async () => mem.alacena,
    alacenaSave: async (a) => { const i = mem.alacena.findIndex(x => x.productoId === a.productoId); if (i >= 0) mem.alacena[i] = a; else mem.alacena.push(a); },
    pinHas: async () => false,
    pinVerify: async () => true,
    pinSet: async () => {}, pinClear: async () => {},
    keyGet: async () => '', keySet: async () => {},
    monedaGet: async () => 'COP', monedaSet: async () => {},
    openExternal: async (url) => { window.open(url, '_blank'); },
    ocrScan: async () => ({ cancelado: false, resultado: { tienda: 'Demo Market', productos: [{ nombre: 'Pan tajado', precio: 5200, cantidad: 1, unidad: 'unidad' }, { nombre: 'Huevos AA x12', precio: 12500, cantidad: 1, unidad: 'unidad' }] } }),
    ocrAdd: async (res, sumar) => {
      (res.productos || []).forEach(it => {
        let prod = mem.productos.find(p => p.nombre.toLowerCase() === String(it.nombre).toLowerCase());
        if (!prod) { prod = { id: uid(), nombre: it.nombre, categoria: 'General', unidadMedida: it.unidad || 'unidad' }; mem.productos.push(prod); }
        const cant = it.cantidad || 1;
        mem.precios.push({ id: uid(), productoId: prod.id, precio: it.precio, cantidad: cant, tipoPrecio: 'unitario', tienda: res.tienda || '', fecha: Date.now() });
        if (sumar) { let a = mem.alacena.find(x => x.productoId === prod.id); if (!a) { a = { productoId: prod.id, cantidadActual: 0, cantidadMinima: 1 }; mem.alacena.push(a); } a.cantidadActual += cant; }
      });
    },
    backupExport: async () => ({ ok: true, ruta: 'demo-backup.zip' }),
    backupImport: async () => ({ ok: true })
  };
}

// ---------- Helpers ----------
function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
const MONEDAS = [
  { code: 'COP', nombre: 'Peso colombiano', symbol: '$', decimals: 0, miles: '.', decimal: ',' },
  { code: 'USD', nombre: 'Dolar', symbol: '$', decimals: 2, miles: ',', decimal: '.' },
  { code: 'EUR', nombre: 'Euro', symbol: '€', decimals: 2, miles: '.', decimal: ',' },
  { code: 'MXN', nombre: 'Peso mexicano', symbol: '$', decimals: 2, miles: ',', decimal: '.' },
  { code: 'ARS', nombre: 'Peso argentino', symbol: '$', decimals: 2, miles: '.', decimal: ',' },
  { code: 'CLP', nombre: 'Peso chileno', symbol: '$', decimals: 0, miles: '.', decimal: ',' },
  { code: 'PEN', nombre: 'Sol peruano', symbol: 'S/', decimals: 2, miles: ',', decimal: '.' },
  { code: 'BRL', nombre: 'Real brasileno', symbol: 'R$', decimals: 2, miles: '.', decimal: ',' },
  { code: 'GBP', nombre: 'Libra', symbol: '£', decimals: 2, miles: ',', decimal: '.' },
  { code: 'JPY', nombre: 'Yen', symbol: '¥', decimals: 0, miles: ',', decimal: '.' }
];
let CURRENCY = MONEDAS[0];
function setCurrency(code) { CURRENCY = MONEDAS.find(m => m.code === code) || MONEDAS[0]; }

function moneda(v) {
  const d = CURRENCY;
  const neg = v < 0;
  const abs = Math.abs(Number(v) || 0);
  const factor = Math.pow(10, d.decimals);
  const round = Math.round(abs * factor) / factor;
  const entero = Math.floor(round);
  const frac = Math.round((round - entero) * factor);
  const enteroStr = String(entero).replace(/\B(?=(\d{3})+(?!\d))/g, d.miles);
  let s = (neg ? '-' : '') + d.symbol + enteroStr;
  if (d.decimals > 0) s += d.decimal + String(frac).padStart(d.decimals, '0');
  return s;
}
function fmtNum(v) { return Number.isInteger(v) ? String(v) : String(v); }
function fecha(ms) { return new Date(ms).toLocaleDateString(); }
function fechaHora(ms) {
  const d = new Date(ms);
  return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}
function preciosDe(id) { return state.precios.filter(p => p.productoId === id); }

function tendenciaDe(precios) {
  if (precios.length < 2) return { t: 'none', pct: 0 };
  const ord = [...precios].sort((a, b) => a.fecha - b.fecha);
  const prev = ord[ord.length - 2].precio;
  const act = ord[ord.length - 1].precio;
  if (prev === 0) return { t: 'none', pct: 0 };
  const pct = (act - prev) / prev * 100;
  if (pct > 0.5) return { t: 'up', pct };
  if (pct < -0.5) return { t: 'down', pct };
  return { t: 'flat', pct };
}
function chipHtml(precios) {
  const { t, pct } = tendenciaDe(precios);
  const map = {
    up: ['up', '&#9650; +' + Math.abs(pct).toFixed(1) + '%'],
    down: ['down', '&#9660; -' + Math.abs(pct).toFixed(1) + '%'],
    flat: ['flat', '&#9644; estable'],
    none: ['none', '&#8212;']
  };
  const [cls, txt] = map[t];
  return `<span class="chip ${cls}">${txt}</span>`;
}
function prediccion(precios) {
  if (precios.length < 2) return null;
  const ord = [...precios].sort((a, b) => a.fecha - b.fecha);
  const base = ord[0].fecha;
  const xs = ord.map(p => (p.fecha - base) / 86400000);
  const ys = ord.map(p => p.precio);
  const n = xs.length;
  const mx = xs.reduce((a, b) => a + b, 0) / n;
  const my = ys.reduce((a, b) => a + b, 0) / n;
  let num = 0, den = 0;
  for (let i = 0; i < n; i++) { num += (xs[i] - mx) * (ys[i] - my); den += (xs[i] - mx) ** 2; }
  const slope = den === 0 ? 0 : num / den;
  const inter = my - slope * mx;
  const pred = Math.max(0, inter + slope * (xs[n - 1] + 7));
  return { pred, slope };
}
function compararTiendas(precios) {
  const map = {};
  precios.filter(p => p.tienda).forEach(p => {
    if (!map[p.tienda] || p.fecha > map[p.tienda].fecha) map[p.tienda] = { precio: p.precio, fecha: p.fecha };
  });
  return Object.entries(map).map(([t, v]) => ({ tienda: t, precio: v.precio })).sort((a, b) => a.precio - b.precio);
}

// ---------- Carga / arranque ----------
async function cargar() {
  const [productos, precios, tiendas, alacena] = await Promise.all([
    window.api.productosList(), window.api.preciosAll(),
    window.api.tiendasList(), window.api.alacenaList()
  ]);
  state.productos = productos;
  state.precios = precios;
  state.tiendas = tiendas;
  state.alacena = alacena;
  render();
}

async function initApp() {
  try { setCurrency(await window.api.monedaGet()); } catch (e) { /* usa COP por defecto */ }
  const tienePin = await window.api.pinHas();
  if (tienePin) mostrarLock();
  else await cargar();
}

function mostrarLock() {
  const lock = $('#lock-screen');
  lock.classList.remove('hidden');
  lock.innerHTML = `
    <h2>🔒 Seguimiento de Precios</h2>
    <div class="lock-sub">Ingresa tu PIN</div>
    <input id="lock-pin" type="password" inputmode="numeric" maxlength="8" autofocus />
    <div class="err" id="lock-err"></div>
    <button class="btn btn-primary" id="lock-btn">Desbloquear</button>`;
  const intentar = async () => {
    const ok = await window.api.pinVerify($('#lock-pin').value);
    if (ok) { lock.classList.add('hidden'); await cargar(); }
    else { $('#lock-err').textContent = 'PIN incorrecto'; $('#lock-pin').value = ''; }
  };
  $('#lock-btn').addEventListener('click', intentar);
  $('#lock-pin').addEventListener('keydown', e => { if (e.key === 'Enter') intentar(); });
  setTimeout(() => $('#lock-pin').focus(), 50);
}

function go(view, detalleId = null) {
  state.view = view;
  state.detalleId = detalleId;
  $$('.tab').forEach(b => b.classList.toggle('active', b.dataset.view === view));
  render();
}

// ---------- Render ----------
function render() {
  const c = $('#content');
  if (state.view === 'dashboard') c.innerHTML = viewDashboard();
  else if (state.view === 'productos') c.innerHTML = viewProductos();
  else if (state.view === 'alacena') c.innerHTML = viewAlacena();
  else if (state.view === 'graficos') c.innerHTML = viewGraficos();
  else if (state.view === 'ajustes') c.innerHTML = viewAjustes();
  else if (state.view === 'detalle') c.innerHTML = viewDetalle();
  attach();
}

function viewDashboard() {
  const filas = state.productos.map(p => {
    const pp = preciosDe(p.id);
    const ultimo = pp.length ? pp.reduce((a, b) => a.fecha > b.fecha ? a : b) : null;
    return `<div class="list-item" data-open="${p.id}">
      <div class="grow">
        <div class="item-name">${esc(p.nombre)}</div>
        <div class="item-sub">${esc(p.categoria)} &middot; ${esc(p.unidadMedida)}</div>
      </div>
      <div>
        <div class="item-price">${ultimo ? moneda(ultimo.precio) : '&mdash;'}</div>
        <div style="text-align:right;margin-top:4px">${chipHtml(pp)}</div>
      </div>
    </div>`;
  }).join('');

  return `
    <h1 class="page-title">Resumen</h1>
    <p class="page-sub">Comparador de precios personal y gestor de gastos.</p>
    <div class="stat-row">
      <div class="stat"><div class="value">${state.productos.length}</div><div class="label">Productos</div></div>
      <div class="stat"><div class="value">${state.precios.length}</div><div class="label">Registros de precio</div></div>
      <div class="stat"><div class="value">${state.tiendas.length}</div><div class="label">Tiendas</div></div>
    </div>
    <div class="section-title">Productos rastreados</div>
    ${state.productos.length ? filas : '<div class="empty">Aun no hay productos. Ve a la pestana Productos.</div>'}
  `;
}

function viewProductos() {
  const filas = state.productos.map(p => `
    <div class="list-item" data-open="${p.id}" data-nombre="${esc(p.nombre.toLowerCase())}">
      <div class="grow">
        <div class="item-name">${esc(p.nombre)}</div>
        <div class="item-sub">${esc(p.categoria)} &middot; ${esc(p.unidadMedida)}${p.tipo ? ' &middot; ' + esc(p.tipo) : ''}</div>
      </div>
      <button class="icon-btn edit" data-edit="${p.id}" title="Editar">&#9998;</button>
      <button class="icon-btn danger" data-del="${p.id}" title="Eliminar">&#128465;</button>
    </div>`).join('');

  return `
    <div class="header-row">
      <div><h1 class="page-title">Productos</h1></div>
      <div>
        <button class="btn btn-ghost" id="scan-factura">Escanear factura</button>
        <button class="btn btn-primary" id="nuevo-producto">+ Nuevo producto</button>
      </div>
    </div>
    ${state.productos.length ? '<input id="buscar-prod" class="buscador" placeholder="Buscar producto..." />' : ''}
    ${state.productos.length ? filas : '<div class="empty">Toca "+ Nuevo producto" o escanea una factura.</div>'}
  `;
}

function viewAlacena() {
  const itemDe = (p) => state.alacena.find(a => a.productoId === p.id) || { productoId: p.id, cantidadActual: 0, cantidadMinima: 1 };
  const bajo = state.productos.filter(p => { const a = itemDe(p); return a.cantidadActual <= a.cantidadMinima; });
  const filas = state.productos.map(p => {
    const a = itemDe(p);
    const low = a.cantidadActual <= a.cantidadMinima;
    return `<div class="list-item" style="cursor:default" data-nombre="${esc(p.nombre.toLowerCase())}">
      <div class="grow">
        <div class="item-name">${esc(p.nombre)}</div>
        <div class="item-sub ${low ? 'low' : 'ok'}">Stock: ${fmtNum(a.cantidadActual)} &middot; min: ${fmtNum(a.cantidadMinima)} ${esc(p.unidadMedida)}</div>
      </div>
      <div class="stepper">
        <button class="icon-btn" data-stock="${p.id}" data-delta="-1">&minus;</button>
        <span class="count">${fmtNum(a.cantidadActual)}</span>
        <button class="icon-btn" data-stock="${p.id}" data-delta="1">+</button>
        <button class="icon-btn edit" data-alacena="${p.id}" title="Editar minimo">&#9998;</button>
      </div>
    </div>`;
  }).join('');

  return `
    <h1 class="page-title">Mi Alacena</h1>
    ${bajo.length ? `<div class="card shopping"><div class="title">&#128722; Lista de compras (${bajo.length})</div><div style="margin-top:6px">${bajo.map(p => esc(p.nombre)).join(', ')}</div></div>` : ''}
    <div class="section-title">Inventario</div>
    ${state.productos.length ? '<input id="buscar-prod" class="buscador" placeholder="Buscar producto..." />' : ''}
    ${state.productos.length ? filas : '<div class="empty">Agrega productos para gestionar tu inventario.</div>'}
  `;
}

function viewDetalle() {
  const p = state.productos.find(x => x.id === state.detalleId);
  if (!p) return '<div class="empty">Producto no encontrado.</div>';
  const pp = preciosDe(p.id);
  const registros = [...pp].sort((a, b) => b.fecha - a.fecha).map(pr => `
    <div class="list-item" style="cursor:default">
      <div class="grow">
        <div class="item-price" style="text-align:left">${moneda(pr.precio)}</div>
        <div class="item-sub">${pr.tienda ? esc(pr.tienda) : 'Sin tienda'} &middot; ${esc(pr.tipoPrecio)}${pr.cantidad != 1 ? ' &middot; x' + pr.cantidad : ''}</div>
      </div>
      <div class="item-sub">${fechaHora(pr.fecha)}</div>
      <button class="icon-btn danger" data-delprecio="${pr.id}" title="Eliminar">&#128465;</button>
    </div>`).join('');

  const stats = pp.length ? `
    <div class="stat-row">
      <div class="stat"><div class="value">${moneda(Math.min(...pp.map(x => x.precio)))}</div><div class="label">Minimo</div></div>
      <div class="stat"><div class="value">${moneda(pp.reduce((s, x) => s + x.precio, 0) / pp.length)}</div><div class="label">Promedio</div></div>
      <div class="stat"><div class="value">${moneda(Math.max(...pp.map(x => x.precio)))}</div><div class="label">Maximo</div></div>
    </div>` : '';

  return `
    <div class="header-row">
      <div>
        <button class="btn btn-ghost" id="volver">&larr; Volver</button>
        <h1 class="page-title" style="display:inline-block;margin-left:8px">${esc(p.nombre)}</h1>
        <span class="item-sub">${esc(p.categoria)} &middot; ${esc(p.unidadMedida)}</span>
      </div>
      <button class="btn btn-primary" id="nuevo-precio">+ Registrar precio</button>
    </div>
    <div class="card">
      <div class="section-title">Evolucion del precio ${chipHtml(pp)}</div>
      <canvas id="chart"></canvas>
    </div>
    ${stats}
    <div class="section-title">Registros (${pp.length})</div>
    ${pp.length ? registros : '<div class="empty">Sin precios. Toca "+ Registrar precio".</div>'}
  `;
}

function viewGraficos() {
  if (!state.productos.length) {
    return '<h1 class="page-title">Graficos</h1><div class="empty">Agrega productos y precios para ver graficos.</div>';
  }
  const sel = state.detalleId || state.productos[0].id;
  const p = state.productos.find(x => x.id === sel) || state.productos[0];
  const pp = preciosDe(p.id);
  const options = state.productos.map(x =>
    `<option value="${x.id}" ${x.id === p.id ? 'selected' : ''}>${esc(x.nombre)}</option>`).join('');

  let extra = '';
  if (pp.length) {
    extra += `
      <div class="stat-row">
        <div class="stat"><div class="value">${moneda(Math.min(...pp.map(x => x.precio)))}</div><div class="label">Minimo</div></div>
        <div class="stat"><div class="value">${moneda(pp.reduce((s, x) => s + x.precio, 0) / pp.length)}</div><div class="label">Promedio</div></div>
        <div class="stat"><div class="value">${moneda(Math.max(...pp.map(x => x.precio)))}</div><div class="label">Maximo</div></div>
      </div>`;
    const pr = prediccion(pp);
    if (pr) {
      const up = pr.slope > 0;
      extra += `<div class="card">
        <div class="section-title">Prediccion (IA, ~7 dias)</div>
        <div class="pred-value ${up ? 'up' : 'down'}">&asymp; ${moneda(pr.pred)}</div>
        <div class="item-sub">${up ? 'Tendencia al alza' : 'Tendencia a la baja'} segun regresion lineal</div>
      </div>`;
    }
    const rank = compararTiendas(pp);
    if (rank.length) {
      extra += `<div class="card">
        <div class="section-title">Comparacion por tienda</div>
        ${rank.map((r, i) => `<div class="rank-row ${i === 0 ? 'best' : ''}"><span class="rank-name">${i === 0 ? '&#127942; ' : ''}${esc(r.tienda)}</span><span class="rank-price">${moneda(r.precio)}</span></div>`).join('')}
      </div>`;
    }
  } else {
    extra = '<div class="empty">Este producto aun no tiene precios.</div>';
  }

  return `
    <h1 class="page-title">Graficos</h1>
    <p class="page-sub">Evolucion, prediccion y comparacion de tiendas.</p>
    <label>Producto</label>
    <select id="sel-producto" style="max-width:360px">${options}</select>
    <div class="card" style="margin-top:16px">
      <div class="section-title">Variacion reciente ${chipHtml(pp)}</div>
      <canvas id="chart"></canvas>
    </div>
    ${extra}
  `;
}

function viewAjustes() {
  return `
    <h1 class="page-title">Ajustes</h1>
    <div class="card setting-block">
      <h3>OCR de facturas (OpenAI)</h3>
      <div class="setting-desc">Pega tu API key de OpenAI. Se guarda solo en este equipo y se usa para leer facturas.</div>
      <input id="aj-key" type="password" placeholder="sk-..." />
      <div style="margin-top:10px">
        <button class="btn btn-primary" id="aj-key-save">Guardar key</button>
        <span class="saved-ok" id="aj-key-ok" style="display:none">Guardada &#10003;</span>
      </div>
    </div>
    <div class="card setting-block">
      <h3>Moneda</h3>
      <div class="setting-desc">Selecciona la moneda para mostrar los precios (por defecto COP).</div>
      <select id="aj-moneda" style="max-width:320px">${MONEDAS.map(m => `<option value="${m.code}" ${m.code === CURRENCY.code ? 'selected' : ''}>${m.code} - ${m.nombre}</option>`).join('')}</select>
    </div>
    <div class="card setting-block">
      <h3>Seguridad (PIN)</h3>
      <div class="setting-desc" id="aj-pin-desc">Protege el acceso con un PIN (hash SHA-256).</div>
      <div id="aj-pin-actions"></div>
    </div>
    <div class="card setting-block">
      <h3>Copia de seguridad</h3>
      <div class="setting-desc">Exporta o restaura toda tu base de datos (ZIP con SQLite + JSON).</div>
      <button class="btn btn-primary" id="aj-export">Exportar ZIP</button>
      <button class="btn btn-ghost" id="aj-import">Importar ZIP</button>
      <div class="saved-ok" id="aj-backup-msg" style="display:none"></div>
    </div>
    <div class="card setting-block">
      <h3>Acerca de</h3>
      <div class="setting-desc">Seguimiento de Precios &middot; modo oscuro &middot; offline-first</div>
      <div style="margin-top:12px;display:flex;gap:10px;flex-wrap:wrap">
        <button class="btn kofi" id="aj-kofi">&#9749; Apoyame en Ko-fi</button>
        <button class="btn btn-ghost" id="aj-repo">Repositorio en GitHub</button>
      </div>
    </div>
  `;
}

// ---------- Listeners ----------
function attach() {
  $$('[data-open]').forEach(el => el.addEventListener('click', () => go('detalle', el.dataset.open)));
  $$('[data-edit]').forEach(el => el.addEventListener('click', (e) => {
    e.stopPropagation();
    modalProducto(state.productos.find(p => p.id === el.dataset.edit));
  }));
  $$('[data-del]').forEach(el => el.addEventListener('click', async (e) => {
    e.stopPropagation();
    await window.api.productoDelete(el.dataset.del);
    await cargar();
  }));
  $$('[data-delprecio]').forEach(el => el.addEventListener('click', async () => {
    await window.api.precioDelete(el.dataset.delprecio);
    await cargar();
  }));
  $$('[data-stock]').forEach(el => el.addEventListener('click', () => ajustarStock(el.dataset.stock, Number(el.dataset.delta))));
  $$('[data-alacena]').forEach(el => el.addEventListener('click', () => {
    modalAlacena(state.productos.find(p => p.id === el.dataset.alacena));
  }));

  const nuevoProd = $('#nuevo-producto');
  if (nuevoProd) nuevoProd.addEventListener('click', () => modalProducto(null));
  const scan = $('#scan-factura');
  if (scan) scan.addEventListener('click', runOcr);
  const volver = $('#volver');
  if (volver) volver.addEventListener('click', () => go('productos'));
  const nuevoPrecio = $('#nuevo-precio');
  if (nuevoPrecio) nuevoPrecio.addEventListener('click', () => modalPrecio(state.detalleId));
  const selP = $('#sel-producto');
  if (selP) selP.addEventListener('change', () => { state.detalleId = selP.value; render(); });

  const buscar = $('#buscar-prod');
  if (buscar) buscar.addEventListener('input', () => {
    const q = buscar.value.toLowerCase();
    $$('#content .list-item[data-nombre]').forEach((el) => {
      el.style.display = el.dataset.nombre.includes(q) ? '' : 'none';
    });
  });

  if (state.view === 'ajustes') attachAjustes();

  const canvas = $('#chart');
  if (canvas) {
    const id = state.detalleId || (state.productos[0] && state.productos[0].id);
    const pp = id ? preciosDe(id) : [];
    requestAnimationFrame(() => drawChart(canvas, pp));
  }
}

async function attachAjustes() {
  $('#aj-key').value = await window.api.keyGet();
  $('#aj-key-save').addEventListener('click', async () => {
    await window.api.keySet($('#aj-key').value);
    const ok = $('#aj-key-ok'); ok.style.display = 'inline';
  });

  const tienePin = await window.api.pinHas();
  const acciones = $('#aj-pin-actions');
  const desc = $('#aj-pin-desc');
  if (tienePin) {
    desc.textContent = 'El acceso a la app esta protegido con PIN.';
    acciones.innerHTML = '<button class="btn btn-ghost" id="aj-pin-clear">Quitar PIN</button>';
    $('#aj-pin-clear').addEventListener('click', async () => { await window.api.pinClear(); render(); });
  } else {
    acciones.innerHTML = '<button class="btn btn-primary" id="aj-pin-set">Definir PIN</button>';
    $('#aj-pin-set').addEventListener('click', () => modalPin());
  }

  $('#aj-export').addEventListener('click', async () => {
    const r = await window.api.backupExport();
    const msg = $('#aj-backup-msg');
    if (r.ok) { msg.textContent = 'Exportado' + (r.ruta ? ': ' + r.ruta : ''); msg.style.display = 'block'; }
  });
  $('#aj-import').addEventListener('click', async () => {
    const r = await window.api.backupImport();
    if (r.ok) { await cargar(); go('ajustes'); const msg = $('#aj-backup-msg'); if (msg) { msg.textContent = 'Datos importados'; msg.style.display = 'block'; } }
  });

  const selMoneda = $('#aj-moneda');
  if (selMoneda) selMoneda.addEventListener('change', async () => {
    await window.api.monedaSet(selMoneda.value);
    setCurrency(selMoneda.value);
    go('ajustes');
  });
  const kofi = $('#aj-kofi');
  if (kofi) kofi.addEventListener('click', () => window.api.openExternal('https://ko-fi.com/V7V81LV7GX'));
  const repo = $('#aj-repo');
  if (repo) repo.addEventListener('click', () => window.api.openExternal('https://github.com/jhonsu01/SeguimientoPrecios'));
}

async function ajustarStock(id, delta) {
  const a = state.alacena.find(x => x.productoId === id) || { productoId: id, cantidadActual: 0, cantidadMinima: 1 };
  const nuevo = Math.max(0, a.cantidadActual + delta);
  await window.api.alacenaSave({ productoId: id, cantidadActual: nuevo, cantidadMinima: a.cantidadMinima });
  await cargar();
}

async function runOcr() {
  openModal('<h3>OCR de factura</h3><div style="padding:8px 0">Selecciona la imagen y espera el analisis...</div>');
  try {
    const r = await window.api.ocrScan();
    closeModal();
    if (r && r.cancelado) return;
    modalOcr(r.resultado);
  } catch (e) {
    closeModal();
    modalMensaje('OCR de factura', String((e && e.message) || e));
  }
}

// ---------- Grafico (canvas) ----------
function drawChart(canvas, precios) {
  const dpr = window.devicePixelRatio || 1;
  const W = canvas.clientWidth || 600, H = 220;
  canvas.width = W * dpr;
  canvas.height = H * dpr;
  const ctx = canvas.getContext('2d');
  ctx.scale(dpr, dpr);
  ctx.clearRect(0, 0, W, H);

  const pts = [...precios].sort((a, b) => a.fecha - b.fecha);
  if (pts.length === 0) {
    ctx.fillStyle = '#94A3B8';
    ctx.font = 'italic 13px Segoe UI';
    ctx.textAlign = 'center';
    ctx.fillText('Sin datos de precios todavia', W / 2, H / 2);
    return;
  }
  const vals = pts.map(p => p.precio);
  const minV = Math.min(...vals), maxV = Math.max(...vals);
  const rango = (maxV - minV) || 1;
  const L = 72, R = 14, T = 14, B = 34;
  const w = W - L - R, h = H - T - B;

  ctx.font = '10px Segoe UI';
  ctx.textAlign = 'left';
  const rows = 4;
  for (let i = 0; i <= rows; i++) {
    const y = T + h * i / rows;
    ctx.strokeStyle = 'rgba(51,65,85,.7)';
    ctx.lineWidth = 1;
    ctx.setLineDash([5, 7]);
    ctx.beginPath();
    ctx.moveTo(L, y);
    ctx.lineTo(L + w, y);
    ctx.stroke();
    ctx.setLineDash([]);
    ctx.fillStyle = '#94A3B8';
    ctx.fillText(moneda(maxV - rango * i / rows), 6, y + 3);
  }

  const px = i => pts.length === 1 ? L + w / 2 : L + w * i / (pts.length - 1);
  const py = v => T + (h - h * ((v - minV) / rango));

  if (pts.length >= 2) {
    const grad = ctx.createLinearGradient(0, T, 0, T + h);
    grad.addColorStop(0, 'rgba(52,211,153,.28)');
    grad.addColorStop(1, 'rgba(52,211,153,.02)');
    ctx.beginPath();
    ctx.moveTo(px(0), py(pts[0].precio));
    pts.forEach((p, i) => ctx.lineTo(px(i), py(p.precio)));
    ctx.lineTo(px(pts.length - 1), T + h);
    ctx.lineTo(px(0), T + h);
    ctx.closePath();
    ctx.fillStyle = grad;
    ctx.fill();

    ctx.beginPath();
    ctx.moveTo(px(0), py(pts[0].precio));
    pts.forEach((p, i) => ctx.lineTo(px(i), py(p.precio)));
    ctx.strokeStyle = '#34D399';
    ctx.lineWidth = 2.5;
    ctx.stroke();
  }

  pts.forEach((p, i) => {
    ctx.beginPath();
    ctx.arc(px(i), py(p.precio), 4, 0, Math.PI * 2);
    ctx.fillStyle = '#14B8A6';
    ctx.fill();
    ctx.beginPath();
    ctx.arc(px(i), py(p.precio), 1.6, 0, Math.PI * 2);
    ctx.fillStyle = '#fff';
    ctx.fill();
  });

  // Fechas de cada registro en el eje X (max ~5 etiquetas)
  ctx.fillStyle = '#94A3B8';
  ctx.textAlign = 'center';
  ctx.font = '9px Segoe UI';
  const fmtF = (ms) => { const d = new Date(ms); return String(d.getDate()).padStart(2, '0') + '/' + String(d.getMonth() + 1).padStart(2, '0'); };
  const maxLabels = 5;
  const paso = pts.length <= maxLabels ? 1 : Math.ceil(pts.length / maxLabels);
  pts.forEach((p, i) => {
    if (i % paso === 0 || i === pts.length - 1) ctx.fillText(fmtF(p.fecha), px(i), H - 8);
  });
  ctx.textAlign = 'left';
}

// ---------- Modales ----------
function openModal(html) {
  $('#modal').innerHTML = html;
  $('#modal-overlay').classList.remove('hidden');
}
function closeModal() {
  $('#modal-overlay').classList.add('hidden');
  $('#modal').innerHTML = '';
}
function modalMensaje(titulo, msg) {
  openModal(`<h3>${esc(titulo)}</h3><div style="padding:8px 0">${esc(msg)}</div>
    <div class="modal-actions"><button class="btn btn-primary" id="m-ok">Cerrar</button></div>`);
  $('#m-ok').addEventListener('click', closeModal);
}

let scanReader = null;
async function openCameraScan(onResult) {
  if (typeof ZXing === 'undefined') {
    modalMensaje('Escaner', 'La libreria de escaneo no esta disponible.');
    return;
  }
  const ov = $('#scan-overlay');
  const box = $('#scan-modal');
  box.innerHTML = `
    <h3>Escanear codigo de barras</h3>
    <label>Camara</label>
    <select id="scan-cam"><option value="">Camara predeterminada</option></select>
    <video id="scan-video" muted autoplay playsinline style="margin-top:8px"></video>
    <div class="setting-desc" id="scan-msg" style="margin-top:8px">Apunta la camara al codigo de barras...</div>
    <div class="modal-actions"><button class="btn btn-ghost" id="scan-cancel">Cerrar</button></div>`;
  ov.classList.remove('hidden');
  const cerrar = () => {
    if (scanReader) { try { scanReader.reset(); } catch (e) {} scanReader = null; }
    ov.classList.add('hidden');
    box.innerHTML = '';
  };
  $('#scan-cancel').addEventListener('click', cerrar);
  const cb = (result) => { if (result) { const t = result.getText(); cerrar(); onResult(t); } };
  const iniciar = (deviceId) => {
    try {
      if (scanReader) { try { scanReader.reset(); } catch (e) {} }
      scanReader = new ZXing.BrowserMultiFormatReader();
      scanReader.decodeFromVideoDevice(deviceId || undefined, 'scan-video', cb);
    } catch (e) {
      const m = $('#scan-msg');
      if (m) m.textContent = 'No se pudo iniciar la camara: ' + (e.message || e);
    }
  };
  iniciar(null);
  // Poblar el selector con las camaras instaladas (tras iniciar, para tener etiquetas)
  try {
    const devices = await navigator.mediaDevices.enumerateDevices();
    const cams = devices.filter((d) => d.kind === 'videoinput');
    const sel = $('#scan-cam');
    if (sel && cams.length) {
      sel.innerHTML = cams.map((c, i) => `<option value="${c.deviceId}">${esc(c.label || ('Camara ' + (i + 1)))}</option>`).join('');
      sel.addEventListener('change', () => iniciar(sel.value));
    }
  } catch (e) { /* enumerateDevices no disponible */ }
}

function modalProducto(prod) {
  const p = prod || {};
  openModal(`
    <h3>${prod ? 'Editar producto' : 'Nuevo producto'}</h3>
    <label>Nombre *</label>
    <input id="m-nombre" value="${esc(p.nombre || '')}" />
    <div class="field-error" id="e-nombre">El nombre es obligatorio.</div>
    <label>Categoria</label>
    <select id="m-categoria">${CATEGORIAS.map(c => `<option ${c === (p.categoria || 'General') ? 'selected' : ''}>${c}</option>`).join('')}</select>
    <label>Tipo / marca (opcional)</label>
    <input id="m-tipo" value="${esc(p.tipo || '')}" />
    <label>Unidad de medida</label>
    <select id="m-unidad">${UNIDADES.map(u => `<option ${u === (p.unidadMedida || 'unidad') ? 'selected' : ''}>${u}</option>`).join('')}</select>
    <label>Codigo de barras (opcional)</label>
    <div style="display:flex;gap:8px">
      <input id="m-codigo" value="${esc(p.codigoBarras || '')}" style="flex:1" />
      <button type="button" class="btn btn-ghost" id="m-scan">&#128247; Escanear</button>
    </div>
    <div class="modal-actions">
      <button class="btn btn-ghost" id="m-cancel">Cancelar</button>
      <button class="btn btn-primary" id="m-save">Guardar</button>
    </div>
  `);
  $('#m-cancel').addEventListener('click', closeModal);
  $('#m-scan').addEventListener('click', () => openCameraScan((v) => { const el = $('#m-codigo'); if (el) el.value = v; }));
  $('#m-save').addEventListener('click', async () => {
    const nombre = $('#m-nombre').value.trim();
    if (!nombre) { $('#e-nombre').classList.add('show'); return; }
    await window.api.productoSave({
      id: p.id, nombre,
      categoria: $('#m-categoria').value,
      tipo: $('#m-tipo').value.trim(),
      unidadMedida: $('#m-unidad').value,
      codigoBarras: $('#m-codigo').value.trim() || null,
      creadoEn: p.creadoEn
    });
    closeModal();
    await cargar();
  });
}

function modalPrecio(productoId) {
  const datalist = state.tiendas.map(t => `<option value="${esc(t.nombre)}">`).join('');
  openModal(`
    <h3>Registrar precio</h3>
    <label>Precio *</label>
    <input id="m-precio" type="number" step="0.01" min="0" />
    <div class="field-error" id="e-precio">Ingresa un precio valido mayor que 0.</div>
    <label>Cantidad</label>
    <input id="m-cantidad" type="number" step="0.01" value="1" />
    <label>Tipo de precio</label>
    <select id="m-tipo">${TIPOS.map(t => `<option>${t}</option>`).join('')}</select>
    <label>Tienda</label>
    <input id="m-tienda" list="dl-tiendas" />
    <datalist id="dl-tiendas">${datalist}</datalist>
    <div class="modal-actions">
      <button class="btn btn-ghost" id="m-cancel">Cancelar</button>
      <button class="btn btn-primary" id="m-save">Guardar</button>
    </div>
  `);
  $('#m-cancel').addEventListener('click', closeModal);
  $('#m-save').addEventListener('click', async () => {
    const precio = parseFloat(($('#m-precio').value || '').replace(',', '.'));
    if (!precio || precio <= 0) { $('#e-precio').classList.add('show'); return; }
    const cantidad = parseFloat(($('#m-cantidad').value || '1').replace(',', '.')) || 1;
    await window.api.precioSave({
      productoId, precio, cantidad,
      tipoPrecio: $('#m-tipo').value,
      tienda: $('#m-tienda').value.trim(),
      fecha: Date.now()
    });
    closeModal();
    await cargar();
  });
}

function modalAlacena(prod) {
  const a = state.alacena.find(x => x.productoId === prod.id) || { productoId: prod.id, cantidadActual: 0, cantidadMinima: 1 };
  openModal(`
    <h3>${esc(prod.nombre)}</h3>
    <label>Cantidad actual</label>
    <input id="m-actual" type="number" step="0.01" value="${a.cantidadActual}" />
    <label>Cantidad minima (alerta)</label>
    <input id="m-min" type="number" step="0.01" value="${a.cantidadMinima}" />
    <div class="modal-actions">
      <button class="btn btn-ghost" id="m-cancel">Cancelar</button>
      <button class="btn btn-primary" id="m-save">Guardar</button>
    </div>
  `);
  $('#m-cancel').addEventListener('click', closeModal);
  $('#m-save').addEventListener('click', async () => {
    await window.api.alacenaSave({
      productoId: prod.id,
      cantidadActual: parseFloat(($('#m-actual').value || '0').replace(',', '.')) || 0,
      cantidadMinima: parseFloat(($('#m-min').value || '1').replace(',', '.')) || 1
    });
    closeModal();
    await cargar();
  });
}

function modalPin() {
  openModal(`
    <h3>Definir PIN</h3>
    <label>PIN (4 a 8 digitos)</label>
    <input id="m-pin" type="password" inputmode="numeric" maxlength="8" />
    <label>Confirmar PIN</label>
    <input id="m-pin2" type="password" inputmode="numeric" maxlength="8" />
    <div class="field-error" id="e-pin">Revisa el PIN.</div>
    <div class="modal-actions">
      <button class="btn btn-ghost" id="m-cancel">Cancelar</button>
      <button class="btn btn-primary" id="m-save">Guardar</button>
    </div>
  `);
  $('#m-cancel').addEventListener('click', closeModal);
  $('#m-save').addEventListener('click', async () => {
    const pin = $('#m-pin').value;
    const pin2 = $('#m-pin2').value;
    const err = $('#e-pin');
    if (pin.length < 4) { err.textContent = 'Usa al menos 4 digitos'; err.classList.add('show'); return; }
    if (pin !== pin2) { err.textContent = 'Los PIN no coinciden'; err.classList.add('show'); return; }
    await window.api.pinSet(pin);
    closeModal();
    go('ajustes');
  });
}

function modalOcr(res) {
  const items = res.productos || [];
  const lista = items.length
    ? items.map(it => `<div style="padding:4px 0">&bull; ${esc(it.nombre)} &mdash; ${moneda(Number(it.precio) || 0)} (${esc(String(it.cantidad || 1))} ${esc(it.unidad || 'unidad')})</div>`).join('')
    : '<div class="empty">No se detectaron productos.</div>';
  openModal(`
    <h3>Factura leida</h3>
    ${res.tienda ? `<div style="margin-bottom:8px">Tienda: <b>${esc(res.tienda)}</b></div>` : ''}
    <div style="max-height:300px;overflow:auto">${lista}</div>
    ${items.length ? `<label style="display:flex;align-items:center;gap:8px;margin-top:12px;cursor:pointer"><input type="checkbox" id="ocr-alacena" style="width:auto" /> Sumar cantidades a Mi Alacena</label>` : ''}
    <div class="modal-actions">
      <button class="btn btn-ghost" id="ocr-cancel">Cancelar</button>
      <button class="btn btn-primary" id="ocr-add" ${items.length ? '' : 'disabled'}>Agregar ${items.length}</button>
    </div>
  `);
  $('#ocr-cancel').addEventListener('click', closeModal);
  const add = $('#ocr-add');
  if (items.length) {
    add.addEventListener('click', async () => {
      const sumar = !!($('#ocr-alacena') && $('#ocr-alacena').checked);
      await window.api.ocrAdd(res, sumar);
      closeModal();
      await cargar();
    });
  }
}

// ---------- Init ----------
$$('.tab').forEach(b => b.addEventListener('click', () => go(b.dataset.view)));
$('#modal-overlay').addEventListener('click', (e) => { if (e.target.id === 'modal-overlay') closeModal(); });
initApp();
