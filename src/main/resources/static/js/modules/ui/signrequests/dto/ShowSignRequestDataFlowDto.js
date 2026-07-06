function asArray(value) {
    return Array.isArray(value) ? value : [];
}

function asObject(value) {
    return value != null && typeof value === "object" ? value : {};
}

export class FrontUserDto {
    constructor(raw = {}) {
        const source = asObject(raw);
        this.id = source.id ?? null;
        this.eppn = source.eppn ?? null;
        this.name = source.name ?? null;
        this.firstname = source.firstname ?? null;
        this.email = source.email ?? null;
        this.defaultSignImageNumber = source.defaultSignImageNumber ?? null;
        this.phone = source.phone ?? null;
        this.returnToHomeAfterSign = source.returnToHomeAfterSign ?? null;
    }

    static from(raw) {
        return raw instanceof FrontUserDto ? raw : new FrontUserDto(raw);
    }
}

export class CommentFrontDto {
    constructor(raw = {}) {
        const source = asObject(raw);
        this.id = source.id ?? null;
        this.pageNumber = source.pageNumber ?? null;
        this.stepNumber = source.stepNumber ?? null;
        this.posX = source.posX ?? null;
        this.posY = source.posY ?? null;
        this.deleteAllowed = source.deleteAllowed ?? false;
    }

    static from(raw) {
        return raw instanceof CommentFrontDto ? raw : new CommentFrontDto(raw);
    }
}

export class SignRequestParamsFrontDto {
    constructor(raw = {}) {
        const source = asObject(raw);
        this.id = source.id ?? null;
        this.pdSignatureFieldName = source.pdSignatureFieldName ?? null;
        this.stepNumber = source.stepNumber ?? null;
        this.signImageNumber = source.signImageNumber ?? null;
        this.signPageNumber = source.signPageNumber ?? null;
        this.signDocumentNumber = source.signDocumentNumber ?? null;
        this.signWidth = source.signWidth ?? null;
        this.signHeight = source.signHeight ?? null;
        this.xPos = source.xPos ?? null;
        this.yPos = source.yPos ?? null;
        this.extraText = source.extraText ?? "";
        this.isExtraText = source.isExtraText ?? null;
        this.addWatermark = source.addWatermark ?? null;
        this.allPages = source.allPages ?? null;
        this.addImage = source.addImage ?? null;
        this.addExtra = source.addExtra ?? null;
        this.extraType = source.extraType ?? null;
        this.extraName = source.extraName ?? null;
        this.extraDate = source.extraDate ?? null;
        this.extraOnTop = source.extraOnTop ?? null;
        this.textPart = source.textPart ?? null;
        this.signScale = source.signScale ?? null;
        this.red = source.red ?? null;
        this.green = source.green ?? null;
        this.blue = source.blue ?? null;
        this.fontSize = source.fontSize ?? null;
        this.recipientId = source.recipientId ?? null;
        this.ready = source.ready ?? null;
    }

    static from(raw) {
        return raw instanceof SignRequestParamsFrontDto ? raw : new SignRequestParamsFrontDto(raw);
    }
}

export class FieldFrontDto {
    constructor(raw = {}) {
        const source = asObject(raw);
        this.id = source.id ?? null;
        this.name = source.name ?? null;
        this.description = source.description ?? null;
        this.page = source.page ?? null;
        this.required = Boolean(source.required);
        this.readOnly = Boolean(source.readOnly);
        this.editable = Boolean(source.editable);
        this.workflowSteps = asArray(source.workflowSteps);
        this.defaultValue = source.defaultValue ?? null;
        this.searchServiceName = source.searchServiceName ?? null;
        this.searchType = source.searchType ?? null;
        this.searchReturn = source.searchReturn ?? null;
        this.type = source.type ?? null;
        this.favorisable = Boolean(source.favorisable);
    }

    static from(raw) {
        return raw instanceof FieldFrontDto ? raw : new FieldFrontDto(raw);
    }
}

