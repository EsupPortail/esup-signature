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
package org.esupportail.esupnfccarteculture.postgres;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.type.Type;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.BooleanType;

public class PgFullTextFunction implements SQLFunction {

	/* Column name of TSVECTOR field in PgSQL table */
    public static final String FTS_VECTOR_FIELD = "textsearchable_index_col";

    @Override
    public Type getReturnType(Type firstArgumentType, Mapping mapping) throws QueryException{
        return new BooleanType();
    }
    

    @Override
    public boolean hasArguments() {
        return true;
    }

    @Override
    public boolean hasParenthesesIfNoArguments() {
        return false;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public String render(Type type, List args, SessionFactoryImplementor factory) throws QueryException {
        String searchString = (String) args.get(0);
        return FTS_VECTOR_FIELD + " @@ to_tsquery('simple'," + searchString + ")";
    }
    
}
