export class PdfLayerController {

    constructor(viewer) {
        this.viewer = viewer;
    }

    async applyLinkAnnotationsVisibility() {
        if (!this.viewer.pdfDoc) {
            return;
        }
        const config = await Promise.resolve(this.viewer._optionalContentConfigPromise);
        if (!config) {
            return;
        }

        const visibleLayerNames = new Set();
        for (const [id, group] of config) {
            if (group?.visible) {
                visibleLayerNames.add(group.name);
            }
        }

        const tasks = [];
        for (let pageNum = 1; pageNum <= this.viewer.numPages; pageNum++) {
            tasks.push(this.applyLinkAnnotationsVisibilityForPage(pageNum, visibleLayerNames));
        }
        await Promise.all(tasks);
    }

    async applyLinkAnnotationsVisibilityForPage(pageNum, visibleLayerNames) {
        const pageContainer = document.getElementById(`page_${pageNum}`);
        if (!pageContainer) {
            return;
        }
        const annotationLayer = pageContainer.querySelector('.annotationLayer');
        if (!annotationLayer) {
            return;
        }

        const page = await this.viewer.pdfDoc.getPage(pageNum);
        const annotations = await page.getAnnotations();
        const annotationsById = new Map();
        annotations.forEach(annotation => {
            if (annotation?.id != null) {
                annotationsById.set(String(annotation.id), annotation);
            }
        });

        annotationLayer.querySelectorAll('section.linkAnnotation[data-annotation-id]').forEach(section => {
            const annotation = annotationsById.get(String(section.dataset.annotationId));
            if (!annotation) {
                section.style.removeProperty('display');
                return;
            }

            const layerIds = this.extractAnnotationLayerIds(annotation);
            if (!layerIds.length) {
                section.style.removeProperty('display');
                return;
            }

            const shouldDisplay = layerIds.some(layerId => visibleLayerNames.has(layerId));
            section.style.display = shouldDisplay ? '' : 'none';
        });
    }

    extractAnnotationLayerIds(annotation) {
        const layerIds = new Set();
        if (!annotation) {
            return [];
        }

        [annotation.annotationName, annotation.name, annotation.ocgName, annotation.layerName].forEach(value => {
            if (typeof value === 'string' && value.trim()) {
                layerIds.add(value.trim());
            }
        });

        [annotation.contents, annotation.contentsObj?.str, annotation.title].forEach(value => {
            if (typeof value !== 'string' || !value.trim()) {
                return;
            }
            try {
                const parsed = JSON.parse(value);
                if (parsed?.layer_id) {
                    layerIds.add(String(parsed.layer_id).trim());
                }
            } catch (e) {
                const match = value.match(/"layer_id"\s*:\s*"([^"]+)"/);
                if (match?.[1]) {
                    layerIds.add(match[1].trim());
                }
            }
        });

