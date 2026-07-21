(() => {
            const addCommentForm = document.getElementById('add-comment-form');
            const addCommentModalElement = document.getElementById('addCommentModal');
            if (!addCommentForm || !addCommentModalElement) {
                return;
            }

            const signRequestId = document.body.dataset.esupSignrequestId || '0';
            const urlProfil = document.body.dataset.esupSignrequestProfilePath || 'user';
            const userEppn = document.body.dataset.esupUserEppn || '';
            const csrfParameterName = document.querySelector('meta[name="_csrf_parameter"]')?.getAttribute('content') || '_csrf';
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';

            function escapeHtml(value) {
                return String(value ?? '')
                    .replace(/&/g, '&amp;')
                    .replace(/</g, '&lt;')
                    .replace(/>/g, '&gt;')
                    .replace(/"/g, '&quot;')
                    .replace(/'/g, '&#039;');
            }

            function showSnackbar(message, type = 'info') {
                if (typeof window.showSnackbar === 'function') {
                    window.showSnackbar(escapeHtml(message), type, {
                        delay: type === 'dark' ? 2000 : 4000
                    });
                    return;
                }
                console.info(message);
            }

            function formatPostitDate(timestamp) {
                if (!timestamp) {
                    return '';
                }
                return new Intl.DateTimeFormat('fr-FR', {
                    day: '2-digit',
                    month: '2-digit',
                    year: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit'
                }).format(new Date(timestamp));
            }

            function getPostitLayer() {
                return document.querySelector('.es-signrequest-postits-layer');
            }

            function getPostitIndex() {
                return document.querySelectorAll('.es-signrequest-postits-layer > [id^="postit-"]:not(#postit-comment)').length;
            }

            function buildPostitHtml(postit) {
                const index = getPostitIndex();
                const left = 310 + (index * 15);
                const top = 310 + (index * 70);
                const authorName = `${postit.createBy?.firstname || ''} ${postit.createBy?.name || ''}`.trim();
                const escapedText = escapeHtml(postit.text);
                const escapedColor = escapeHtml(postit.postitColor || '#FFF740');
                const canEdit = Boolean(postit.canEdit) && !postit.refuse && postit.createBy?.eppn === userEppn;

                return `
                    <div id="postit-${postit.id}" class="postit-global overflow-hidden"
                         style="width: 215px; height:215px; z-index: 1001; cursor: move; background-color: ${escapedColor}; position: fixed; left: ${left}px; top: ${top}px;">
                        <button type="button" class="postit-global-close close">
                            <i class="fi fi-rr-eye-crossed"></i>
                        </button>
                        <b>${escapeHtml(authorName)}</b><br>
                        <span style="font-size: 12px; width: fit-content;">${escapeHtml(formatPostitDate(postit.createDate))}</span>
                        <button data-es-postit-id="${postit.id}" type="button" title="Copier le texte du postit" class="postit-copy btn btn-sm btn-transparent text-dark">
                            <i data-es-postit-id="${postit.id}" class="fi fi-rr-copy"></i>
                        </button>
                        ${canEdit ? `
                            <div class="es-postit-edit-form" id="postit-${postit.id}" data-postit-id="${postit.id}">
                                <textarea style="color: var(--bs-black); width: 100%;height: 75%;resize:none;background-color: ${escapedColor}" name="comment" id="postit-text-${postit.id}" class="postitarea">${escapedText}</textarea>
                                <button type="button" data-postit-id="${postit.id}" class="btn btn-sm btn-dark float-end position-absolute es-postit-save-btn postit-save-ajax" title="Enregistrer"><i class="fi fi-rr-disk"></i></button>
                            </div>
                            <button type="button" data-postit-id="${postit.id}" class="btn btn-sm btn-dark float-start position-absolute es-postit-delete-btn postit-delete-ajax" title="Supprimer">
                                <i class="fi fi-rr-trash"></i>
                            </button>`
                            : `<p id="postit-text-${postit.id}" class="postitarea">${escapedText}</p>`}
                    </div>`;
            }

            function toggleCompactMode(postitElement) {
                const $postit = $(postitElement);
                if (typeof $postit.resizable === 'function') {
                    try {
                        $postit.resizable($postit.hasClass('postit-small') ? 'enable' : 'disable');
                    } catch (e) {
                        // ignore partially initialized widget states
                    }
                }
                $postit.toggleClass('postit-small');
                $postit.find('button').each(function () {
                    if (!$(this).hasClass('postit-global-close')) {
                        $(this).toggle();
                    }
                });
            }

            async function copyPostitText(postitId) {
                const postitTextNode = document.getElementById(`postit-text-${postitId}`);
                const text = postitTextNode?.value ?? postitTextNode?.textContent ?? '';
                try {
                    if (navigator.clipboard?.writeText) {
                        await navigator.clipboard.writeText(text);
                        showSnackbar('Texte copié dans le presse-papier', 'success');
                        return;
                    }
                } catch (e) {
                    console.warn('Clipboard fallback', e);
                }

                const tempTextarea = document.createElement('textarea');
                tempTextarea.value = text;
                document.body.appendChild(tempTextarea);
                tempTextarea.style.position = 'absolute';
                tempTextarea.style.left = '-9999px';
                tempTextarea.select();
                document.execCommand('copy');
                document.body.removeChild(tempTextarea);
                showSnackbar('Texte copié dans le presse-papier', 'success');
            }

            function initRenderedPostit(postitElement) {
                if (!postitElement) {
                    return;
                }
                $(postitElement).find('.postit-global-close').off('click.renderedPostit').on('click.renderedPostit', () => toggleCompactMode(postitElement));
                $(postitElement).find('.postit-copy').off('click.renderedPostit').on('click.renderedPostit', event => {
                    const postitId = event.currentTarget.getAttribute('data-es-postit-id') || event.target.getAttribute('data-es-postit-id');
                    copyPostitText(postitId);
                });

                if (typeof $(postitElement).draggable === 'function') {
                    $(postitElement).draggable();
                }
                if (typeof $(postitElement).resizable === 'function') {
                    $(postitElement).resizable({
                        aspectRatio: false,
                        minWidth: 215,
                        minHeight: 215,
                        resize: function () {
                            const currentPostitArea = $(this).find('.postitarea').first().get(0);
                            const parent = currentPostitArea?.closest('.postit-global');
                            if (!currentPostitArea || !parent) {
                                return;
                            }
                            const lineHeight = parseFloat(window.getComputedStyle(currentPostitArea).lineHeight);
                            if (!Number.isFinite(lineHeight) || lineHeight <= 0) {
                                return;
                            }
                            currentPostitArea.style.webkitLineClamp = Math.floor(parent.clientHeight / lineHeight);
                        }
                    });
                }

                $(postitElement).off('mousedown.renderedPostit').on('mousedown.renderedPostit', function () {
                    const currentPostitId = this.id;
                    $('.es-signrequest-postits-layer > [id^="postit-"]:not(#postit-comment)').each(function () {
                        $(this).css('z-index', this.id === currentPostitId ? 1001 : 1000);
                    });
                });
            }

            async function fetchJson(url, options = {}) {
                const response = await fetch(url, {
                    ...options,
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest',
                        'Accept': 'application/json',
                        [csrfHeader]: csrfToken,
                        ...(options.headers || {})
                    }
                });
                const payload = await response.json();
                if (!response.ok) {
                    throw new Error(payload.message || 'Une erreur est survenue');
                }
                return payload;
            }

            addCommentForm.addEventListener('submit', async event => {
                event.preventDefault();
                try {
                    const payload = await fetchJson(addCommentForm.action, {
                        method: 'POST',
                        body: new FormData(addCommentForm)
                    });
                    if (payload.postit) {
                        const postitLayer = getPostitLayer();
                        if (postitLayer) {
                            const wrapper = document.createElement('div');
                            wrapper.innerHTML = buildPostitHtml(payload.postit).trim();
                            const postitElement = wrapper.firstElementChild;
                            postitLayer.appendChild(postitElement);
                            initRenderedPostit(postitElement);
                        }
                    }
                    addCommentForm.reset();
                    bootstrap.Modal.getInstance(addCommentModalElement)?.hide();
                    showSnackbar(payload.message || 'Post-it ajouté', 'success');
                } catch (error) {
                    showSnackbar(error.message || 'Erreur lors de l’ajout du post-it', 'error');
                }
            });
        })();
