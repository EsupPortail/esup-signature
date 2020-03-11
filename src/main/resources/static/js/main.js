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
import {GlobalUi} from "./modules/globalUi.js";
let globalUi = new GlobalUi();
if(document.URL.match("(\/user\/signrequests\/[\\s\\S]+[^?|^\/])")) {
    console.info("show side bar");
    globalUi.showSideBar();
}
if(document.URL.match("(\/user\/signrequests\/?$)")) {
    console.info("localStorage remove : mode");
    localStorage.removeItem('mode');
}

export let stepper;
let stepDiv = document.getElementById("stepperDefault");
if (stepDiv != null) {
    import('./modules/step.js').then((step) => {
        const Step = step.default;
        stepper = new Step(stepDiv);
    });
}

if ($("#managersSelect").length) {
    import('./modules/selectUser.js').then((selectUser) => {
        const SelectUser = selectUser.default;
        new SelectUser("managersSelect");
    });
}
