/*
 * Licensed to ESUP-Portail under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * ESUP-Portail licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

$(document).ready(function () {
	$('#new-scroll').on('mousewheel DOMMouseScroll', function(event){

		var delta = Math.max(-1, Math.min(1, (event.originalEvent.wheelDelta || -event.originalEvent.detail)));

		$(this).scrollLeft( $(this).scrollLeft() - ( delta * 40 ) );
		event.preventDefault();

	});
	$(document).click(function (event) {
		var clickover = $(event.target);
		var _opened = $("#user-infos").hasClass("user-infos collapse show");
		console.log(clickover);
		console.log(_opened);
		if (_opened === true && !clickover.hasClass("user-toggle")) {
			$("#user-toggle").click();
		}
	});

	$(".clickable-row").click(function() {
		window.location = $(this).data("href");
	});

	var sideBarStatus = localStorage.getItem('sideBarStatus');

	if(sideBarStatus == null) {
		localStorage.setItem('sideBarStatus', 'on');
		sideBarStatus = localStorage.getItem('sideBarStatus');
	}

	if(sideBarStatus == 'off' && !$('#sidebar').hasClass('active')) {
		toggleSideBar();
	}

	if(sideBarStatus == 'on' && $('#sidebar').hasClass('active')) {
		toggleSideBar();
	}

	$('#sidebarCollapse').on('click', function () {
		sideBarStatus = localStorage.getItem('sideBarStatus');
		toggleSideBar();
		if(sideBarStatus == 'on') {
			localStorage.setItem('sideBarStatus', 'off');
		} else {
			localStorage.setItem('sideBarStatus', 'on');
		}
	});

	function toggleSideBar() {
		$('#sidebar').toggleClass('active');
		$('.sidebar-label').toggleClass('d-none');
		$('#logo').toggleClass('logooverflow');
		$('#content').toggleClass('content content2');
		$('#breadcrumb').toggleClass('breadcrumb-nav breadcrumb-nav2');
	}

});


$(function()
{
	$(document).on('click', '.btn-add-field', function(e)
	{
		e.preventDefault();
		var controlForm = $('#repeatingInputFields:first'),
			currentEntry = $(this).parents('.entry:first'),
			newEntry = $(currentEntry.clone()).appendTo(controlForm);
		newEntry.find('input').val('');
		controlForm.find('.entry:not(:last) .btn-add-field')
			.removeClass('btn-add-field').addClass('btn-remove')
			.removeClass('btn-success').addClass('btn-danger')
			.html('<span class="fas fa-minus" aria-hidden="true"></span>');
	}).on('click', '.btn-remove', function(e)
	{
		e.preventDefault();
		$(this).parents('.entry:first').remove();
		return false;
	});
});

document.addEventListener('DOMContentLoaded', function() {

	if(window.location.hash) {
		var element_to_scroll_to = document.getElementById(window.location.hash.substring(1));
		element_to_scroll_to.scrollIntoView();
	}

	//input type file
	var inputFile = document.getElementById("inputGroupFile01");
	if(inputFile != null) {
		inputFile.onchange = 
    		function(){
            var fileName = this.files[0].name;
            document.getElementById("inputGroupLabel01").innerHTML = fileName;
    	};
	}

	var inputFile = document.getElementById("inputGroupFile02");
	if(inputFile != null) {
		inputFile.onchange = 
    		function(){
            var fileName = this.files[0].name;
            document.getElementById("inputGroupLabel02").innerHTML = fileName;
    	};
	}
	
	//Messages modal
	var dialogMsg = document.querySelector('#messageModal #dialog');
	if(dialogMsg != null){
		$('#messageModal').modal({
	        resizable: false,
	        modal: true,
	        buttons: {
	                Fermer: function() {
	                       $( this ).dialog( "close" );
	               }
	        }
	       });
	}
	
});

//sign launch

var percent = 0;

function launchSign(id) {
	percent = 0;
	console.log('launch sign for : ' + id);
	$('#signModal').modal('hide');
	$('#wait').modal('show');
	$('#wait').modal({backdrop: 'static', keyboard: false})
	submitSignRequest(id);
}

function launchSignAll(id) {
	$('#signAllModal').modal('hide');
	$('#wait').modal('show');
	$('#wait').modal({backdrop: 'static', keyboard: false})
	var csrf = document.getElementsByName("_csrf")[0];
	var signRequestParams = "password=" + document.getElementById("passwordAll").value +
		"&" + csrf.name + "=" + csrf.value;
	var xmlHttp = new XMLHttpRequest();
	xmlHttp.open('POST', '/user/signbooks/sign/' + id, true);
	xmlHttp.setRequestHeader('Content-Type','application/x-www-form-urlencoded');
	xmlHttp.send(signRequestParams);
}

function submitSignRequest(id) {
	var csrf = document.getElementsByName("_csrf")[0];
	var signPageNumber = document.getElementById("signPageNumber");
	var signRequestParams;
	if(signPageNumber != null) {
		signRequestParams = "password=" + document.getElementById("password").value +
							"&addDate=" + dateActive +
							"&visual=" + visualActive +
							"&xPos=" + document.getElementById("xPos").value +
							"&yPos=" + document.getElementById("yPos").value +
							"&signPageNumber=" + document.getElementById("signPageNumber").value +
							"&" + csrf.name + "=" + csrf.value
							;
	} else {
		signRequestParams = "password=" + document.getElementById("password").value +
							"&" + csrf.name + "=" + csrf.value;
	}
	sendData(id, signRequestParams);
}

function sendData(id, signRequestParams) {
	document.getElementById("passwordError").style.display = "none";
	document.getElementById("signError").style.display = "none";
	document.getElementById("closeModal").style.display = "none";
	document.getElementById("validModal").style.display = "none";
	document.getElementById("bar").style.display = "none";
	document.getElementById("bar").classList.add("progress-bar-animated");
	getProgressTimer = setInterval(function() { getStep(id, signRequestParams); }, 500);
	var xmlHttp = new XMLHttpRequest();
	xmlHttp.open('POST', '/user/signrequests/sign/' + id, true);
	xmlHttp.setRequestHeader('Content-Type','application/x-www-form-urlencoded');
	xmlHttp.send(signRequestParams);
}

function getStep(id, signRequestParams) {
	percent = percent + 2;
	console.log("getStep");
	var xmlHttp = new XMLHttpRequest();
	xmlHttp.open("GET", "/user/signrequests/get-step", true);
	xmlHttp.onreadystatechange = function() {
		var result = xmlHttp.responseText;
		if (result == "security_bad_password") {
			document.getElementById("passwordError").style.display = "block";
			document.getElementById("closeModal").style.display = "block";
			document.getElementById("bar").classList.remove("progress-bar-animated");
			clearInterval(getProgressTimer);
		} else if(result == "sign_system_error" || result == "not_authorized") {
			clearInterval(getProgressTimer);
			document.getElementById("signError").style.display = "block";
			document.getElementById("closeModal").style.display = "block";
			document.getElementById("bar").classList.remove("progress-bar-animated");
		} else if(result == "initNexu") {
			clearInterval(getProgressTimer);
			document.location.href="/user/nexu-sign/" + id;
		} else if (result == "end") {
			clearInterval(getProgressTimer);
			document.getElementById("validModal").style.display = "block";
			document.getElementById("bar").classList.remove("progress-bar-animated");
			document.getElementById("bar-text").innerHTML = "Signature terminée";
			document.getElementById("bar").style.width = 100 + "%";
			document.location.href="/user/signrequests/" + id;
		} else {
			document.getElementById("bar").style.display = "block";
			document.getElementById("bar").style.width = percent + "%";
			document.getElementById("bar-text").innerHTML = result;
		}
	}
	xmlHttp.send(null);
	return;
}

//drag signature position
var pointerDiv;
var startPosX;
var startPosY;
var pointItMove = false;

function resetPosition() {
	console.log("out");
	var cross = document.getElementById("cross");
	if(cross != null) {
		cross.style.left = (startPosX * zoom) + "px";
		cross.style.top = (startPosY * zoom)  + "px";
		document.getElementById("xPos").value = startPosX;
		document.getElementById("yPos").value = startPosY;
		cross.style.pointerEvents = "auto";
		document.body.style.cursor = "default";
		pointItEnable = false;
		pointItMove = false
	}
}

function savePosition() {
	if(pointItEnable && pointItMove) {
		console.log("save");
		startPosX = Math.round(posX * scale);
		startPosY = Math.round(posY * scale);
	}
	cross.style.backgroundColor= 'rgba(0, 255, 0, 0)';
	cross.style.pointerEvents = "auto";
	document.body.style.cursor = "default";
	pointItEnable = false;
	pointItMove = false
}

function dragSignature() {
	console.log("drag");
	cross.style.pointerEvents = "none";
	pdf.style.pointerEvents = "auto";
	document.body.style.cursor = "move";
	pointItEnable = true;
}

//Hack to convert dss signature panel to card (bootstrap 4)
document.addEventListener('DOMContentLoaded', function() {
	var divs = document.getElementsByTagName("div");
	[].forEach.call(divs, function(div) {
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
	
	var spans = document.getElementsByTagName("span");
	[].forEach.call(spans, function(span) {
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
});

//image picker
document.addEventListener('DOMContentLoaded', function() {
	
	var vanillaResult = document.querySelector('.vanilla-result'), vanillaUpload = document.querySelector('.vanilla-upload');
	if(document.getElementById('vanilla-crop') != null) {
		var vanillaCrop = new Croppie(document.getElementById('vanilla-crop'), {
			viewport : {
				width : 200,
				height : 150
			},
			boundary : {
				width : 400,
				height : 300
			},
			enableExif : true,
			enableOrientation : true,
			enableResize : true,
			enforceBoundary : false
	
		});
	
		$('.vanilla-rotate').on('click', function(ev) {
            vanillaCrop.rotate(parseInt($(this).data('deg')));
        });
		
		document.getElementById('vanilla-crop').addEventListener('update', function(ev) {
			var result = vanillaCrop.result('base64');
			result.then(saveVanilla);
		});
	
		vanillaUpload.addEventListener('change', function() {
			readFile(this);
		});
	}

	function saveVanilla(result) {
		document.getElementById('signImageBase64').value = result;
	}

	function readFile(input) {
		if (input.files) {
			if (input.files[0]) {
				var reader = new FileReader();

				reader.onload = function(e) {
					document.getElementById('vanilla-crop').classList.add('good');
					vanillaCrop.bind({
						url : e.target.result,
						orientation : 1
					});
				}
				reader.readAsDataURL(input.files[0]);
			}
		}
	}

});

//create signBook
var	signBookTypeSelect;
var recipientEmailInput;
var signBooksIdsInput;
var tempoFieldInput;

document.addEventListener('DOMContentLoaded', function() {
	/*
	signBookTypeSelect = document.getElementById("_signBookType_id");
	recipientEmailInput = document.getElementById("_c_org_esupportail_esupsignature_domain_SignRequest_recipientemails");
	signBooksIdsInput = document.getElementById("_c_org_esupportail_esupsignature_domain_SignRequest_signBooksIds");
	tempoFieldInput = document.getElementById("_c_org_esupportail_esupsignature_domain_SignRequest_tempofield");
	if(recipientEmailInput != null) {
		recipientEmailInput.style.display = "none";
		signBooksIdsInput.style.display = "none";
	}
	*/
});

