// Ajustes locales (PIN hasheado + API key OpenAI) en userData/config.json.
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

let cfgPath = null;
let cache = { pinHash: null, openAiKey: '' };

function init(userDataPath) {
  cfgPath = path.join(userDataPath, 'config.json');
  if (fs.existsSync(cfgPath)) {
    try {
      cache = Object.assign(cache, JSON.parse(fs.readFileSync(cfgPath, 'utf8')));
    } catch (e) { /* config corrupta: se ignora */ }
  }
}

function guardar() {
  fs.writeFileSync(cfgPath, JSON.stringify(cache, null, 2), 'utf8');
}

function sha256(texto) {
  return crypto.createHash('sha256').update(String(texto), 'utf8').digest('hex');
}

module.exports = {
  init,
  hasPin: () => !!cache.pinHash,
  verifyPin: (pin) => cache.pinHash === sha256(pin),
  setPin: (pin) => { cache.pinHash = sha256(pin); guardar(); },
  clearPin: () => { cache.pinHash = null; guardar(); },
  getOpenAiKey: () => cache.openAiKey || '',
  setOpenAiKey: (key) => { cache.openAiKey = (key || '').trim(); guardar(); }
};
