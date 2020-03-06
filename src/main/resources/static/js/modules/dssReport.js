export class DssReport {

    divs = document.getElementsByTagName("div");
    spans = document.getElementsByTagName("span");

    constructor() {
        this.hackDivs();
        this.hackSpans();
    }

    hackDivs() {
        [].forEach.call(this.divs, function(div) {
            if (div.classList.contains('panel')) {
                div.classList.remove('panel');
                div.classList.add('card');
            }

            if (div.classList.contains('panel-heading')) {
                div.classList.remove('panel-heading');
                div.classList.add('card-header');
                div.innerHTML = '<i class="fas fa-plus"><!-- --></i>' + div.innerHTML;
            }

            if (div.classList.contains('panel-body')) {
                div.classList.remove('panel-body');
                div.classList.add('card-body');
                div.classList.add('bg-light');
            }

            if (div.classList.contains('panel-primary')) {
                div.classList.remove('panel-primary');
                div.classList.add('bg-light');
            }

            if (div.classList.contains('panel-success')) {
                div.classList.remove('panel-success');
                div.classList.add('bg-success');
            }

            if (div.classList.contains('panel-warning')) {
                div.classList.remove('panel-warning');
                div.classList.add('bg-warning');
            }

            if (div.classList.contains('panel-danger')) {
                div.classList.remove('panel-danger');
                div.classList.add('bg-danger');
            }
        });
    }

    hackSpans() {
        [].forEach.call(this.spans, function(span) {
            if (span.classList.contains('glyphicon-ok-sign')) {
                span.classList.remove('glyphicon');
                span.classList.remove('glyphicon-ok-sign');
                span.classList.add('fas');
                span.classList.add('fa-check-circle');
            }
            if (span.classList.contains('glyphicon-remove-sign')) {
                span.classList.remove('glyphicon');
                span.classList.remove('glyphicon-remove-sign');
                span.classList.add('fas');
                span.classList.add('fa-times-circle');

            }

        });
    }
}