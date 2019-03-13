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
