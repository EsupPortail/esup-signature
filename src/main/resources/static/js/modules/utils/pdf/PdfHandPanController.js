export class PdfHandPanController {

    constructor(viewer) {
        this.viewer = viewer;
    }

    initHandPan() {
        const workspace = this.viewer.getWorkspaceElement();
        if (!workspace || workspace.dataset.esHandPanBound === 'true') {
            return;
        }
        workspace.dataset.esHandPanBound = 'true';
        this.ensureHandPanStyles();
        this.ensureHandPanOverlay();

        const setHandPanCursor = (cursor, target = null) => {
            document.body.classList.toggle('es-hand-pan-grab', cursor === 'grab');
            document.body.classList.toggle('es-hand-pan-grabbing', cursor === 'grabbing');
            workspace.classList.toggle('es-hand-pan-grab', cursor === 'grab');
            workspace.classList.toggle('es-hand-pan-grabbing', cursor === 'grabbing');
            if (this.viewer.pdfDiv?.length) {
                this.viewer.pdfDiv.toggleClass('es-hand-pan-grab', cursor === 'grab');
                this.viewer.pdfDiv.toggleClass('es-hand-pan-grabbing', cursor === 'grabbing');
            }
            workspace.style.cursor = cursor;
            const previousTarget = workspace._esHandPanCursorTarget;
            if (previousTarget instanceof Element && previousTarget !== target) {
                previousTarget.style.cursor = '';
            }
            if (target instanceof Element) {
                target.style.cursor = cursor;
                workspace._esHandPanCursorTarget = target;
            } else {
                workspace._esHandPanCursorTarget = null;
            }
        };

        const refreshHandPanCursor = (event = null) => {
            if (this.viewer.handPanState) {
                const draggingTarget = workspace._esHandPanCursorTarget instanceof Element
                    ? workspace._esHandPanCursorTarget
                    : this.viewer.pdfDiv?.get?.(0);
                setHandPanCursor('grabbing', draggingTarget);
                return;
            }
            const target = event?.target ?? document.elementFromPoint(window.innerWidth / 2, window.innerHeight / 2);
            if (target instanceof Element && this.canStartHandPan({
                button: 0,
                defaultPrevented: false,
                target: target,
            })) {
                const pannableTarget = target.closest('.pdf-page, .page, .canvasWrapper, canvas');
                setHandPanCursor('grab', pannableTarget);
                return;
            }
            setHandPanCursor('');
        };

        workspace.addEventListener('mousedown', event => {
            if (!this.canStartHandPan(event)) {
                return;
            }
            this.viewer.handPanState = {
                startClientX: event.clientX,
                startClientY: event.clientY,
                startScrollLeft: this.viewer.getScrollLeft(),
                startScrollTop: this.viewer.getScrollTop(),
                dragging: false,
            };
            const pannableTarget = event.target instanceof Element
                ? event.target.closest('.pdf-page, .page, .canvasWrapper, canvas')
                : null;
            setHandPanCursor('grabbing', pannableTarget);
        });

        workspace.addEventListener('mousemove', refreshHandPanCursor);
        workspace.addEventListener('mouseenter', refreshHandPanCursor);

        document.addEventListener('mousemove', event => {
            if (!this.viewer.handPanState) {
                return;
            }

            const deltaX = event.clientX - this.viewer.handPanState.startClientX;
            const deltaY = event.clientY - this.viewer.handPanState.startClientY;
            if (!this.viewer.handPanState.dragging) {
                if (Math.abs(deltaX) < 4 && Math.abs(deltaY) < 4) {
                    return;
                }
                this.viewer.handPanState.dragging = true;
                setHandPanCursor('grabbing', workspace._esHandPanCursorTarget);
                document.body.classList.add('user-select-none');
            }

            event.preventDefault();
            if (workspace) {
                workspace.scrollLeft = this.viewer.handPanState.startScrollLeft - deltaX;
                workspace.scrollTop = this.viewer.handPanState.startScrollTop - deltaY;
            } else {
                window.scrollTo({
                    left: this.viewer.handPanState.startScrollLeft - deltaX,
                    top: this.viewer.handPanState.startScrollTop - deltaY,
                    behavior: 'auto'
                });
            }
        }, { passive: false });

        const stopHandPan = () => {
            if (!this.viewer.handPanState) {
                return;
            }
            this.viewer.handPanState = null;
            document.body.classList.remove('user-select-none');
            refreshHandPanCursor();
        };

        document.addEventListener('mouseup', stopHandPan);
        workspace.addEventListener('mouseleave', () => {
            if (!this.viewer.handPanState) {
                setHandPanCursor('');
            }
        });
        window.addEventListener('blur', stopHandPan);
    }

    ensureHandPanStyles() {
        if (document.getElementById('pdf-hand-pan-style')) {
            return;
        }
        const style = document.createElement('style');
        style.id = 'pdf-hand-pan-style';
        style.appendChild(document.createTextNode(`
            #workspace.es-hand-pan-grab,
            #workspace.es-hand-pan-grab #pdf,
            #workspace.es-hand-pan-grab .pdf-page,
            #workspace.es-hand-pan-grab .page,
            #workspace.es-hand-pan-grab .canvasWrapper,
            #workspace.es-hand-pan-grab canvas {
                cursor: grab !important;
            }
            body.es-hand-pan-grabbing,
            body.es-hand-pan-grabbing #workspace,
            body.es-hand-pan-grabbing #workspace *,
            #workspace.es-hand-pan-grabbing,
            #workspace.es-hand-pan-grabbing *,
            #pdf.es-hand-pan-grabbing,
            #pdf.es-hand-pan-grabbing * {
                cursor: grabbing !important;
            }
            #pdf-hand-pan-overlay {
                position: fixed;
                inset: 0;
                display: none;
                background: transparent;
                cursor: grabbing !important;
                z-index: 2147483647;
            }
            body.es-hand-pan-grabbing #pdf-hand-pan-overlay {
                display: block;
            }
        `));
        document.head.appendChild(style);
    }

    ensureHandPanOverlay() {
        let overlay = document.getElementById('pdf-hand-pan-overlay');
        if (overlay == null) {
            overlay = document.createElement('div');
            overlay.id = 'pdf-hand-pan-overlay';
            overlay.setAttribute('aria-hidden', 'true');
            document.body.appendChild(overlay);
        }
        this.viewer.handPanOverlay = overlay;
    }

    canStartHandPan(event) {
        if (event.button !== 0 || event.defaultPrevented) {
            return false;
        }
        if (this.viewer.isRendering || this.viewer.pendingRender) {
            return false;
        }
        const target = event.target;
        if (!(target instanceof Element)) {
            return false;
        }
        const interactiveSelector = [
            'input',
            'textarea',
            'select',
            'option',
            'button',
            'label',
            'a',
            '[contenteditable="true"]',
            '.annotationLayer',
            '.annotationLayer *',
            '.annotationEditorLayer',
            '.annotationEditorLayer *',
            '.linkAnnotation',
            '.linkAnnotation *',
            '.textLayer span',
            '.textLayer br',
            '.ui-draggable',
            '.ui-resizable-handle',
            '.sign-space',
            '.sign-space *',
            '.spot',
            '.spot *',
            '.postit',
            '.postit *',
            '.toggle-layer-btn',
            '.display-layer-btn',
            '.pdf-link-test-btn',
            '.pdf-link-display-btn',
            '.search-completion',
            '.custom-autocompletion',
            '.custom-autocompletion *'
        ].join(', ');

        if (target.closest(interactiveSelector)) {
            return false;
        }

        const pageContainer = target.closest('.pdf-page, .page, .canvasWrapper, canvas, .textLayer');
        return pageContainer != null;
    }
}
