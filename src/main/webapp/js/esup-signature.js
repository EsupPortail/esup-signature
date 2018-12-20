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
	
	//Messages modal
	var dialogMsg = document.querySelector('#messageModal #dialog');
	if(dialogMsg != null){
		var messageModal = document.getElementById('messageModal');
		var myModalInstance = new Modal(messageModal);
		myModalInstance.show();
	}
	
	if(document.querySelector('#cancelFilters') != null) {
		document.querySelector('#cancelFilters').onclick = function() {
		   window.location.href='';
		}
	}
	
	//Custom roocol link
	var links = document.querySelectorAll('.viewEtudiant');
	[].forEach.call(links, function(link) {
		if(link.querySelector('a') != null){
			link.querySelector('a').classList.add('btn');
			link.querySelector('a').classList.add('btn-info');
			link.querySelector('a').classList.add('far');
			link.querySelector('a').classList.add('fa-eye');
			link.querySelector('a').text = '';
		}
	});
	
	//Chart JS
	Chart.defaults.global.responsive= true;
	Chart.defaults.global.maintainAspectRatio=false;
	Chart.defaults.global.responsiveAnimationDuration=0;
	Chart.defaults.global.legend.labels.fontFamily = "Arial";
	Chart.defaults.global.legend.labels.boxWidth = 20;
	Chart.defaults.global.legend.position= "bottom";
	Chart.defaults.global.title.fontFamily = "Arial";
	Chart.defaults.global.title.fontSize = 14;
	Chart.defaults.global.title.display= false;
	Chart.defaults.global.elements.point.radius=7;
	Chart.defaults.global.elements.point.backgroundColor= "#fff";
	Chart.defaults.global.elements.point.borderColor= "rgba(0,0,0,1)";
	Chart.defaults.global.elements.line.tension=0.2;
	Chart.defaults.global.elements.line.backgroundColor= "rgba(220,220,220,0.5)";
	Chart.defaults.global.elements.line.borderColor= "rgba(100,100,100,0.7)";
	Chart.pluginService.register({
		  beforeRender: function (chart) {
		    if (chart.config.options.showAllTooltips) {
		        chart.pluginTooltips = [];
		        chart.config.data.datasets.forEach(function (dataset, i) {
		            chart.getDatasetMeta(i).data.forEach(function (sector, j) {
		                chart.pluginTooltips.push(new Chart.Tooltip({
		                    _chart: chart.chart,
		                    _chartInstance: chart,
		                    _data: chart.data,
		                    _options: chart.options.tooltips,
		                    _active: [sector]
		                }, chart));
		            });
		        });
		        chart.options.tooltips.enabled = false;
		    }
		    
		},
		afterDraw: function (chart, easing) {
		    if (chart.config.options.showAllTooltips) {
		        if (!chart.allTooltipsOnce) {
		            if (easing !== 1)
		                return;
		            chart.allTooltipsOnce = true;
		        }
		        chart.options.tooltips.enabled = true;
		        Chart.helpers.each(chart.pluginTooltips, function (tooltip) {
		            tooltip.initialize();
		            tooltip.update();
		            tooltip.pivot();
		            tooltip.transition(easing).draw();
		        });
		    }
		}
	});
});

function getTagByLocationDoughnut(name, tooltips) {
	var request = new XMLHttpRequest();
	request.open('GET',  "stats/chartJson?model=numberTagByLocation" + name + "&annee="+annee, true);
	request.onload = function(){
		if(request.status >= 200 && request.status < 400) {
		    var data = JSON.parse(this.response);
			var ctx = document.getElementById("tagsByLocation" + name);
	    	var tagsByLocationInscChart = new Chart(ctx, {
	    		type: 'doughnut',
	    		data: data,
	    		options: {
	    			animation:{
	        			animateRotate:true,
	        			animateScale:true
	        		},
        			title: {
        				text: "tagsByLocation" + name
        			},
        			showAllTooltips: tooltips,
        			tooltips: {

        				callbacks: {
        					label:function(item, data){
        						var sum = 0;
        						data.datasets[0].data.forEach(
        							function addNumber(value) { sum += parseInt(value); }
        						);
        						pourcent = data.datasets[0].data[item.index] / sum * 100;
        						return data.labels[item.index]+" : "+data.datasets[0].data[item.index] + " - "+ Math.round(pourcent*100)/100 +"%";
        					}
	                
        				}
        			}
	            },
	    	});
		}
	}
	request.send();
}

function getNumberTagByWeekChart(name, tooltips) {
	var request = new XMLHttpRequest();
	request.open('GET',  "stats/chartJson?model=numberTagByWeek" + name + "&annee="+annee, true);
	request.onload = function() {
		if(request.status >= 200 && request.status < 400) {
		    var data = JSON.parse(this.response);
	    	var ctx = document.getElementById("tagsByWeek" + name).getContext("2d");
	    	var repartionComposantesChart = new Chart(ctx, {
	    		type: 'bar',
	    		data: data,
	    		options: {
	    			animation:{
	    				animateRotate:true,
	    				animateScale:true
	        		},
	        		title: {
	        			text: "tagsByWeek" + name,
	        		},
	        		showAllTooltips: tooltips,
	    		},
	    	});
		}
	}
	request.send();
}
