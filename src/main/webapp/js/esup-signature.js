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
document.addEventListener('DOMContentLoaded', function() {
	
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
		var messageModal = document.getElementById('messageModal');
		var myModalInstance = new Modal(messageModal);
		myModalInstance.show();
	}
	
});

var pointerDiv;
var startPosX;
var startPosY;

document.addEventListener('DOMContentLoaded', function() {
	pointerDiv = document.getElementById("pointer_div");
	if(pointerDiv != null) {
		startPosX = document.getElementById("xPos").value;
		startPosY = document.getElementById("yPos").value;
	}
});

var pointItEnable = true;

function pointIt(event) {
	posX = event.offsetX ? (event.offsetX)
			: event.pageX
					- document
							.getElementById("pointer_div").offsetLeft;
	posY = event.offsetY ? (event.offsetY)
			: event.pageY
					- document
							.getElementById("pointer_div").offsetTop;
	var cross = document.getElementById("cross");
	if(cross != null && posX > 0 && posY > 0 && pointItEnable) {
		cross.style.left = (posX);
		cross.style.top = (posY);
		document.getElementById("xPos").value = posX;
		document.getElementById("yPos").value = posY;
		document.getElementById("borders").classList.add("anim-border");
	}
}

function resetPosition() {
	var cross = document.getElementById("cross");
	if(cross != null) {
		cross.style.left = (startPosX);
		cross.style.top = (startPosY);
		document.getElementById("xPos").value = startPosX;
		document.getElementById("yPos").value = startPosY;
		document.getElementById("borders").classList.remove("anim-border");
	}
}

function savePosition() {
	var borders = document.getElementById("borders");
	if(borders != null) {
		if(pointItEnable) {
			startPosX = posX;
			startPosY = posY;
			pointItEnable = false;
			borders .classList.remove("anim-border");
		} else {
			pointItEnable = true;
			borders .classList.add("anim-border");
		}
	}
}

// Paging
document.addEventListener('DOMContentLoaded', function() {
	if(typeof currentImagePage !== 'undefined') {
		if (currentImagePage == 0) {
			document.getElementById("previous").disabled = true;
		}
		if (currentImagePage == nbImagePage - 1) {
			document.getElementById("next").disabled = true;
		}
	}
});
function nextImage() {
	currentImagePage++;
	document.getElementById("pointer_div").style.backgroundImage = "url('" + documentUrl + "/" + currentImagePage + "')";
	if (currentImagePage == nbImagePage - 1) {
		document.getElementById("next").disabled = true;
	}
	if (currentImagePage > 0) {
		document.getElementById("previous").disabled = false;
	}
	document.getElementById("signPageNumber").value = currentImagePage + 1;
}

function previousImage() {
	currentImagePage--;
	document.getElementById("pointer_div").style.backgroundImage = "url('" + documentUrl + "/" + currentImagePage + "')";
	if (currentImagePage == 0) {
		document.getElementById("previous").disabled = true;
	}
	if (nbImagePage - 1 > currentImagePage) {
		document.getElementById("next").disabled = false;
	}
	document.getElementById("signPageNumber").value = currentImagePage + 1;
}

//toggle overloadsignparams
var overloadSignParams;
var signTypeSelector;
var signTypeDiv;
var newPageTypeSelector;
var newPageTypeDiv;
var overloadYes;
var overloadNo;

document.addEventListener('DOMContentLoaded', function() {
	signTypeSelector = document.getElementById("_signType_id");
	signTypeDiv = document.getElementById("_signType_div_id");
	newPageTypeSelector = document.getElementById("_newPageType_id");
	newPageTypeDiv = document.getElementById("_newPageType_div_id");
	
	if(signTypeDiv != null) {
		signTypeSelector.disabled = true;
		newPageTypeSelector.disabled = true;
		signTypeDiv.classList.add("div-disable");
		newPageTypeDiv.classList.add("div-disable");
	}
});

