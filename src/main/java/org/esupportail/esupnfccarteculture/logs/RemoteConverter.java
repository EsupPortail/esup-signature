/**
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
package org.esupportail.esupnfccarteculture.logs;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class RemoteConverter extends ClassicConverter {
    @Override
    public String convert(ILoggingEvent event) {
    	try{
	    	String remoteAddress = ((ServletRequestAttributes)RequestContextHolder.currentRequestAttributes())
	    			.getRequest().getRemoteAddr();
	    	if (remoteAddress != null) {
	            return remoteAddress;
	    	}
    	}catch(Exception e){
			e.printStackTrace();
    	}
    	return "NO_REMOTE_ADDRESS";
       
    }
}