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
package com.alibaba.druid.bvt.sql.mysql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.junit.Assert;

import com.alibaba.druid.sql.MysqlTest;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLCallStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.clause.MySqlCreateProcedureStatement;
import com.alibaba.druid.sql.dialect.mysql.executor.MySqlProcedureExecutor;
import com.alibaba.druid.sql.dialect.mysql.executor.MySqlProcedureExecutor.VariableObject;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.visitor.SQLEvalVisitorUtils;
import com.alibaba.druid.util.JdbcConstants;

public class MySqlExecuteProcedureTest2 extends MysqlTest {

	/**
	 * test leave with label
	 * @throws Exception
	 */
    public void test_0() throws Exception {
    	String sql="create or replace procedure sp_leave2()"+
				" begin"+
				" declare @x int;"+
				" set @x = 10;"+
				" label1: while @x>0 do"+
				" set @x=@x-1;"+
				" while 1>0 do"+
				" if @x<5 then"+
				" leave label1;"+
				" end if;"+
				" set @x=@x-1;"+
				" end while;"+
				" end while label1;"+
				" set @x=1000;"+
				" end";
	
    	MySqlStatementParser parser=new MySqlStatementParser(sql);
    	List<SQLStatement> statementList = parser.parseStatementList();
    	SQLStatement statemen = statementList.get(0);
    	
    	print(statementList);
    	
    	System.out.println("#############################");
    	
    	MySqlCreateProcedureStatement sp=(MySqlCreateProcedureStatement)statemen;
    	//MySqlProcedureExecutor.executeCreateProcedure(sp, sql);
    	
    }

    
    public void test_1() throws Exception {
    	String sql="create or replace procedure sp_leave1()"+
				" begin"+
				" declare @x int;"+
				" set @x = 10;"+
				" label1: while @x>0 do"+
				" set @x=@x-1;"+
				" while 1>0 do"+
				" if @x<5 then"+
				" leave;"+
				" end if;"+
				" set @x=@x-1;"+
				" end while;"+
				" end while label1;"+
				" set @x=1000;"+
				" end";
	
    	MySqlStatementParser parser=new MySqlStatementParser(sql);
    	List<SQLStatement> statementList = parser.parseStatementList();
    	SQLStatement statemen = statementList.get(0);
    	
    	print(statementList);
    	
    	System.out.println("#############################");
    	
    	MySqlCreateProcedureStatement sp=(MySqlCreateProcedureStatement)statemen;
    	//MySqlProcedureExecutor.executeCreateProcedure(sp, sql);
    	
    }
    
    /**
     * 调用存储过程的语句
     */
    public void test_2() throws Exception {
    	
    	
    	String sql="call sp_leave1()";
	
    	MySqlStatementParser parser=new MySqlStatementParser(sql);
    	List<SQLStatement> statementList = parser.parseStatementList();
    	SQLStatement statement = statementList.get(0);
    	
    	System.out.println("\n#################### execute procedure #####################");
    	SQLCallStatement call=(SQLCallStatement)statement;
    	MySqlProcedureExecutor.executeCallProcedure(call);
    	
    	
    }
}
