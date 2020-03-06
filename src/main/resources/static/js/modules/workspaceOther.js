export class WorkspaceOther {
    mode = 'sign';

    constructor() {
    }

    init() {
        if (localStorage.getItem('mode') != null && localStorage.getItem('mode') != "") {
            mode = localStorage.getItem('mode');
        } else {
            localStorage.setItem('mode', 'sign');
        }
        if (mode == 'sign') {
            enableSignMode();
        } else if (mode == 'refuse') {
            enableRefuseMode();
        } else {
            enableSignMode();
        }
    }

    enableSignMode() {
        disableAllModes();
        mode = 'sign';
        localStorage.setItem('mode', 'sign');
        pointItEnable = false;
        $('#workspace').toggleClass('alert-success alert-secondary');
        $('#signButton').toggleClass('btn-light btn-success');
        $('#signtools').show();
        $('#stepscard').show();
        $('#infos').show();
    }

    enableRefuseMode() {
        disableAllModes();
        mode = 'refuse';
        localStorage.setItem('mode', 'refuse');
        $('#workspace').toggleClass('alert-danger alert-secondary');
        $('#refuseButton').toggleClass('btn-light btn-danger');
        $('#refusetools').show();
        $('#infos').show();
    }

    disableAllModes() {
        $('#pen').hide();
        $('#clock').hide();
        mode = 'sign';
        $('#workspace').removeClass('alert-danger').removeClass('alert-warning').removeClass('alert-success').addClass('alert-secondary');
        $('#commentButton').addClass('btn-light').removeClass('btn-warning');
        $('#signButton').addClass('btn-light').removeClass('btn-success');
        $('#refuseButton').addClass('btn-light').removeClass('btn-danger');
        $('#readButton').addClass('btn-light').removeClass('btn-secondary');
        $('#commentsTools').hide();
        $('#stepscard').hide();
        $('#signtools').hide();
        $('#cross').hide();
        $('#infos').hide();
        $('#refusetools').hide();
        $('#rotateleft').prop('disabled', true);
        $('#rotateright').prop('disabled', true);
    }

}