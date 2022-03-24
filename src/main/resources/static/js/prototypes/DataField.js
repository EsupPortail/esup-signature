export class DataField {

    constructor(dataField) {
        this.defaultValue;
        this.defaultValues;
        this.eppnEditRight;
        this.extValue;
        this.fillOrder;
        this.id;
        this.label;
        this.leftPos;
        this.name;
        this.description;
        this.page;
        this.required;
        this.readOnly;
        this.searchServiceName;
        this.searchType;
        this.searchReturn;
        // this.stepNumbers;
        this.stepZero;
        this.workflowSteps;
        this.topPos;
        this.type;
        this.action;
        this.editable;
        Object.assign(this, dataField);
    }

    compareByPage(b) {
        let comparison = 0;
        if (this.page > b.page) {
            comparison = 1;
        } else if (this.page < b.page) {
            comparison = -1;
        }
        return comparison;
    }
}