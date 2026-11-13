import NotificationCenter from "../NotificationCenter.js?version=@version@";

export class PostitManager {

	constructor(state, options = {}) {
		this.state = state;
		this.options = {
			eventNamespace: options.eventNamespace ?? ".postitManager",
			postitNamespace: options.postitNamespace ?? ".postitManagerPostit",
			postitSelector: options.postitSelector ?? ".postit-global",
			getComments: options.getComments ?? (() => []),
			snackbarSelector: options.snackbarSelector ?? "#snackbar"
		};
	}

	shouldDisplayOnLoad(comments = this.options.getComments()) {
		if (Array.isArray(comments) && comments.length > 0) {
			return true;
		}
		return document.querySelector(this.options.postitSelector) != null;
	}

	bind() {
		$(window)
			.off("resize" + this.options.eventNamespace)
			.on("resize" + this.options.eventNamespace, () => this.keepInViewport(true));
		$(document.getElementById("sidebar"))
			.off(this.options.eventNamespace)
			.on("shown.bs.offcanvas" + this.options.eventNamespace + " hidden.bs.offcanvas" + this.options.eventNamespace,
				() => this.keepInViewport(true));

		$(".postit-global-close")
			.off("click" + this.options.eventNamespace)
			.on("click" + this.options.eventNamespace, e => this.toggleCompactMode(e.currentTarget));

		$(".postit-copy")
			.off("click" + this.options.eventNamespace)
			.on("click" + this.options.eventNamespace, async e => this.copyPostitText(e));
	}

	applyVisibility(visible) {
		this.setVisibility(visible);
	}

	hideAll() {
		this.setVisibility(false);
	}

	showAll() {
		const postitSelector = this.options.postitSelector;
		const postitNamespace = this.options.postitNamespace;
		this.setVisibility(true);
		$(this.options.postitSelector).each((_, element) => {
			const postit = $(element);
			postit.off("mousedown" + postitNamespace);
			try {
				if (this.hasJqueryUiMethod(postit, "draggable") && postit.hasClass("ui-draggable")) {
					postit.draggable("destroy");
				}
			} catch (error) {
				// Ignore partially initialized draggable widgets.
			}
			if (this.hasJqueryUiMethod(postit, "draggable")) {
				postit.draggable({
					containment: "window",
					stop: () => this.keepInViewport()
				});
			}
			postit.on("mousedown" + postitNamespace, function () {
				let currentPostitId = $(this).attr("id");
				$(postitSelector).each(function () {
					$(this).css("z-index", $(this).attr("id") === currentPostitId ? 1001 : 1000);
				});
			});

			const postitArea = postit.find(".postitarea").first();
			postitArea.off("scroll" + postitNamespace).on("scroll" + postitNamespace, function () {
				$(this).addClass("postitarea-basic");
			});

			try {
				if (this.hasJqueryUiMethod(postit, "resizable") && postit.hasClass("ui-resizable")) {
					postit.resizable("destroy");
				}
			} catch (error) {
				// Ignore partially initialized resizable widgets.
			}
			if (this.hasJqueryUiMethod(postit, "resizable")) {
				postit.resizable({
					aspectRatio: false,
					minWidth: 215,
					minHeight: 215,
					stop: () => this.keepInViewport(),
					resize: function () {
						const currentPostit = $(this);
						const currentPostitArea = currentPostit.find(".postitarea").first().get(0);
						const parent = currentPostitArea?.closest(".postit-global");

						if (currentPostitArea && parent) {
							const lineHeight = parseFloat(window.getComputedStyle(currentPostitArea).lineHeight);
							if (!Number.isFinite(lineHeight) || lineHeight <= 0) {
								return;
							}
							const availableHeight = parent.clientHeight;
							currentPostitArea.style.webkitLineClamp = Math.floor(availableHeight / lineHeight);
						}
					}
				});
			}
		});
	}

	toggleCompactMode(button) {
		const postit = $(button).parent();
		if (this.hasJqueryUiMethod(postit, "resizable")) {
			if (postit.hasClass("postit-small")) {
				postit.resizable("enable");
			} else {
				postit.resizable("disable");
			}
		}
		postit.toggleClass("postit-small");
		postit.find("button").each(function () {
			if (!$(this).hasClass("postit-global-close")) {
				$(this).toggle();
			}
		});
		this.keepInViewport();
	}