        return Array.from(layerIds);
    }

    isApplicationLayerName(layerName) {
        if (typeof layerName !== 'string' || !layerName.trim()) {
            return false;
        }
        return /^layer_\d+$/.test(layerName)
            || /^sign_\d+_.+/.test(layerName)
            || /^SignStep_\d+_.+/.test(layerName);
    }

    getApplicationLayers(config) {
        const layers = [];
        if (!config) {
            return layers;
        }
        for (const [id, group] of config) {
            if (group?.name && this.isApplicationLayerName(group.name)) {
                layers.push({ id, name: group.name, visible: group.visible });
            }
        }
        return layers;
    }

    resolveLayerName(stepNumber, requestedLayerName, layers) {
        if (requestedLayerName) {
            const exactMatch = layers.find(group => group.name === requestedLayerName);
            if (exactMatch) {
                return exactMatch.name;
            }
            const requestedLayerId = Number.parseInt(String(requestedLayerName).replace('layer_', ''), 10);
            if (!Number.isNaN(requestedLayerId)) {
                const compatibleMatch = layers.find(group => {
                    const liveWorkflowStepId = this.extractStableLayerStepId(group.name);
                    return liveWorkflowStepId === requestedLayerId;
                });
                if (compatibleMatch) {
                    return compatibleMatch.name;
                }
            }

            return null;
        }
        return layers[stepNumber - 1]?.name || null;
    }

    extractStableLayerStepId(layerName) {
        if (typeof layerName !== 'string') {
            return null;
        }
        let match = layerName.match(/^layer_(\d+)$/);
        if (match) {
            return Number.parseInt(match[1], 10);
        }
        match = layerName.match(/^sign_(\d+)_/);
        if (match) {
            return Number.parseInt(match[1], 10);
        }
        return null;
    }

    async showLayerByStep(stepNumber, solo, layerName = null) {
        if (!this.viewer.pdfDoc) {
            return;
        }
        try {
            const config = await this.viewer.pdfDoc.getOptionalContentConfig().catch(e => {
                console.error("Error getting optional content config: " + e);
                return null;
            });
            if (!config) {
                return;
            }
            const allGroups = this.getApplicationLayers(config);
            const resolvedLayerName = this.resolveLayerName(stepNumber, layerName, allGroups);
            if (!resolvedLayerName && stepNumber !== 0) {
                console.warn(`showLayerByStep: layer introuvable pour step=${stepNumber}, layerName=${layerName}`);
                return;
            }
            if(solo) {
                allGroups.forEach((group) => {
                    const shouldBeVisible = group.name === resolvedLayerName;
                    config.setVisibility(group.id, shouldBeVisible);
                });
            } else {
                allGroups.forEach((group) => {
                    const selectedGroupIndex = allGroups.findIndex(candidate => candidate.name === resolvedLayerName);
                    const currentGroupIndex = allGroups.findIndex(candidate => candidate.name === group.name);
                    const shouldBeVisible = stepNumber === 0 ? false : (selectedGroupIndex >= 0 && currentGroupIndex <= selectedGroupIndex);
                    config.setVisibility(group.id, shouldBeVisible);
                });
            }
            this.viewer.optionalContentConfigPromise = Promise.resolve(config);
            this.viewer._activeLayerView = { stepNumber, solo, layerId: resolvedLayerName };
            this.updateLayerButtonsState();
        } catch(err) {
            console.error('Erreur showLayerByStep:', err);
        }
    }

    updateLayerButtonsState() {
        const onClasses = ['active', 'bg-primary-subtle', 'text-primary', 'border', 'border-primary-subtle'];
        $('.display-layer-btn, .toggle-layer-btn').each(function () {
            onClasses.forEach(c => $(this).removeClass(c));
        });

        if (!this.viewer._activeLayerView) {
            return;
        }

        const selector = this.viewer._activeLayerView.solo
            ? `.toggle-layer-btn[data-layer-id="${this.viewer._activeLayerView.layerId}"]`
            : `.display-layer-btn[data-layer-id="${this.viewer._activeLayerView.layerId}"]`;

        const $btn = $(selector);
        if ($btn.length) {
            onClasses.forEach(c => $btn.addClass(c));
        }
    }

    async showAllLayers() {
        if (!this.viewer.pdfDoc) {
            return;
        }
        try {
            const config = await this.viewer.pdfDoc.getOptionalContentConfig().catch(e => {
                console.error("Error getting optional content config: " + e);
                return null;
            });
            if (!config) {
                return;
            }
            for (const group of this.getApplicationLayers(config)) {
                config.setVisibility(group.id, true);
            }
            this.viewer.optionalContentConfigPromise = Promise.resolve(config);
            this.viewer._activeLayerView = null;
            this.updateLayerButtonsState();
        } catch(err) {
            console.error('Erreur showAllLayers:', err);
        }
    }

    async toggleLayerByStep(stepNumber, solo, layerId = null) {
        if (this.viewer._activeLayerView
            && this.viewer._activeLayerView.solo === solo
            && this.viewer._activeLayerView.layerId === (layerId || this.viewer._activeLayerView.layerId)) {
            await this.showAllLayers();
            return;
        }
        await this.showLayerByStep(stepNumber, solo, layerId);
    }

    async highlightStep(stepNumber, layerId = null) {
        if (!this.viewer.highlighter) {
            console.error('highlightStep: LayerHighlighter non initialisé');
            return;
        }

        try {
            const config = await Promise.resolve(this.viewer._optionalContentConfigPromise).catch(e => {
                console.error("Error resolving optionalContentConfigPromise: " + e);
                return null;
            });
            if (!config) {
                console.warn('highlightStep: Aucun calque disponible');
                return;
            }
            const allGroups = this.getApplicationLayers(config);

            const resolvedLayerName = this.resolveLayerName(stepNumber, layerId, allGroups);
            const targetGroup = resolvedLayerName
                ? allGroups.find(group => group.name === resolvedLayerName)
                : null;
            if (!targetGroup) {
                console.warn(`highlightStep: layer introuvable pour step=${stepNumber}, layerId=${layerId}`);
                return;
            }
            this.viewer.highlighter.clearHighlights();
            await this.viewer.highlighter.highlightLayer(targetGroup.id);
            console.log(`highlightStep(${stepNumber}): Calque "${targetGroup.name}" en ${this.viewer.highlighter.highlightColor}`);
        } catch(err) {
            console.error('highlightStep error:', err);
        }
    }

    clearHighlight() {
        if (this.viewer.highlighter) {
            this.viewer.highlighter.clearHighlights();
            console.log('Highlight effacé');
        }
    }
}
