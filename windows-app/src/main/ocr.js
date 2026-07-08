// OCR de facturas via OpenAI (vision + PDF). Acepta imagen, PDF o ZIP (con PDF/imagen dentro).
const fs = require('fs');
const path = require('path');
const AdmZip = require('adm-zip');
const config = require('./config');

function prompt() {
  return `Eres un extractor de facturas/recibos. Devuelve SOLO un objeto JSON con esta forma:
{"tienda": "nombre del establecimiento o vacio", "productos": [{"nombre": "nombre limpio", "precio": 0, "cantidad": 1, "unidad": "unidad|kg|g|L|ml|lb"}]}

REGLAS DE NUMEROS (MUY IMPORTANTE):
- La moneda es ${config.getMoneda()}. Muchos paises (Colombia, etc.) usan el PUNTO como separador de MILES y la COMA como separador decimal.
- Interpreta los montos en ese contexto. Ejemplos: "9.120" = 9120 ; "22.950" = 22950 ; "1.234.567" = 1234567 ; "1.234,50" = 1234.50.
- Devuelve "precio" como numero real SIN separadores de miles (ej: 9120, NUNCA 9.12).
- Usa el precio UNITARIO (columna VR. UNIT o similar) cuando exista.
Normaliza los nombres (sin codigos). Si no hay datos, usa listas/valores vacios.`;
}

function detectarMime(nombre) {
  const n = nombre.toLowerCase();
  if (n.endsWith('.pdf')) return 'application/pdf';
  if (n.endsWith('.png')) return 'image/png';
  if (n.endsWith('.webp')) return 'image/webp';
  return 'image/jpeg';
}

function prepararArchivo(filePath) {
  const ext = path.extname(filePath).toLowerCase();
  let bytes, mime;
  if (ext === '.zip') {
    const zip = new AdmZip(filePath);
    const entry = zip.getEntries().find(
      (e) => !e.isDirectory && /\.(pdf|jpg|jpeg|png|webp)$/i.test(e.entryName)
    );
    if (!entry) throw new Error('El ZIP no contiene un PDF ni una imagen de factura.');
    bytes = entry.getData();
    mime = detectarMime(entry.entryName);
  } else {
    bytes = fs.readFileSync(filePath);
    mime = detectarMime(filePath);
  }
  return { b64: bytes.toString('base64'), mime };
}

async function run(filePath) {
  const key = config.getOpenAiKey();
  if (!key) throw new Error('Configura tu API key de OpenAI en Ajustes.');

  const { b64, mime } = prepararArchivo(filePath);
  const parteArchivo = mime === 'application/pdf'
    ? { type: 'file', file: { filename: 'factura.pdf', file_data: `data:application/pdf;base64,${b64}` } }
    : { type: 'image_url', image_url: { url: `data:${mime};base64,${b64}` } };

  const body = {
    model: 'gpt-4o-mini',
    response_format: { type: 'json_object' },
    max_tokens: 2000,
    messages: [{ role: 'user', content: [{ type: 'text', text: prompt() }, parteArchivo] }]
  };

  const resp = await fetch('https://api.openai.com/v1/chat/completions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${key}` },
    body: JSON.stringify(body)
  });

  const text = await resp.text();
  if (!resp.ok) throw new Error(`OpenAI (${resp.status}): ${text.slice(0, 300)}`);
  const content = JSON.parse(text).choices[0].message.content;
  const parsed = JSON.parse(content);
  return {
    tienda: parsed.tienda || '',
    productos: Array.isArray(parsed.productos) ? parsed.productos : []
  };
}

module.exports = { run };
