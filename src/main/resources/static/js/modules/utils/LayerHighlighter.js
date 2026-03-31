/**
 * LayerHighlighter - Filtre et highlight les annotations par calque OCG
 * Utilise annotationName pour matcher avec le calque
 */

class LayerHighlighter {
    constructor(viewer) {
        this.viewer = viewer;
        this.highlightedLayers = new Set();
        this.highlightColor = '#9cdf7f';
        this.highlightOpacity = 0.3;
    }

    /**
     * Récupérer les annotation IDs d'un calque spécifique
     * Utilise annotationName pour matcher
     */
    async getAnnotationIdsForLayer(pageNum, targetLayerId) {
        if (!this.viewer.pdfDoc) {
            console.error('PDF non chargé');
            return [];
        }

        try {
            const page = await this.viewer.pdfDoc.getPage(pageNum);
            const annotations = await page.getAnnotations();

            console.log(`\n[Page ${pageNum}] ${annotations.length} annotation(s) trouvée(s)`);

            const matchingAnnotationIds = [];

            for (const annotation of annotations) {
                const annotationId = annotation.id;
                const contents =
                    annotation.contentsObj?.str ||
                    annotation.contents ||
                    annotation.contentsObj ||
                    '';
                console.log(`\n  Annotation ${annotationId}:`);
                console.log(`    - contents:`, contents);
                let layerIdFromContent = null;
                try {
                    const parsed = JSON.parse(contents);
                    layerIdFromContent = parsed.layer_id;
                } catch (e) {
                    // pas du JSON → ignorer
                }
                if (layerIdFromContent === targetLayerId) {
                    console.log(`    ✓ MATCH avec calque ${targetLayerId}!`);
                    matchingAnnotationIds.push(annotationId);
                }
            }

            return matchingAnnotationIds;

        } catch (err) {
            console.error(`Erreur page ${pageNum}:`, err);
            return [];
        }
    }

    /**
     * Highlight un calque
     */
    async highlightLayer(groupId, color = null) {
        if (color) this.highlightColor = color;

        const config = await this.viewer.optionalContentConfigPromise;
        if (!config) {
            console.error('Aucun calque disponible');
            return;
        }

        // Trouver le groupe
        let groupName = null;
        for (const [id, group] of config) {
            if (id === groupId) {
                groupName = group.name;
                break;
            }
        }

        if (!groupName) {
            console.error(`Groupe ${groupId} non trouvé`);
            return;
        }

        console.log(`\n🎯 HIGHLIGHT CALQUE: ${groupName} (ID: ${groupId})`);
        this.highlightedLayers.add(groupId);

        // Récupérer les annotations de ce calque pour chaque page
        const allAnnotationIds = [];
        for (let pageNum = 1; pageNum <= this.viewer.numPages; pageNum++) {
            console.log(`\n--- PAGE ${pageNum} ---`);
            const pageAnnotationIds = await this.getAnnotationIdsForLayer(pageNum, groupName);
            allAnnotationIds.push(...pageAnnotationIds);
        }

        if (allAnnotationIds.length === 0) {
            console.log(`\n❌ Aucune annotation trouvée pour le calque ${groupId}`);
            return;
        }

        console.log(`\n✓ Total: ${allAnnotationIds.length} annotation(s) trouvée(s)`);

        // Highlight dans le DOM
        this._highlightAnnotationsInDOM(groupId, allAnnotationIds);
    }

    /**
     * Highlight les annotations spécifiques dans le DOM
     */
    _highlightAnnotationsInDOM(groupId, annotationIds) {
        let highlightCount = 0;
        const pages = document.querySelectorAll('.pdf-page');

        console.log(`\nCherchant ${annotationIds.length} annotation(s) dans le DOM...`);
        let self = this;
        pages.forEach((page) => {
            const annotationLayer = page.querySelector('.annotationLayer');
            if (!annotationLayer) {
                return;
            }

            annotationIds.forEach(annotationId => {
                const section = annotationLayer.querySelector(`section[data-annotation-id="${annotationId}"]`);

                if (section) {
                    section.classList.add(`highlight-layer-${groupId}`);
                    section.style.outline = `2px solid ${this.highlightColor}`;
                    section.style.borderRadius = `3px`;
                    section.style.backgroundColor = this._hexToRgba(this.highlightColor, this.highlightOpacity);
                    console.log(`  ✓ Highlighted annotation ${annotationId}`);
                    highlightCount++;
                } else {
                    console.log(`  ⚠ Annotation ${annotationId} not found in DOM`);
                }
            });
        });

        console.log(`\n✓ Highlight appliqué: ${highlightCount} annotation(s) mis en évidence`);
    }

    /**
     * Highlight par nom
     */
    async highlightLayerByName(layerName, color = null) {
        const config = await this.viewer.optionalContentConfigPromise;
        if (!config) return;

        for (const [id, group] of config) {
            if (group.name === layerName) {
                await this.highlightLayer(id, color);
                return;
            }
        }

        console.error(`Calque "${layerName}" non trouvé`);
    }

    /**
     * Supprimer highlight
     */
    removeHighlight(groupId = null) {
        if (groupId) {
            this.highlightedLayers.delete(groupId);
            document.querySelectorAll(`.highlight-layer-${groupId}`).forEach(el => {
                el.classList.remove(`highlight-layer-${groupId}`);
                el.style.outline = '';
                el.style.outlineOffset = '';
                el.style.boxShadow = '';
                el.style.backgroundColor = '';
            });
        } else {
            this.highlightedLayers.forEach(id => {
                this.removeHighlight(id);
            });
            this.highlightedLayers.clear();
        }
    }

    /**
     * Supprimer tous les highlights
     */
    clearHighlights() {
        this.removeHighlight();
    }

    /**
     * Lister les calques
     */
    async listLayers() {
        const config = await this.viewer.optionalContentConfigPromise;
        if (!config) {
            console.log('Aucun calque disponible');
            return [];
        }

        const layers = [];
        for (const [id, group] of config) {
            layers.push({
                id: id,
                name: group.name,
                visible: group.visible
            });
        }

        console.table(layers);
        return layers;
    }

    /**
     * Convertir hex à rgba
     */
    _hexToRgba(hex, alpha) {
        const r = parseInt(hex.slice(1, 3), 16);
        const g = parseInt(hex.slice(3, 5), 16);
        const b = parseInt(hex.slice(5, 7), 16);
        return `rgba(${r}, ${g}, ${b}, ${alpha})`;
    }

    /**
     * Changer couleur
     */
    setHighlightColor(color, opacity = 0.3) {
        this.highlightColor = color;
        this.highlightOpacity = opacity;
    }
}

export { LayerHighlighter };