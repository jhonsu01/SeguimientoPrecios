'use strict';

const state = { productos: [], precios: [], tiendas: [], view: 'dashboard', detalleId: null };

const UNIDADES = ['unidad', 'ml', 'L', 'g', 'kg', 'lb'];
const TIPOS = ['unitario', 'promocion'];
const CATEGORIAS = ['General', 'Alimentos', 'Bebidas', 'Aseo', 'Hogar', 'Tecnologia', 'Otros'];

const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));

// Fallback SOLO para previsualizacion en navegador (sin Electron). En produccion
// window.api lo provee preload.js y este bloque no se ejecuta.
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
    tiendas: [{ id: 't1', nombre: 'Exito' }, { id: 't2', nombre: 'D1' }, { id: 't3', nombre: 'Ara' }]
  };
  const uid = () => 'id' + Math.random().toString(36).slice(2);
  window.api = {
    productosList: async () => mem.productos,
    productoSave: async (p) => { if (!p.id) { p.id = uid(); mem.productos.push(p); } else { Object.assign(mem.productos.find(x => x.id === p.id), p); } return p.id; },
    productoDelete: async (id) => { mem.productos = mem.productos.filter(x => x.id !== id); mem.precios = mem.precios.filter(x => x.productoId !== id); },
    preciosAll: async () => mem.precios,
    precioSave: async (p) => { p.id = p.id || uid(); mem.precios.push(p); if (p.tienda && !mem.tiendas.find(t => t.nombre.toLowerCase() === p.tienda.toLowerCase())) mem.tiendas.push({ id: uid(), nombre: p.tienda }); return p.id; },
    precioDelete: async (id) => { mem.precios = mem.precios.filter(x => x.id !== id); },
    tiendasList: async () => mem.tiendas
  };
}

// ---------- Helpers ----------
function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
function moneda(v) {
  return '$' + Number(v).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
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

// ---------- Carga de datos ----------
async function cargar() {
  const [productos, precios, tiendas] = await Promise.all([
    window.api.productosList(),
    window.api.preciosAll(),
    window.api.tiendasList()
  ]);
  state.productos = productos;
  state.precios = precios;
  state.tiendas = tiendas;
  render();
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
  else if (state.view === 'graficos') c.innerHTML = viewGraficos();
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
    <div class="list-item" data-open="${p.id}">
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
      <button class="btn btn-primary" id="nuevo-producto">+ Nuevo producto</button>
    </div>
    ${state.productos.length ? filas : '<div class="empty">Toca "+ Nuevo producto" para empezar.</div>'}
  `;
}

function viewDetalle() {
  const p = state.productos.find(x => x.id === state.detalleId);
  if (!p) { return '<div class="empty">Producto no encontrado.</div>'; }
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

  const stats = pp.length ? `
    <div class="stat-row">
      <div class="stat"><div class="value">${moneda(Math.min(...pp.map(x => x.precio)))}</div><div class="label">Minimo</div></div>
      <div class="stat"><div class="value">${moneda(pp.reduce((s, x) => s + x.precio, 0) / pp.length)}</div><div class="label">Promedio</div></div>
      <div class="stat"><div class="value">${moneda(Math.max(...pp.map(x => x.precio)))}</div><div class="label">Maximo</div></div>
    </div>` : '<div class="empty">Este producto aun no tiene precios.</div>';

  return `
    <h1 class="page-title">Graficos</h1>
    <p class="page-sub">Evolucion de precios por producto en el tiempo.</p>
    <label>Producto</label>
    <select id="sel-producto" style="max-width:360px">${options}</select>
    <div class="card" style="margin-top:16px">
      <div class="section-title">Variacion reciente ${chipHtml(pp)}</div>
      <canvas id="chart"></canvas>
    </div>
    ${stats}
  `;
}

// ---------- Listeners ----------
function attach() {
  $$('[data-open]').forEach(el => el.addEventListener('click', () => go('detalle', el.dataset.open)));
  $$('[data-edit]').forEach(el => el.addEventListener('click', (e) => {
    e.stopPropagation();
    const prod = state.productos.find(p => p.id === el.dataset.edit);
    modalProducto(prod);
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
  const nuevoProd = $('#nuevo-producto');
  if (nuevoProd) nuevoProd.addEventListener('click', () => modalProducto(null));
  const volver = $('#volver');
  if (volver) volver.addEventListener('click', () => go('productos'));
  const nuevoPrecio = $('#nuevo-precio');
  if (nuevoPrecio) nuevoPrecio.addEventListener('click', () => modalPrecio(state.detalleId));
  const selP = $('#sel-producto');
  if (selP) selP.addEventListener('change', () => { state.detalleId = selP.value; render(); });

  const canvas = $('#chart');
  if (canvas) {
    const id = state.detalleId || (state.productos[0] && state.productos[0].id);
    const pp = id ? preciosDe(id) : [];
    requestAnimationFrame(() => drawChart(canvas, pp));
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
  const L = 72, R = 14, T = 14, B = 26;
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
    <input id="m-codigo" value="${esc(p.codigoBarras || '')}" />
    <div class="modal-actions">
      <button class="btn btn-ghost" id="m-cancel">Cancelar</button>
      <button class="btn btn-primary" id="m-save">Guardar</button>
    </div>
  `);
  $('#m-cancel').addEventListener('click', closeModal);
  $('#m-save').addEventListener('click', async () => {
    const nombre = $('#m-nombre').value.trim();
    if (!nombre) { $('#e-nombre').classList.add('show'); return; }
    await window.api.productoSave({
      id: p.id,
      nombre,
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
      productoId,
      precio,
      cantidad,
      tipoPrecio: $('#m-tipo').value,
      tienda: $('#m-tienda').value.trim(),
      fecha: Date.now()
    });
    closeModal();
    await cargar();
  });
}

// ---------- Init ----------
$$('.tab').forEach(b => b.addEventListener('click', () => go(b.dataset.view)));
$('#modal-overlay').addEventListener('click', (e) => { if (e.target.id === 'modal-overlay') closeModal(); });
cargar();
