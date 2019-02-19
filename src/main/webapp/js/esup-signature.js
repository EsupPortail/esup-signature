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
	
	//TODO a virer ?
	if(document.querySelector('#cancelFilters') != null) {
		document.querySelector('#cancelFilters').onclick = function() {
		   window.location.href='';
		}
	}
	
	//ui list signRequest
	var selectAll = document.getElementById("selectall");
	var checkBoxes = document.querySelectorAll(".check");
	var openDocsButton = document.getElementById("opendocs");
	var signDocsButton = document.getElementById("signdocs");
	if (selectAll != null) {
		openDocsButton.disabled = true;
		selectAll.onchange = function() {
			[].forEach.call(checkBoxes, function(checkBox) {
				if (selectAll.checked) {
					checkBox.checked = true;
				} else {
					checkBox.checked = false;
				}
			});

		};
	}
	
	[].forEach.call(checkBoxes, function(checkBox) {
		checkBox.addEventListener('click', function(event) {
			checkOpenDocButtonEnable();
			checkSignDocButtonEnable();
		});
	});
	
	function checkOpenDocButtonEnable() {
		for (i = 0; i < checkBoxes.length; i++) {
			if(checkBoxes[i].checked) {
				openDocsButton.disabled = false;
				break;
			} else {
				openDocsButton.disabled = true;
			}
		}
	}
	
	function checkSignDocButtonEnable() {
		signDocsButton.disabled = true;
		for (i = 0; i < checkBoxes.length; i++) {
			if(checkBoxes[i].checked) {
				if(document.getElementById("status_" + checkBoxes[i].value).value != "pending") {
					signDocsButton.disabled = true;
					break;
				} else {
					signDocsButton.disabled = false;
				}
			}
			
		}
	}

	
});