export class SignUiFrontDto {
    constructor(raw = {}) {
        const source = asObject(raw);
        this.signRequestId = source.signRequestId ?? null;
        this.dataId = source.dataId ?? null;
        this.formId = source.formId ?? null;
        this.steps = asArray(source.steps);
        this.currentSignRequestParamses = asArray(source.currentSignRequestParamses).map(item => SignRequestParamsFrontDto.from(item));
        this.signImageNumber = source.signImageNumber ?? null;
        this.currentSignType = source.currentSignType ?? null;
        this.signable = Boolean(source.signable);
        this.editable = Boolean(source.editable);
        this.comments = asArray(source.comments).map(item => CommentFrontDto.from(item));
        this.spots = asArray(source.spots).map(item => SignRequestParamsFrontDto.from(item));
        this.pdf = Boolean(source.pdf);
        this.currentStepNumber = source.currentStepNumber ?? null;
        this.currentStepMultiSign = Boolean(source.currentStepMultiSign);
        this.currentStepSingleSignWithAnnotation = Boolean(source.currentStepSingleSignWithAnnotation);
        this.currentStepMinSignLevel = source.currentStepMinSignLevel ?? null;
        this.workflowAvailable = Boolean(source.workflowAvailable);
        this.signImages = asArray(source.signImages);
        this.userName = source.userName ?? null;
        this.authUserName = source.authUserName ?? null;
        this.fields = asArray(source.fields).map(item => FieldFrontDto.from(item));
        this.stepRepeatable = source.stepRepeatable ?? null;
        this.status = source.status ?? null;
        this.action = source.action ?? null;
        this.nbSignRequests = source.nbSignRequests ?? 0;
        this.notSigned = Boolean(source.notSigned);
        this.attachmentAlert = Boolean(source.attachmentAlert);
        this.attachmentRequire = Boolean(source.attachmentRequire);
        this.otp = Boolean(source.otp);
        this.restore = source.restore ?? null;
        this.phone = source.phone ?? null;
        this.returnToHomeAfterSign = source.returnToHomeAfterSign ?? null;
        this.manager = Boolean(source.manager);
    }

    static from(raw) {
        return raw instanceof SignUiFrontDto ? raw : new SignUiFrontDto(raw);
    }
}

export class ShowSignRequestFrontDto {
    constructor(raw = {}) {
        const source = asObject(raw);
        this.user = source.user != null ? FrontUserDto.from(source.user) : null;
        this.authUser = source.authUser != null ? FrontUserDto.from(source.authUser) : null;
        this.creator = source.creator != null ? FrontUserDto.from(source.creator) : null;
        this.currentStepNumber = source.currentStepNumber ?? null;
        this.supervisors = asArray(source.supervisors);
        this.lastStep = Boolean(source.lastStep);
        this.signUi = SignUiFrontDto.from(source.signUi);
    }

    static from(raw) {
        return raw instanceof ShowSignRequestFrontDto ? raw : new ShowSignRequestFrontDto(raw);
    }
}

export class ShowSignRequestBackDto {
    constructor(raw = {}) {
        const source = asObject(raw);
        Object.assign(this, source);
        this.postits = asArray(source.postits);
        this.comments = asArray(source.comments);
        this.spots = asArray(source.spots);
        this.attachments = asArray(source.attachments);
        this.fields = asArray(source.fields);
        this.signRequestParams = asArray(source.signRequestParams);
        this.signImages = asArray(source.signImages);
        this.signWiths = asArray(source.signWiths);
        this.certificats = asArray(source.certificats);
        this.steps = asArray(source.steps);
        this.refuseLogs = asArray(source.refuseLogs);
        this.logs = asArray(source.logs);
        this.externalsRecipients = asArray(source.externalsRecipients);
        this.signatureIds = asArray(source.signatureIds);
        this.allSignWiths = asArray(source.allSignWiths);
    }

    static from(raw) {
        return raw instanceof ShowSignRequestBackDto ? raw : new ShowSignRequestBackDto(raw);
    }
}

export class ShowSignRequestDataFlowDto {
    constructor(raw = {}) {
        const source = asObject(raw);
        this.back = ShowSignRequestBackDto.from(source.back);
        this.front = ShowSignRequestFrontDto.from(source.front);
    }

    static from(raw) {
        return raw instanceof ShowSignRequestDataFlowDto ? raw : new ShowSignRequestDataFlowDto(raw);
    }
}
