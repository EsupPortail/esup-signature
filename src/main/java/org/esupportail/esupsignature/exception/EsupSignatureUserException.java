/**
 * Licensed to EsupPortail under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * EsupPortail licenses this file to you under the Apache License,
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
package org.esupportail.esupsignature.exception;

public class EsupSignatureUserException extends EsupSignatureException {

	private static final long serialVersionUID = 1L;

	String message;

	public EsupSignatureUserException(String message) {
		super(message);
		this.message = message;
	}

	public EsupSignatureUserException(String message, Throwable e) {
		super(message, e);
		this.message = message;
	}
	
}