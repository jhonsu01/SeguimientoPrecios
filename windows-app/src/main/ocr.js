// OCR de facturas via OpenAI (vision). La API key la provee el usuario (Ajustes).
const fs = require('fs');
const config = require('./config');

const PROMPT = `Eres un extractor de facturas. Analiza la imagen de la factura/recibo y devuelve
SOLO un objeto JSON con esta forma exacta:
{"tienda": "nombre del establecimiento o vacio", "productos": [{"nombre": "nombre limpio del producto", "precio": 0.0, "cantidad": 1, "unidad": "unidad|kg|g|L|ml|lb"}]}
Normaliza los nombres. El precio es el precio unitario numerico sin simbolos. Si no hay datos, usa listas/valores vacios.`;

async function run(imagePath) {
  const key = config.getOpenAiKey();
  if (!key) throw new Error('Configura tu API key de OpenAI en Ajustes.');

  const b64 = fs.readFileSync(imagePath).toString('base64');
  const ext = String(imagePath.split('.').pop() || 'jpeg').toLowerCase();
  const mime = ext === 'png' ? 'image/png' : 'image/jpeg';

  const body = {
    model: 'gpt-4o-mini',
    response_format: { type: 'json_object' },
    max_tokens: 1500,
    messages: [
      {
        role: 'user',
        content: [
          { type: 'text', text: PROMPT },
          { type: 'image_url', image_url: { url: `data:${mime};base64,${b64}` } }
        ]
      }
    ]
  };

  const resp = await fetch('https://api.openai.com/v1/chat/completions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${key}`
    },
    body: JSON.stringify(body)
  });

  const text = await resp.text();
  if (!resp.ok) {
    throw new Error(`OpenAI (${resp.status}): ${text.slice(0, 300)}`);
  }
  const content = JSON.parse(text).choices[0].message.content;
  const parsed = JSON.parse(content);
  return {
    tienda: parsed.tienda || '',
    productos: Array.isArray(parsed.productos) ? parsed.productos : []
  };
}

module.exports = { run };
