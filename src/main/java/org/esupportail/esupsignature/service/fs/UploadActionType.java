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
package org.esupportail.esupsignature.service.fs;

public enum UploadActionType {

	/** To override the same filename. */
	OVERRIDE(0),
	/** To automatically rename the file uploaded. */
	RENAME_NEW(1),
	/** To rautomatically ename the file already on the server before upload. */
	RENAME_OLD(2),
	/** To throw an "existingFileException" and return success=false with a specific message and to manage in the UI with user action. */
	ERROR(3);

	/** Int value of the action type*/
	private int code;

	/**
	 * Contructor of the object UploadActionType.java.
	 * @param code an int
	 */
	private UploadActionType(final int code) {
		this.code = code;
	}

	/**
	 * Contructor of the object UploadActionType.java.
	 * @param code a String
	 */
	private UploadActionType(final String code) {
		this.code = Integer.parseInt(code);
	}

	/**
	 * Getter of member code.
	 * @return <code>int</code> the attribute code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Setter of attribute code.
	 * @param code the attribute code to set
	 */
	public void setCode(final int code) {
		this.code = code;
	}
}
