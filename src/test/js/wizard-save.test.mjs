import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';

const source = readFileSync(new URL('../../main/resources/static/js/modules/ui/WizUi.js', import.meta.url), 'utf8');

function setup(valid = true, title = 'Circuit & modèle') {
    const elements = new Map();
    const requests = [];
    const $ = selector => {
        if (!elements.has(selector)) {
            elements.set(selector, {
                disabled: false, content: '', hidden: true,
                prop(name, value) { this[name] = value; return this; },
                text(value) { this.content = value; return this; },
                addClass() { this.hidden = true; return this; },
                removeClass() { this.hidden = false; return this; },
                attr() { return 'Veuillez réessayer'; },
                val() { return title; }
            });
        }
        return elements.get(selector);
    };
    $.ajax = options => requests.push(options);
    const context = vm.createContext({$, document: {getElementById: () => ({reportValidity: () => valid})}, location: {href: ''}});
    vm.runInContext(source.slice(source.indexOf('export class ')).replace('export class ', 'class ')
        + '\nglobalThis.WizUi = WizUi;', context);
    const wizard = Object.assign(Object.create(context.WizUi.prototype), {
        csrf: {parameterName: '_csrf', token: 'token'}, newSignBookId: '12', newWorkflowId: '',
        savingWorkflow: false, recipientCCSelect: null
    });
    return {wizard, requests, $, context};
}

test('failure keeps modal open, shows server message and allows retry', () => {
    const {wizard, requests, $, context} = setup();
    wizard.saveWorkflow();
    assert.equal($('#save-workflow').disabled, true);
    wizard.saveWorkflow();
    assert.equal(requests.length, 1);
    assert.equal(requests[0].data.name, 'Circuit & modèle');
    requests[0].error({responseJSON: {message: 'Limite atteinte'}});
    requests[0].complete();
    assert.equal($('#es-wizard-save-error').content, 'Limite atteinte');
    assert.equal($('#es-wizard-save-error').hidden, false);
    assert.equal($('#save-workflow').disabled, false);
    assert.equal(context.location.href, '');
    wizard.saveWorkflow();
    assert.equal(requests.length, 2);
    requests[1].success();
    assert.equal(context.location.href, '/user/signbooks/12');
});

test('invalid form sends no request and non-JSON failures use readable fallback', () => {
    const invalid = setup(false);
    invalid.wizard.saveWorkflow();
    assert.equal(invalid.requests.length, 0);
    const {$, wizard, requests} = setup();
    wizard.saveWorkflow();
    requests[0].error({responseText: '<html>SQL details</html>'});
    requests[0].complete();
    assert.equal($('#es-wizard-save-error').content, 'Veuillez réessayer');
});

test('modal dismissal and exit are blocked during saving', () => {
    const {wizard, context} = setup();
    const listeners = new Map();
    wizard.modal = {on: (event, callback) => listeners.set(event, callback)};
    wizard.initListeners();
    wizard.savingWorkflow = true;
    let prevented = false;
    listeners.get('hide.bs.modal')({preventDefault: () => { prevented = true; }});
    wizard.wizardExit();
    assert.equal(prevented, true);
    assert.equal(context.location.href, '');
});

test('oversized title is rejected before sending and 255 characters are accepted', () => {
    const oversized = setup(true, 'A'.repeat(256));
    oversized.wizard.saveWorkflow();
    assert.equal(oversized.requests.length, 0);
    assert.equal(oversized.$('#es-wizard-save-error').hidden, false);
    assert.equal(oversized.$('#save-workflow').disabled, false);
    const limit = setup(true, 'A'.repeat(255));
    limit.wizard.saveWorkflow();
    assert.equal(limit.requests.length, 1);
});
