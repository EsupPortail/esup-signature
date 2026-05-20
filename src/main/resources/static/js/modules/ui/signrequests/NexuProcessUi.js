import {Nexu} from "./Nexu.js?version=@version@";

export class NexuProcessUi {

    constructor(rootElement) {
        this.rootElement = rootElement;
        this.root = $(rootElement);
        this.nexu = null;
    }

    static collectRootElements(container = document) {
        const containerElement = container instanceof jQuery ? container.get(0) : container;
        if (containerElement == null) {
            return [];
        }
        const roots = [];
        if (containerElement.matches?.('.js-nexu-process')) {
            roots.push(containerElement);
        }
        const nestedRoots = containerElement.querySelectorAll?.('.js-nexu-process') ?? [];
        roots.push(...nestedRoots);
        return roots;
    }

    static initWithin(container = document) {
        this.collectRootElements(container).forEach(rootElement => {
            if (rootElement.dataset.nexuInitialized === 'true') {
                return;
            }
            new NexuProcessUi(rootElement).init();
        });
    }

    parseBoolean(value, fallback = false) {
        if (value == null || value === '') {
            return fallback;
        }
        if (typeof value === 'boolean') {
            return value;
        }
        return `${value}`.toLowerCase() === 'true';
    }

    parseNullableLong(value) {
        if (value == null || value === '') {
            return null;
        }
        const parsedValue = Number.parseInt(value, 10);
        return Number.isNaN(parsedValue) ? null : parsedValue;
    }

    getIds() {
        return this.root.find('.nexu-id').get()
            .map(input => Number.parseInt(input.value, 10))
            .filter(id => !Number.isNaN(id));
    }

    getConfig() {
        const {dataset} = this.rootElement;
        return {
            addExtra: this.parseBoolean(dataset.nexuAddExtra, false),
            ids: this.getIds(),
            urlProfil: dataset.nexuUrlProfil || 'user',
            massSignReportId: this.parseNullableLong(dataset.nexuMassSignReportId),
            rootUrl: dataset.nexuRootUrl || window.location.origin,
            fullScreen: this.parseBoolean(dataset.nexuFullscreen, false)
        };
    }

    startNexu() {
        const {addExtra, ids, urlProfil, massSignReportId, rootUrl} = this.getConfig();
        this.nexu = new Nexu(addExtra, ids, 'nexuSign', urlProfil, massSignReportId, rootUrl, this.rootElement);
    }

    handleReloadRequest() {
        const {fullScreen, ids} = this.getConfig();
        if (fullScreen) {
            window.location.reload();
            return;
        }
        this.rootElement.dispatchEvent(new CustomEvent('nexuProcessReloadRequested', {
            bubbles: true,
            detail: {ids}
        }));
    }

    init() {
        this.rootElement.dataset.nexuInitialized = 'true';
        this.startNexu();
        this.root.find('#refresh-certType2').on('click', () => this.startNexu());
        this.root.find('.js-nexu-process-reload').on('click', () => this.handleReloadRequest());
    }

}



