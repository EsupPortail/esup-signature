import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';

const source = readFileSync(new URL('../../main/resources/static/js/modules/ui/signrequests/SignPlacementController.js', import.meta.url), 'utf8');
const context = vm.createContext({
    EventFactory: class {},
    SPECIAL_SIGN_IMAGE_NUMBERS: {SPOT: 999999, PARAPHE: 999998},
    SignRequestParams: class {
        async changeSignImage(number) { this.signImageNumber = number; }
    }
});
vm.runInContext(source.slice(source.indexOf('export class ')).replace('export class ', 'class ')
    + '\nglobalThis.SignPlacementController = SignPlacementController;', context);

for (const signType of ['visa', 'pdfImageStamp']) {
    for (const imageNumber of [-1, -2, -3, -4]) {
        test(`${signType} loads annotation image ${imageNumber} before displaying its placement`, async () => {
            const controller = Object.assign(Object.create(context.SignPlacementController.prototype), {
                signType,
                id: 0,
                signRequestParamses: new Map(),
                normalizeSignImageNumber: number => number,
                prepareVisualPlacement: () => {},
                getPendingCurrentSignRequestParams: () => null,
                buildInitialSignRequestParamsModel: () => null,
                canAddAnnotations: () => true,
                applySpecialSignImageNumbers: () => {},
                bindSignRequestParamsEvents: () => {},
                syncAddSignButtonState: () => {},
                refreshSteps: () => {}
            });
            const placement = await controller.addSign(1, false, imageNumber, null, 'annotation');
            assert.equal(placement.signImageNumber, imageNumber);
            assert.equal(controller.signRequestParamses.get(0), placement);
        });
    }
}