	async copyPostitText(e) {
		const postitId = $(e.currentTarget).attr("es-postit-id") ?? $(e.target).attr("es-postit-id");
		const postitTextNode = $("#postit-text-" + postitId);
		const text = postitTextNode.val() ?? postitTextNode.text();
		try {
			if (navigator.clipboard && navigator.clipboard.writeText) {
				await navigator.clipboard.writeText(text);
				this.showSnackbarMessage("Texte copié dans le presse-papier");
			} else {
				this.copyWithFallback(text);
			}
		} catch (err) {
			console.warn("Erreur clipboard, utilisation du fallback :", err);
			this.copyWithFallback(text);
		}
	}

	showSnackbarMessage(message) {
		NotificationCenter.showSnackbar(message, "success");
	}

	setVisibility(visible) {
		$(this.options.postitSelector).each((_, element) => {
			$(element).toggleClass("d-none", !visible);
		});
		if (visible) {
			this.keepInViewport(true);
		}
	}

	keepInViewport(reposition = false) {
		const margin = 8;
		const sidebar = document.getElementById("sidebar");
		const sidebarStyle = sidebar ? window.getComputedStyle(sidebar) : null;
		const sidebarVisible = sidebarStyle && sidebarStyle.visibility !== "hidden" && sidebarStyle.display !== "none";
		const sidebarRight = sidebarVisible
			? (sidebarStyle.position === "fixed" ? sidebar.offsetLeft + sidebar.offsetWidth : sidebar.getBoundingClientRect().right)
			: 0;
		const initialLeft = Math.max(margin, sidebarRight + margin);
		const workspace = document.getElementById("es-id-signrequest-workspace");
		const initialTop = Math.max(margin, (workspace?.getBoundingClientRect().top ?? 0) + margin + 114);
		const availableWidth = Math.max(0, window.innerWidth - margin * 2);
		const availableHeight = Math.max(0, window.innerHeight - margin * 2);
		$(this.options.postitSelector).filter(":visible").each((index, element) => {
			if (window.getComputedStyle(element).position !== "fixed") {
				return;
			}
			const postit = $(element);
			if (postit.outerWidth() > availableWidth) {
				postit.outerWidth(availableWidth);
			}
			if (postit.outerHeight() > availableHeight) {
				postit.outerHeight(availableHeight);
			}
			const bounds = element.getBoundingClientRect();
			postit.css({
				left: Math.max(margin, Math.min(reposition ? initialLeft + index * 15 : bounds.left,
					window.innerWidth - bounds.width - margin)),
				top: Math.max(margin, Math.min(reposition ? initialTop + index * 70 : bounds.top,
					window.innerHeight - bounds.height - margin))
			});
		});
	}

	hasJqueryUiMethod(element, methodName) {
		return element != null && typeof element[methodName] === "function";
	}

	copyWithFallback(text) {
		let tempTextarea = document.createElement("textarea");
		tempTextarea.value = text;
		document.body.appendChild(tempTextarea);
		tempTextarea.style.position = "absolute";
		tempTextarea.style.left = "-9999px";
		tempTextarea.select();
		tempTextarea.setSelectionRange(0, 99999);

		try {
			let success = document.execCommand("copy");
			this.showSnackbarMessage(success ? "Texte copié..." : "Échec de la copie");
		} catch (error) {
			console.error("Impossible de copier le texte :", error);
			this.showSnackbarMessage("Erreur : Impossible de copier le texte");
		}

		document.body.removeChild(tempTextarea);
	}

	destroy() {
		$(window).off("resize" + this.options.eventNamespace);
		$(document.getElementById("sidebar")).off(this.options.eventNamespace);
		$(".postit-global-close").off(this.options.eventNamespace);
		$(".postit-copy").off(this.options.eventNamespace);
		$(this.options.postitSelector).each((_, element) => {
			const postit = $(element);
			postit.off(this.options.postitNamespace);
			postit.find(".postitarea").first().off(this.options.postitNamespace);
			try {
				if (postit.hasClass("ui-draggable")) {
					postit.draggable("destroy");
				}
			} catch (error) {
				// Ignore partially initialized draggable widgets.
			}
			try {
				if (postit.hasClass("ui-resizable")) {
					postit.resizable("destroy");
				}
			} catch (error) {
				// Ignore partially initialized resizable widgets.
			}
		});
	}

}

