export const SPECIAL_SIGN_IMAGE_NUMBERS = Object.freeze({
    PARAPHE: 999997,
    GENERATED: 999998,
    SPOT: 999999
});

export class SignatureImageResolver {

    static normalizeSignImageNumber(signImageNumber) {
        const normalizedSignImageNumber = signImageNumber == null ? null : Number.parseInt(signImageNumber, 10);
        return signImageNumber != null && Number.isFinite(normalizedSignImageNumber) ? normalizedSignImageNumber : signImageNumber;
    }

    static getUserSignImageIds(userState) {
        if (Array.isArray(userState?.signImageIds)) {
            return userState.signImageIds;
        }
        if (Array.isArray(userState?.userImagesIds)) {
            return userState.userImagesIds;
        }
        return null;
    }

    static getSpecialIndexes(signImages, userState = null) {
        const images = Array.isArray(signImages) ? signImages : [];
        if (images.length === 0) {
            return {
                generatedSignImageNumber: null,
                parapheSignImageNumber: null
            };
        }

        const userSignImageIds = this.getUserSignImageIds(userState);
        if (userSignImageIds != null) {
            const generatedSignImageNumber = images.length > userSignImageIds.length ? userSignImageIds.length : null;
            return {
                generatedSignImageNumber,
                parapheSignImageNumber: generatedSignImageNumber != null && images.length > generatedSignImageNumber + 1
                    ? generatedSignImageNumber + 1
                    : null
            };
        }

        if (images.length === 1) {
            return {
                generatedSignImageNumber: 0,
                parapheSignImageNumber: null
            };
        }

        return {
            generatedSignImageNumber: images.length - 2,
            parapheSignImageNumber: images.length - 1
        };
    }

    static isGeneratedSignImageNumber(imageNum, specialIndexes = {}) {
        const normalizedImageNum = Number.parseInt(imageNum, 10);
        return normalizedImageNum === SPECIAL_SIGN_IMAGE_NUMBERS.GENERATED
            || (Number.isInteger(specialIndexes.generatedSignImageNumber) && normalizedImageNum === specialIndexes.generatedSignImageNumber);
    }

    static isParapheSignImageNumber(imageNum, specialIndexes = {}) {
        const normalizedImageNum = Number.parseInt(imageNum, 10);
        return normalizedImageNum === SPECIAL_SIGN_IMAGE_NUMBERS.PARAPHE
            || (Number.isInteger(specialIndexes.parapheSignImageNumber) && normalizedImageNum === specialIndexes.parapheSignImageNumber);
    }

    static resolveRequestedSignImageNumber(imageNum, specialIndexes = {}) {
        if (this.isParapheSignImageNumber(imageNum, specialIndexes)) {
            return SPECIAL_SIGN_IMAGE_NUMBERS.PARAPHE;
        }
        if (this.isGeneratedSignImageNumber(imageNum, specialIndexes)) {
            return SPECIAL_SIGN_IMAGE_NUMBERS.GENERATED;
        }
        return imageNum;
    }

    static getSelectableSignImageNumbers(signImages, specialIndexes = {}) {
        if (!Array.isArray(signImages) || signImages.length === 0) {
            return [];
        }

        const selectableNumbers = [];
        for (let imageIndex = 0; imageIndex < signImages.length; imageIndex++) {
            if (!signImages[imageIndex]) {
                continue;
            }
            if (imageIndex === specialIndexes.generatedSignImageNumber || imageIndex === specialIndexes.parapheSignImageNumber) {
                continue;
            }
            selectableNumbers.push(imageIndex);
        }
        if (specialIndexes.generatedSignImageNumber != null && signImages[specialIndexes.generatedSignImageNumber]) {
            selectableNumbers.push(SPECIAL_SIGN_IMAGE_NUMBERS.GENERATED);
        }
        if (specialIndexes.parapheSignImageNumber != null && signImages[specialIndexes.parapheSignImageNumber]) {
            selectableNumbers.push(SPECIAL_SIGN_IMAGE_NUMBERS.PARAPHE);
        }
        return selectableNumbers;
    }

    static resolveImageRequest(imageNum, signImages, specialIndexes = {}) {
        const images = Array.isArray(signImages) ? signImages : [];
        const normalizedImageNum = this.normalizeSignImageNumber(imageNum);
        const requestedSignImageNumber = this.resolveRequestedSignImageNumber(normalizedImageNum, specialIndexes);
        let resolvedImageNumber = normalizedImageNum;

        if (requestedSignImageNumber === SPECIAL_SIGN_IMAGE_NUMBERS.GENERATED) {
            if (Number.isInteger(specialIndexes.generatedSignImageNumber)) {
                resolvedImageNumber = specialIndexes.generatedSignImageNumber;
            } else if (Number.isInteger(specialIndexes.parapheSignImageNumber)) {
                resolvedImageNumber = Math.max(0, specialIndexes.parapheSignImageNumber - 1);
            } else {
                resolvedImageNumber = images.length > 1
                    ? Math.max(0, images.length - 2)
                    : Math.max(0, images.length - 1);
            }
        } else if (requestedSignImageNumber === SPECIAL_SIGN_IMAGE_NUMBERS.PARAPHE) {
            if (Number.isInteger(specialIndexes.parapheSignImageNumber)) {
                resolvedImageNumber = specialIndexes.parapheSignImageNumber;
            } else {
                resolvedImageNumber = Math.max(0, images.length - 1);
            }
        } else if (resolvedImageNumber > images.length - 1) {
            resolvedImageNumber = 0;
        }

        return {
            requestedSignImageNumber,
            resolvedImageNumber
        };
    }
}
