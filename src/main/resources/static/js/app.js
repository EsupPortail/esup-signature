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
	}

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

	/*
    var wait = $.ajax({
        url: "/user/signrequests/wait-for-sign/" + token, success: function (result) {
            location.reload();
        }
    });
    $('#wait').on('hidden.bs.modal', function () {
        console.log('abort ajax');
        wait.abort();
    })

     */
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
		} else if(result == "sign_system_error") {
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

//signature position

var pointerDiv;
var startPosX;
var startPosY;
var pointItMove = false;

document.addEventListener('DOMContentLoaded', function() {
	pointerDiv = document.getElementById("pointer_div");
	if(pointerDiv != null) {
		startPosX = document.getElementById("xPos").value;
		startPosY = document.getElementById("yPos").value;
	}
});

function animBorder() {
	document.getElementById("borders").classList.add("anim-border");
}

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

function startPointIt() {
	console.log("start point it");
	pointItEnable = true;
}

//pdf navigation
// document.addEventListener('DOMContentLoaded', function() {
// 	if(document.getElementById("next") != null) {
//
// 		if (currentImagePage == nbImagePage - 1) {
// 			document.getElementById("next").classList.add("disabled");
// 			document.getElementById("next").disabled = true;
// 		}
// 		if (currentImagePage > 0) {
// 			document.getElementById("previous").classList.remove("disabled");
// 			document.getElementById("previous").disabled = false
// 		}
// 	}
// });
//
// function nextImage() {
// 	currentImagePage++;
// 	hideSigns(currentImagePage)
// 	console.info("url('" + documentUrl + "" + currentImagePage + "')");
// 	document.getElementById("pointer_div").style.backgroundImage = "url('" + documentUrl + "" + currentImagePage + "')";
// 	if (currentImagePage == nbImagePage - 1) {
// 		document.getElementById("next").classList.add("disabled");
// 		document.getElementById("next").disabled = true;
// 	}
// 	if (currentImagePage > 0) {
// 		document.getElementById("previous").classList.remove("disabled");
// 		document.getElementById("previous").disabled = false
// 	}
// 	document.getElementById("signPageNumber").value = currentImagePage;
// }
//
// function previousImage() {
// 	currentImagePage--;
// 	hideSigns(currentImagePage)
// 	document.getElementById("pointer_div").style.backgroundImage = "url('" + documentUrl + "" + currentImagePage + "')";
// 	if (currentImagePage == 0) {
// 		document.getElementById("previous").classList.add("disabled");
// 		document.getElementById("previous").disabled = true;
// 	}
// 	if (nbImagePage - 1 > currentImagePage) {
// 		document.getElementById("next").classList.remove("disabled");
// 		document.getElementById("next").disabled = false;
// 	}
// 	document.getElementById("signPageNumber").value = currentImagePage;
// }
//
// function hideSigns(currentImagePage) {
// 	var signImages = document.querySelectorAll('[id^="signParam_"]');
// 	[].forEach.call(signImages, function(signImage) {
// 		if(signImage.id.includes(currentImagePage + 1)) {
// 			signImage.style.display = "block";
// 		} else {
// 			signImage.style.display = "none";
// 		}
// 	});
// }
//
// //toggle overloadsignparams
// var overloadSignParams;
// var signTypeSelector;
// var signTypeDiv;
// var newPageTypeSelector;
// var newPageTypeDiv;
// var overloadYes;
// var overloadNo;
//
// document.addEventListener('DOMContentLoaded', function() {
// 	signTypeSelector = document.getElementById("_signType_id");
// 	signTypeDiv = document.getElementById("_signType_div_id");
// 	newPageTypeSelector = document.getElementById("_newPageType_id");
// 	newPageTypeDiv = document.getElementById("_newPageType_div_id");
// });
//
// function toggleOverload() {
// 	overloadSignParams = document.getElementById("_overloadSignParams");
// 	if(!overloadSignParams.checked) {
// 		signTypeDiv.classList.add("d-none");
// 		newPageTypeDiv.classList.add("d-none");
// 		signTypeSelector.disabled = true;
// 		newPageTypeSelector.disabled = true;
// 	} else {
// 		signTypeDiv.classList.remove("d-none");
// 		newPageTypeDiv.classList.remove("d-none");
// 		signTypeSelector.disabled = false;
// 		newPageTypeSelector.disabled = false;
// 	}
// }
//
// function getSelectValues(select) {
// 	  var result = [];
// 	  var options = select && select.options;
// 	  var opt;
//
// 	  for (var i=0, iLen=options.length; i<iLen; i++) {
// 	    opt = options[i];
//
// 	    if (opt.selected) {
// 	      result.push(opt.value || opt.text);
// 	    }
// 	  }
// 	  return result;
// 	}

//Hack to convert panel to card (bootstrap 4)
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
