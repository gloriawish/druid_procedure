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

public class MySqlExecuteProcedureTest0 extends MysqlTest {

	/**
	 * test if
	 * @throws Exception
	 */
    public void test_0() throws Exception {
    	String sql="create or replace procedure sp_name()"+
				" begin"+
				" declare @x int;"+
				" set @x = 10;"+
				" if @x>1 then"+
				" declare @m int;"+
				" insert into test1 values(1,1);"+
				" else"+
				" delete from test2 where id=1;"+
				" end if;"+
				" set @m=1000;"+
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
    	
    	Stack<Map<String,Object>> stack=new Stack<Map<String,Object>>();
    	
    	VariableObject value1=new VariableObject(null, 10);
    	VariableObject value2=new VariableObject(null, 1);
    	VariableObject value3=new VariableObject(null, 1);
    	Map<String,Object> map=new HashMap<String, Object>();
    	
    	map.put("@x", value1);
    	map.put("@y", value2);
    	map.put("@z", value3);
    	stack.push(map);
    	
    	
    	Assert.assertEquals(true,SQLEvalVisitorUtils.evalExpr(JdbcConstants.MYSQL,"3>2&&2>1"));
    	Assert.assertEquals(true,SQLEvalVisitorUtils.evalExpr(JdbcConstants.MYSQL,"@x>@y",stack));
    	Assert.assertEquals(true, SQLEvalVisitorUtils.evalExpr(JdbcConstants.MYSQL, "@z between 1 and 3", stack));
    }
    
    

    /**
     * test while
     * @throws Exception
     */
    public void test_3() throws Exception {
    	String sql="create or replace procedure sp1()"+
				" begin"+
				" declare @x int;"+
				" set @x=10;"+
				" while @x>0 do"+
				" set @x=@x-1;"+
				" insert into test values(1,1);"+
				" end while;"+
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
     * test loop
     * @throws Exception
     */
    public void test_4() throws Exception {
    	String sql="create or replace procedure sp2()"+
				" begin"+
				" declare @x int;"+
				" loop"+
				" insert into test values(1,1);"+
				" leave label;"+//leave not ok
				" end loop;"+
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
     * test repeat
     * @throws Exception
     */
    public void test_5() throws Exception {
    	String sql="create or replace procedure sp3()"+
				" begin"+
				" declare @x int;"+
				" set @x=0;"+
				" repeat"+
				" insert into test values(1,1);"+
				" set @x=@x+1;"+
				" until @x>10"+
				" end repeat;"+
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
     * test parameter
     * @throws Exception
     */
    public void test_6() throws Exception {
    	String sql="create or replace procedure sp4(@level int,@age int)"+
				" begin"+
    			" insert into test values(1,1);"+
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
    public void test_7() throws Exception {
    	
    	String sql="call sp_name()";
	
    	MySqlStatementParser parser=new MySqlStatementParser(sql);
    	List<SQLStatement> statementList = parser.parseStatementList();
    	SQLStatement statement = statementList.get(0);
    	
    	System.out.println("\n#################### execute procedure #####################");
    	SQLCallStatement call=(SQLCallStatement)statement;
    	MySqlProcedureExecutor.executeCallProcedure(call);
    	
    }
}
