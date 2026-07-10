$(document).ready(function() {
    let isHidden = true;

    $('#toggleAnonymous').click(function() {
        $('tr.table-secondary').toggleClass('d-none');

        isHidden = !isHidden;
        const icon = isHidden ? 'fi fi-rr-eye-crossed' : 'fi fi-rr-eye';
        const text = isHidden ? 'Afficher les sessions anonymes' : 'Masquer les sessions anonymes';

        $(this).find('i').attr('class', icon);
        $(this).empty()
            .append($('<i>').attr('class', icon))
            .append(document.createTextNode(' ' + text));
    });

    $('.js-confirm-submit').on('submit', function(event) {
        if (!window.confirm($(this).data('confirmMessage') || 'Confirmer cette action ?')) {
            event.preventDefault();
        }
    });
});
