import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';

const guardSource = readFileSync(new URL('../../main/resources/static/js/customs/pdf-compatibility.js', import.meta.url), 'utf8');
const bootstrapSource = readFileSync(new URL('../../main/resources/static/js/modules/bootstrap/PdfJsBootstrap.js', import.meta.url), 'utf8');

function browser(overrides = {}) {
    const elements = new Map();
    const listeners = {};
    const messages = {'data-browser-error': 'Update browser', 'data-load-error': 'Loading failed'};
    elements.set('es-id-pdf-compatibility-messages', {getAttribute: key => messages[key]});
    const document = {
        body: null,
        createElement: tag => tag === 'script' ? {noModule: false} : {setAttribute() {}},
        getElementById: id => elements.get(id),
        addEventListener: (name, callback) => {listeners[name] = callback;}
    };
    const context = vm.createContext({
        document, Promise, URL, fetch() {}, DOMMatrix() {}, Path2D() {}, ReadableStream() {},
        addEventListener: (name, callback) => {listeners[name] = callback;},
        ...overrides
    });
    vm.runInContext('window = globalThis;', context);
    vm.runInContext(guardSource, context);
    function readyDom() {
        document.body = {insertBefore: alert => elements.set(alert.id, alert)};
        listeners.DOMContentLoaded();
    }
    return {context, elements, listeners, readyDom, guard: context.esupPdfCompatibility};
}

test('a missing baseline capability shows a translated warning even when modules never start', async () => {
    const page = browser({fetch: undefined});
    assert.equal(await page.guard.ready, false);
    assert.equal(page.elements.has('es-id-pdf-compatibility-alert'), false);
    page.readyDom();
    assert.equal(page.elements.get('es-id-pdf-compatibility-alert').textContent, 'Update browser');
});

test('a browser without module support gets the independent warning', () => {
    const page = browser();
    page.context.document.createElement = tag => tag === 'script' ? {} : {setAttribute() {}};
    vm.runInContext(guardSource, page.context);
    page.readyDom();
    assert.equal(page.context.esupPdfCompatibility.incompatible, true);
    assert.equal(page.elements.get('es-id-pdf-compatibility-alert').textContent, 'Update browser');
});

test('PDF.js syntax errors are distinguished from network failures', async () => {
    const syntax = browser();
    syntax.readyDom();
    syntax.guard.complete({name: 'SyntaxError', message: 'Unexpected token'});
    assert.equal(await syntax.guard.ready, false);
    assert.equal(syntax.elements.get('es-id-pdf-compatibility-alert').textContent, 'Update browser');

    const network = browser();
    network.readyDom();
    network.guard.complete({name: 'TypeError', message: 'Failed to fetch dynamically imported module'});
    assert.equal(await network.guard.ready, false);
    assert.equal(network.guard.incompatible, false);
    assert.equal(network.elements.get('es-id-pdf-compatibility-alert').textContent, 'Loading failed');
    assert.equal(network.guard.isCompatibilityError({name: 'TypeError', message: 'Object.groupBy is not a function'}, true), true);
});

test('module errors are reported even before the application can start', () => {
    const page = browser();
    page.listeners.error({filename: 'https://signature.test/webjars/pdfjs-dist/legacy/build/pdf.mjs',
        error: {name: 'SyntaxError', message: 'Unexpected token'}});
    page.readyDom();
    assert.equal(page.elements.get('es-id-pdf-compatibility-alert').textContent, 'Update browser');
});

test('PDF and HTTP errors never suggest a browser update', () => {
    const {guard} = browser();
    for (const name of ['InvalidPDFException', 'MissingPDFException', 'PasswordException', 'UnexpectedResponseException']) {
        assert.equal(guard.isCompatibilityError({name, message: 'Document error'}, false), false);
    }
    assert.equal(guard.isCompatibilityError({name: 'TypeError', message: 'Failed to fetch'}, false), false);
    assert.equal(guard.isCompatibilityError({name: 'SyntaxError', message: 'Invalid PDF data'}, false), false);
    assert.equal(guard.isCompatibilityError({name: 'TypeError', message: 'Promise.withResolvers is not a function'}, false), true);
});

test('runtime capability failures update a single alert', () => {
    const page = browser();
    page.readyDom();
    page.guard.fail({name: 'TypeError', message: 'Failed to fetch'}, false);
    const alert = page.elements.get('es-id-pdf-compatibility-alert');
    page.guard.fail({name: 'ReferenceError', message: 'DOMMatrix is not defined'}, false);
    assert.equal(page.elements.get('es-id-pdf-compatibility-alert'), alert);
    assert.equal(alert.textContent, 'Update browser');
});

test('PDF startup waits for the viewer module before checking capabilities', async () => {
    const page = browser({Promise: class extends Promise {static withResolvers = undefined;}});
    assert.equal(page.guard.incompatible, false);
    let resolveViewer;
    const imports = [];
    page.context.loadModule = path => {
        imports.push(path);
        if (path.endsWith('/build/pdf.mjs')) {
            page.context.Promise.withResolvers = () => ({});
            return Promise.resolve({version: 'test', GlobalWorkerOptions: {}});
        }
        return new Promise(resolve => {resolveViewer = resolve;});
    };
    vm.runInContext(bootstrapSource.replaceAll('import(', 'loadModule('), page.context);
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(imports.length, 2);
    let settled = false;
    page.guard.ready.then(() => {settled = true;});
    await Promise.resolve();
    assert.equal(settled, false);
    resolveViewer({});
    assert.equal(await page.guard.ready, true);
    assert.equal(page.elements.has('es-id-pdf-compatibility-alert'), false);
});

test('PDF startup reports missing graphics capabilities after loading the polyfills', async () => {
    const page = browser({DOMMatrix: undefined});
    page.readyDom();
    page.context.loadModule = () => Promise.resolve({version: 'test', GlobalWorkerOptions: {}});
    vm.runInContext(bootstrapSource.replaceAll('import(', 'loadModule('), page.context);
    assert.equal(await page.guard.ready, false);
    assert.equal(page.elements.get('es-id-pdf-compatibility-alert').textContent, 'Update browser');
});
