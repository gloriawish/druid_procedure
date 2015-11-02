/*
 * Copyright 1999-2101 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.druid.sql.dialect.mysql.ast.clause;

import com.alibaba.druid.sql.ast.SQLDataType;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.dialect.mysql.ast.MySqlObjectImpl;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitor;

/**
 * 
 * @Description: MySql procedure parameter
 * @author zz email:455910092@qq.com
 * @date 2015-9-14
 * @version V1.0
 */
public class MySqlParameter extends MySqlObjectImpl {

    private SQLExpr     name;
    private SQLDataType dataType;
    private SQLExpr     defaultValue;
    private ParameterType paramType;

    public ParameterType getParamType() {
		return paramType;
	}

	public void setParamType(ParameterType paramType) {
		this.paramType = paramType;
	}

	public SQLExpr getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(SQLExpr deaultValue) {
        this.defaultValue = deaultValue;
    }

    public SQLExpr getName() {
        return name;
    }

    public void setName(SQLExpr name) {
        this.name = name;
    }

    public SQLDataType getDataType() {
        return dataType;
    }

    public void setDataType(SQLDataType dataType) {
        this.dataType = dataType;
    }

    @Override
    public void accept0(MySqlASTVisitor visitor) {
        if (visitor.visit(this)) {
            acceptChild(visitor, name);
            acceptChild(visitor, dataType);
            acceptChild(visitor, defaultValue);
        }
        visitor.endVisit(this);
    }
    /**
     * mysql procedure parameter type
     */
    public static enum ParameterType
    {
    	DEFAULT,
    	IN,//in
    	OUT,//out
    	INOUT//inout
    }

}