function changeSignBookForm() {
	var selectedValue = signBookTypeSelect.options[signBookTypeSelect.selectedIndex].value;
	if(selectedValue == "group") {
		recipientEmailInput.style.display = "flex";
		signBooksIdsInput.style.display = "none";
		tempoFieldInput.style.display = "none";
	} else {
		recipientEmailInput.style.display = "none";
		signBooksIdsInput.style.display = "flex";
		tempoFieldInput.style.display = "none";
	}
}

//create user
var signImageForm;
var keyForm;
var signImageInput;
var keystoreInput;
var emailAlertFrequencySelect;
var emailAlertDay;
var emailAlertHour;
var signTypeSelect;
var selectedValue;

document.addEventListener('DOMContentLoaded', function() {
	signImageInput = document.getElementById("inputGroupFile01");
	keystoreInput = document.getElementById("inputGroupFile02");
	signImageForm = document.getElementById("signImageForm");
	keyForm = document.getElementById("keyForm");
	emailAlertFrequencySelect = document.getElementById("_emailAlertFrequency_id");
	emailAlertDay = document.getElementById("_c_org_esupportail_esupsignature_domain_user_emailAlertDay");
	emailAlertHour = document.getElementById("_c_org_esupportail_esupsignature_domain_user_emailAlertHour");
	signTypeSelect = document.getElementById("_signType_id");
	if(emailAlertFrequencySelect != null) {
		checkAlertFrequency();
	}
});

