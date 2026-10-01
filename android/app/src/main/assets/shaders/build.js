// Builds nfsmw_shaders.nfsp on the phone, with the same steps and modules as the nfsmw-nx installer page
// (lib/ and wasm/ are copied from it unchanged; see INSTALLER_COMMIT). ShaderBuilder.java serves this folder and
// the game files under https://appassets.androidplatform.net/, and receives progress and the result through the
// NfsmwShaders interface.
import createHlslModule from './wasm/hlsl.mjs';
import createDxcModule from './wasm/dxc_web.mjs';
import createPackModule from './wasm/pack.mjs';
import createLzxModule from './wasm/lzx.mjs';
import { readXexImage } from './lib/xex.js';
import { ContainerScanner } from './lib/containers.js';
import { buildShaderLibrary } from './lib/shaders.js';
import { setLanguage, t } from './lib/i18n.js';

const host = window.NfsmwShaders;

// The page's own texts, in the launcher's languages; ShaderBuilder.java passes the language as ?lang=. The
// pipeline's texts (lib/shaders.js) come from lib/i18n.js in the same language.
const PAGE_TEXTS = {
  en: {
    loading: 'Loading the shader compiler…',
    readingXex: 'Reading default.xex…',
    searching: 'Searching for shaders in {path}…',
    found: 'Found {count} shaders. Translating…',
    checking: 'Checking the library…',
    cannotRead: 'could not read {what}',
    shortRead: '{path}: read {seen} of {size} bytes',
    xexFailed: 'could not decompress default.xex',
    noBlurShader: 'the composition shader was not found: is this a complete copy of the game?',
    mismatch: 'the library does not match the official one ({hash}…)',
  },
  es: {
    loading: 'Cargando el compilador de shaders…',
    readingXex: 'Leyendo default.xex…',
    searching: 'Buscando shaders en {path}…',
    found: 'Encontrados {count} shaders. Traduciendo…',
    checking: 'Comprobando la biblioteca…',
    cannotRead: 'no se pudo leer {what}',
    shortRead: '{path}: se leyeron {seen} de {size} bytes',
    xexFailed: 'fallo al descomprimir default.xex',
    noBlurShader: 'no se encontró el shader de composición: ¿es una copia completa del juego?',
    mismatch: 'la biblioteca no coincide con la oficial ({hash}…)',
  },
};
const language = setLanguage(new URLSearchParams(location.search).get('lang') || 'en');
const page = PAGE_TEXTS[language] ?? PAGE_TEXTS.en;
const say = (key, params = {}) => page[key].replace(/\{(\w+)\}/g, (_, name) => String(params[name]));
const GAME = 'https://appassets.androidplatform.net/game/';
const CHUNK = 16 << 20;  // as the installer: the scanner keeps 64 KB of look-ahead between chunks

const progress = (fraction, text) => host.progress(Math.max(0, Math.min(1, fraction)), text);

const hex = (buffer) => [...new Uint8Array(buffer)].map((b) => b.toString(16).padStart(2, '0')).join('');
const sha256 = async (bytes) => hex(await crypto.subtle.digest('SHA-256', bytes));

async function fetchBytes(url) {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`${say('cannotRead', { what: url })} (${response.status})`);
  }
  return new Uint8Array(await response.arrayBuffer());
}

// Streams one disc file into the scanner in 16 MB pieces.
async function scanFile(scanner, file, onBytes) {
  const response = await fetch(GAME + file.path.split('/').map(encodeURIComponent).join('/'));
  if (!response.ok || !response.body) {
    throw new Error(say('cannotRead', { what: file.path }));
  }
  scanner.beginFile(file.path, file.size);
  const reader = response.body.getReader();
  let pending = [];
  let pendingBytes = 0;
  let seen = 0;
  const flush = (final) => {
    const piece = new Uint8Array(pendingBytes);
    let at = 0;
    for (const p of pending) {
      piece.set(p, at);
      at += p.length;
    }
    pending = [];
    pendingBytes = 0;
    scanner.push(piece, final);
  };
  for (;;) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }
    pending.push(value);
    pendingBytes += value.length;
    seen += value.length;
    onBytes(value.length);
    if (pendingBytes >= CHUNK && seen < file.size) {
      flush(false);
    }
  }
  if (seen !== file.size) {
    throw new Error(say('shortRead', { path: file.path, seen, size: file.size }));
  }
  flush(true);
}

async function main() {
  progress(0, say('loading'));
  const quiet = () => ({ print: () => {}, printErr: () => {} });
  const [hlsl, dxc, pack, lzx] = await Promise.all([
    createHlslModule(quiet()), createDxcModule(quiet()), createPackModule(quiet()), createLzxModule(quiet()),
  ]);
  const manifest = JSON.parse(new TextDecoder().decode(await fetchBytes('./release/manifest.json')));
  const shaderCommon = await fetchBytes('./shader_common.h');

  progress(0.02, say('readingXex'));
  const xex = await fetchBytes(GAME + 'default.xex');
  const xexHash = await sha256(xex);
  const build = manifest.builds.find((b) => b.xex_sha256 === xexHash);
  const { image } = await readXexImage(xex, async (compressed, bits, size) => {
    lzx.FS.writeFile('/i.lzx', compressed);
    if (lzx.callMain(['/i.lzx', '/i.bin', String(bits), String(size)])) {
      throw new Error(say('xexFailed'));
    }
    return lzx.FS.readFile('/i.bin');
  });
  const executable = new ContainerScanner('xex_');
  executable.scanWhole('default.xex', image);

  // The disc files in the installer's order (lower-case path), so the containers get the same names.
  const files = JSON.parse(host.listDiscFiles());
  const total = files.reduce((sum, f) => sum + f.size, 0);
  const disc = new ContainerScanner('');
  let read = 0;
  for (const f of files) {
    await scanFile(disc, f, (n) => {
      read += n;
      progress(0.03 + 0.37 * (read / total), say('searching', { path: f.path }));
    });
  }
  const containers = [...disc.found, ...executable.found];
  progress(0.4, say('found', { count: containers.length }));

  const blurSha = build ? build.blur_container_sha256 : manifest.builds[0].blur_container_sha256;
  let blurShader = null;
  for (const c of containers) {
    if ((await sha256(c.bytes)) === blurSha) {
      blurShader = c.name.slice(0, -4);
      break;
    }
  }
  if (!blurShader) {
    throw new Error(say('noBlurShader'));
  }

  let compiled = 0;
  const library = await buildShaderLibrary(containers, { hlsl, dxc, pack }, shaderCommon, (text) => {
    // lib/shaders.js reports every 25 compiled shaders with t('compiled'), in the current language.
    if (text === t('compiled', { done: compiled + 25, total: containers.length })) {
      compiled += 25;
      progress(0.45 + 0.53 * (compiled / containers.length), text);
    } else {
      progress(0.45, text);
    }
  }, blurShader);

  progress(0.99, say('checking'));
  const libraryHash = await sha256(library);
  if (build && libraryHash !== build.library_sha256) {
    throw new Error(say('mismatch', { hash: libraryHash.slice(0, 12) }));
  }
  // Base64 in pieces, so no single string conversion of 3 MB of bytes has to fit in the call stack.
  let text = '';
  for (let i = 0; i < library.length; i += 0x8000) {
    text += String.fromCharCode.apply(null, library.subarray(i, i + 0x8000));
  }
  host.done(btoa(text), libraryHash, build ? build.edition : '');
}

main().catch((error) => host.fail(String(error && error.message ? error.message : error)));
