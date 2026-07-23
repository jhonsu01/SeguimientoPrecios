// OCR de facturas via OpenAI (vision + PDF). Acepta imagen, PDF o ZIP (con PDF/imagen dentro).
const fs = require('fs');
const path = require('path');
const AdmZip = require('adm-zip');
const config = require('./config');

function prompt() {
  return `Eres un extractor de facturas/recibos de supermercado. Transcribe EXACTAMENTE lo que ves en la imagen.
Devuelve SOLO un objeto JSON: {"tienda": "nombre del establecimiento o vacio", "productos": [{"nombre": "...", "precio": 0, "cantidad": 1, "unidad": "unidad|kg|g|L|ml|lb"}]}

REGLAS CRITICAS (obligatorias):
- Transcribe UNICAMENTE los productos que REALMENTE aparecen en la imagen. NUNCA inventes ni agregues productos que no esten en la factura.
- Si la imagen esta borrosa o no puedes leer una linea, OMITELA. Si no puedes leer casi nada, devuelve "productos": [].
- El nombre debe ser la descripcion tal como aparece en el recibo (puedes expandir abreviaturas obvias, pero sin inventar).
- Ignora lineas de totales, subtotales, descuentos, promociones, NIT, codigos de barras, cajero y datos del pie.

NUMEROS (moneda ${config.getMoneda()}, formato latino):
- El PUNTO es separador de MILES y la coma es decimal. Ej: "8.750" = 8750 ; "10.200" = 10200 ; "22.880" = 22880 ; "1.234,50" = 1234.50.
- Devuelve "precio" como numero real SIN separadores de miles (ej: 8750, NUNCA 8.75).
- Si una linea tiene un subrenglon tipo "N UN X valor" (ej: "2 UN X 3.990"), el precio UNITARIO es ese valor (3990) y la cantidad es N. Si no hay subrenglon, usa el valor de la columna como precio y cantidad 1.
- Para productos por peso (ej: "0,512 KGM X 22.880") el precio es el valor por unidad (22880) y la unidad es kg.`;
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
    : { type: 'image_url', image_url: { url: `data:${mime};base64,${b64}`, detail: 'high' } };

  const body = {
    model: 'gpt-4o',
    response_format: { type: 'json_object' },
    temperature: 0,
    max_tokens: 3000,
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
