$('tr > td > a > span').on('click', function(e) {
    if (jQuery.inArray('small_flag', e.target.classList) !== -1) {
        e.stopPropagation();
    }
});