function toggleOverload() {
	overloadSignParams = document.getElementById("_overloadSignParams");
	if(!overloadSignParams.checked) {
		signTypeDiv.classList.add("div-disable");
		newPageTypeDiv.classList.add("div-disable");
		signTypeSelector.disabled = true;
		newPageTypeSelector.disabled = true;
	} else {
		signTypeDiv.classList.remove("div-disable");
		newPageTypeDiv.classList.remove("div-disable");
		signTypeSelector.disabled = false;
		newPageTypeSelector.disabled = false;
	}
}

//toggle needAllSign
var allSignToComplete;
var allsignYes;
var allsignNo;
var signBookIds;
var signBookIdsDiv;

document.addEventListener('DOMContentLoaded', function() {
	signBookIdsDiv = document.getElementById("_signBookIds_div_id");
	signBookIds = document.getElementById("signBookIds");
	
});

function togglehideAllSign() {
	var selectedValues = getSelectValues(signBookIds);
	if(selectedValues.length > 1) {
		allSignToComplete = signBookIdsDiv.classList.remove("div-disable");
	} else {
		allSignToComplete = signBookIdsDiv.classList.add("div-disable");
	}
}

function getSelectValues(select) {
	  var result = [];
	  var options = select && select.options;
	  var opt;

	  for (var i=0, iLen=options.length; i<iLen; i++) {
	    opt = options[i];

	    if (opt.selected) {
	      result.push(opt.value || opt.text);
	    }
	  }
	  return result;
	}

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
	if(document.getElementById('vanilla-demo') != null) {
		var vanilla = new Croppie(document.getElementById('vanilla-demo'), {
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
			enforceBoundary : false
	
		});
	
		document.getElementById('vanilla-demo').addEventListener('update', function(ev) {
			vanilla.result('canvas').then(saveVanilla);
		});
	
		vanillaUpload.addEventListener('change', function() {
			readFile(this);
		});
	}
	
	function resultVanilla(result) {
		alert(result);
	}

	function saveVanilla(result) {
		document.getElementById('signImageBase64').value = result;
	}

	function readFile(input) {
		if (input.files) {
			if (input.files[0]) {
				var reader = new FileReader();

				reader.onload = function(e) {
					document
							.getElementById('vanilla-demo').classList
							.add('good');
					vanilla.bind({
						url : e.target.result,
						orientation : 1
					});
				}
				reader.readAsDataURL(input.files[0]);
			}
		}
	}

});

//create user
var signImageInput;
var keystoreInput;
var emailAlertFrequencySelect;
var emailAlertDay;
var emailAlertHour;

document.addEventListener('DOMContentLoaded', function() {
	signImageInput = document.getElementById("inputGroupFile01");
	keystoreInput = document.getElementById("inputGroupFile02");
	emailAlertFrequencySelect = document.getElementById("_emailAlertFrequency_id");
	emailAlertDay = document.getElementById("_c_org_esupportail_esupsignature_domain_user_emailAlertDay");
	emailAlertHour = document.getElementById("_c_org_esupportail_esupsignature_domain_user_emailAlertHour");
});

function checkRequirement() {

	var signTypeSelect = document.getElementById("_signType_id");
	var selectedValue = signTypeSelect.options[signTypeSelect.selectedIndex].value; 
	if(selectedValue == 'certSign') {
		signImageInput.required = true;
		keystoreInput.required = true;
	} else if(selectedValue == 'imageStamp' || selectedValue == 'nexuSign') {
		signImageInput.required = true;
		keystoreInput.required = false;
	} else {
		signImageInput.required = false;
		keystoreInput.required = false;
	}
}

function checkAlertFrequency() {
	var selectedValue = emailAlertFrequencySelect.options[emailAlertFrequencySelect.selectedIndex].value;
	if(selectedValue == 'daily') {
		emailAlertDay.style.display = "none";
		emailAlertHour.style.display = "block";
	} else if(selectedValue == 'weekly') { 
		emailAlertDay.style.display = "block";
		emailAlertHour.style.display = "none";
	} else {
		emailAlertDay.style.display = "none";
		emailAlertHour.style.display = "none";
	}
}