function checkAlertFrequency() {
	var selectedValue = emailAlertFrequencySelect.options[emailAlertFrequencySelect.selectedIndex].value;
	if(selectedValue == 'daily') {
		emailAlertDay.style.display = "none";
		emailAlertHour.style.display = "flex";
	} else if(selectedValue == 'weekly') { 
		emailAlertDay.style.display = "flex";
		emailAlertHour.style.display = "none";
	} else {
		emailAlertDay.style.display = "none";
		emailAlertHour.style.display = "none";
	}
}

$(document).ready(function() {
    $('#select-from').dblclick(function(){
        $('#select-from option:selected').each( function() {
                $('#select-to').append("<option value='"+$(this).val()+"'>"+$(this).text()+"</option>");
            $(this).remove();
        });
    });
    $('#select-to').dblclick(function(){
        $('#select-to option:selected').each( function() {
            $('#select-from').append("<option value='"+$(this).val()+"'>"+$(this).text()+"</option>");
            $(this).remove();
        });
    });
    $('#btn-up').bind('click', function() {
        $('#select-to option:selected').each( function() {
            var newPos = $('#select-to option').index(this) - 1;
            if (newPos > -1) {
                $('#select-to option').eq(newPos).before("<option value='"+$(this).val()+"' selected='selected'>"+$(this).text()+"</option>");
                $(this).remove();
            }
        });
    });
    $('#btn-down').bind('click', function() {
        var countOptions = $('#select-to option').length;
        $('#select-to option:selected').each( function() {
            var newPos = $('#select-to option').index(this) + 1;
            if (newPos < countOptions) {
                $('#select-to option').eq(newPos).after("<option value='"+$(this).val()+"' selected='selected'>"+$(this).text()+"</option>");
                $(this).remove();
            }
        });
    });
    
    $('#fc_org_esupportail_esupsignature_domain_SignBook').submit(function(){
        $('#select-to option').prop('selected', true);
    });
});

function resizeHeader() {
	$(document.body).css("margin-top", $(".header-fixed").height());
}

$( document ).ready(function() {
	resizeHeader();
});

$(window).resize(function() {
	resizeHeader();
}).resize();

// PDF.JS

$(document).ready(function() {

});
