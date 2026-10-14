import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';

const source = readFileSync(new URL('../../main/resources/static/js/modules/ui/signrequests/SignWorkspaceController.js', import.meta.url), 'utf8');
const context = vm.createContext({$: () => ({hasClass: () => false})});
vm.runInContext(source.slice(source.indexOf('export class ')).replace('export class ', 'class ')
    + '\nglobalThis.SignWorkspaceController = SignWorkspaceController;', context);

test('annotation tools availability is independent of the current PDF page', () => {
    const states = [];
    const workspace = Object.assign(Object.create(context.SignWorkspaceController.prototype), {
        isPdf: true,
        pdfRenderComplete: true,
        editable: true,
        isManager: true,
        signSpaceManager: {canDisplaySignSpaces: () => true},
        pdfViewer: {pageNum: 1},
        signPlacementController: {signRequestParamses: new Map()},
        toolbar: {setAnnotationActionButtonsDisabled: (...disabled) => states.push(disabled)}
    });

    for (const page of [1, 2, 3]) {
        workspace.pdfViewer.pageNum = page;
        workspace.updateAnnotationActionButtonsAvailability();
    }
    assert.deepEqual(states.splice(0), [[false, false], [false, false], [false, false]]);

    workspace.signPlacementController.signRequestParamses.set(1, {
        isSign: true, signImageNumber: 0, signPageNumber: 2
    });
    for (const page of [1, 2, 3]) {
        workspace.pdfViewer.pageNum = page;
        workspace.updateAnnotationActionButtonsAvailability();
    }
    assert.deepEqual(states.splice(0), [[true, true], [true, true], [true, true]]);

    workspace.signPlacementController.signRequestParamses.clear();
    workspace.signPlacementController.signRequestParamses.set(999999, {
        isSign: true, signImageNumber: 999999, signPageNumber: 3
    });
    workspace.updateAnnotationActionButtonsAvailability();
    assert.deepEqual(states.at(-1), [false, false]);

    workspace.pdfRenderComplete = false;
    workspace.updateAnnotationActionButtonsAvailability();
    assert.deepEqual(states.at(-1), [true, true]);
});
