/*
 * Copyright 2015-2100 http://blog.csdn.net/zhujunxxxxx
 */
package com.alibaba.druid.sql.dialect.mysql.executor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Stack;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLDataType;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLAssignItem;
import com.alibaba.druid.sql.ast.statement.SQLCallStatement;
import com.alibaba.druid.sql.ast.statement.SQLSetStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.clause.MySqlCaseStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.clause.MySqlCaseStatement.MySqlWhenStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.clause.MySqlCreateProcedureStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.clause.MySqlDeclareStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.clause.MySqlIfStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.clause.MySqlIfStatement.MySqlElseIfStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.clause.MySqlLeaveStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.clause.MySqlLoopStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.clause.MySqlRepeatStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.clause.MySqlSelectIntoStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.clause.MySqlStatementType;
import com.alibaba.druid.sql.dialect.mysql.ast.clause.MySqlWhileStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlBlockStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.visitor.SQLEvalVisitorUtils;
import com.alibaba.druid.util.JdbcConstants;

/**
 * 
 * @Description: MySql procedure executor
 * @author zz email:455910092@qq.com
 * @date 2015-9-14
 * @version V1.0
 * one method only have a return statement,when meet error need as quickly stop or return
 */
public class MySqlProcedureExecutor {
	
	/**
	 * create table __store_procedure(id int primary key not null auto_increment,procedureName varchar(100),parameterNumber int,source text);
	 */
	public static int SUCCESS=0;
	public static int ERROR=-1;
	public static int NULL_ERROR=-2;//空指针错误
	public static int NOT_INIT_ERROR=-3;//未初始化错误
	public static int LEAVE=-4;
	public static int EMPTY_ERROR=-5;//空错误
	public static int PARSE_ERROR=-6;//解析错误
	public static int NOT_DOUND_ERROR=-7;//存储过程未找到错误
	public static int PARAM_SIZE_ERROR=-8;//参数错误
	public static int OTHER_ERROR=Integer.MAX_VALUE;
	public static int SQL_EXEC_ERROR=-9;//sql执行错误
	public static class VariableObject
	{
		private SQLDataType type;
		private Object Value;
		public VariableObject()
		{
			
		}
		public SQLDataType getType() {
			return type;
		}
		public void setType(SQLDataType type) {
			this.type = type;
		}
		public Object getValue() {
			return Value;
		}
		public void setValue(Object value) {
			Value = value;
		}
		public VariableObject(SQLDataType type,Object value)
		{
			this.type=type;
			this.Value=value;
		}
	}
	
	//variable store stack
	private static Stack<Map<String,Object>> stack=new Stack<Map<String,Object>>();
	private static String wantLeaveLable = null;
	private static Connection connection;
	static
	{
		// init the database connection
		connection=getDruidConnection();
	}
	
	public static int executeCreateProcedure(MySqlCreateProcedureStatement sp,String source)
	{
		int ret=SUCCESS;
		String insert="insert into __store_procedure(procedureName,parameterNumber,source) values(?,?,?)";
		try {
			connection.setAutoCommit(false);
			PreparedStatement stmt=	connection.prepareStatement(insert);
			stmt.setString(1, sp.getName().getSimpleName());
			stmt.setInt(2, sp.getParameters().size());
			stmt.setString(3, source);
			int affectrow=stmt.executeUpdate();
			if(affectrow<=0)
			{
				ret=OTHER_ERROR;
			}
			else
			{
				commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			rollBack();
			ret=OTHER_ERROR;
		}
		return ret;
	}
	
	public static int executeCallProcedure(SQLCallStatement call)
	{
		int ret=SUCCESS;
		MySqlCreateProcedureStatement sp=null;
		String storeProcedureName=null;
		String sql=null;
		if(call==null)
			ret=NULL_ERROR;
		else if(null==(storeProcedureName=call.getProcedureName().getSimpleName()) || storeProcedureName.length()<=0)
		{
			ret=NOT_DOUND_ERROR;
		}
		// 根据存储过程名称到数据库存储过程表里面查询存储过程的源码
		else if(null==(sql=getStoreProcedureSource(storeProcedureName)) || sql.length()<=0)
		{
			ret=NOT_DOUND_ERROR;
		}
		else
		{
			MySqlStatementParser parser=new MySqlStatementParser(sql);
			try {
				List<SQLStatement> statementList = parser.parseStatementList();
		    	SQLStatement statement = statementList.get(0);
		    	sp=(MySqlCreateProcedureStatement)statement;
			} catch (Exception e) {
				ret=PARSE_ERROR;
			}
		}
		if(ret==SUCCESS)
		{
			ret=executeProcedure(sp,call.getParameters());
		}
		return ret;
	}
	public static int executeProcedure(MySqlCreateProcedureStatement sp,List<SQLExpr> parameters)
	{
		int ret=SUCCESS;
		if(sp==null || parameters==null)
		{
			ret=NULL_ERROR;
		}
		else if(sp.getParameters().size()!=parameters.size())
		{
			ret=PARAM_SIZE_ERROR;
		}
		else
		{
			//第一步初始化所有声明的变量
			Map<String,Object> map=new HashMap<String, Object>();
			for (int i = 0; i < sp.getParameters().size(); i++) {
				String key=sp.getParameters().get(i).getName().toString();
				Object value=executeExpression(parameters.get(i));
				map.put(key, value);
				procedureLog("execute parameter Name: "+key+", value: "+value,LogLevel.DEBUG);
			}
			//设置传进来的执行参数
			stack.push(map);
			try {
				//第二步在一个事务中执行所有的statement
				connection.setAutoCommit(false);
				ret=executeBlock(sp.getBlock());
				if(ret == SUCCESS) {
					commit();//commit transaction
				} else {
					rollBack();
					cleanStack();
				}
				//TODO 第三步处理返回值		
				//第四步清理
			} catch (SQLException e) {
				rollBack();//roll back
				cleanStack();//clean stack sate
			}
		}
		
		if(ret!=SUCCESS)
		{
			procedureLog("procedure execute error! ret ="+ret,LogLevel.ERROR);
		}
		
		return ret;
	}
	
	/**
	 * 
	 * @param list
	 * @return 0 means execute success!
	 */
	private static int executeBlock(MySqlBlockStatement block)
	{
		int ret=SUCCESS;
		if(block==null)  
            ret=NULL_ERROR;
		else
			ret=executeStatements(block.getStatementList());
		return ret;
	}
	
	private static int executeStatements(List<SQLStatement> list)
	{
		int ret=SUCCESS;
		if(list==null || list.size()==0)
			ret=NULL_ERROR;
		else{
			for (int i = 0; ret==SUCCESS && i < list.size(); i++) {
				ret=executeStatement(list.get(i));
				//add [support leave label] 20151130:b
				if (list.get(i) instanceof MySqlWhileStatement && ret == LEAVE) { //如果遇到了一个LEAVEL语句，首先判断是否是leave某个标签，如果不是则退出当前循环(什么也不用做)
					if (wantLeaveLable != null) { //说明想退出某一个label循环
						MySqlWhileStatement whileStmt = (MySqlWhileStatement)list.get(i);
						if(whileStmt.getLabelName() != null && whileStmt.getLabelName().equals(wantLeaveLable)) {
							ret = SUCCESS;
						}
					} else {
						ret = SUCCESS;
					}
				}
				//add:e
			}
		}
		return ret;
	}
	
	private static int executeStatement(SQLStatement stmt)
	{
		int ret=SUCCESS;
		switch(MySqlStatementType.getType(stmt))
		{
			case IF:
				procedureLog("execute IF begin", LogLevel.DEBUG);
				ret=executeIf(stmt);
				procedureLog("execute IF end", LogLevel.DEBUG);
				break;
			case WHILE:
				procedureLog("execute WHILE begin", LogLevel.DEBUG);
				ret=executeWhile(stmt);
				procedureLog("execute WHILE end", LogLevel.DEBUG);
				break;
			case CASE:
				procedureLog("execute WHILE begin", LogLevel.DEBUG);
				ret=executeCase(stmt);
				break;
			case BLOCK:
				ret=executeBlock((MySqlBlockStatement)stmt);
				break;
			case LOOP:
				ret=executeLoop(stmt);
				break;
			case REPEAT:
				procedureLog("execute REPEAT begin", LogLevel.DEBUG);
				ret=executeRepeat(stmt);
				procedureLog("execute REPEAT end", LogLevel.DEBUG);
				break;
			case LEAVE:
				ret=executeLeave(stmt);
				break;
			case DECLARE:
				ret=executeDeclare(stmt);
				break;
			case ASSIGN:
				ret=executeAssign(stmt);
				break;
			case DELETE:
			case INSERT:
			case UPDATE:
			case SELECT:
				ret=executeSQL(stmt);
				break;
			case SELECTINTO:
				ret=executeSelectInto(stmt);
				break;
			default:
				break;
		}
		return ret;
	}
	
	private static int executeIf(SQLStatement stmt)
	{
		int ret=SUCCESS;
		int stackSize=stack.size();
		boolean is_need_else=true;
		MySqlIfStatement if_stmt=(MySqlIfStatement)stmt;
		SQLExpr condition=if_stmt.getCondition();
		//判断IF的表达式的真值
		if(executeExpression(condition)==(Object)true)
		{
			ret=executeStatements(if_stmt.getStatements());
			is_need_else=false;
		}
		else
		{
			//遍历执行所有的else if语句
			for (int i = 0; ret==SUCCESS && i < if_stmt.getElseIfList().size(); i++) {
				MySqlElseIfStatement else_stmt=if_stmt.getElseIfList().get(i);
				SQLExpr else_condition=else_stmt.getCondition();
				if(executeExpression(else_condition)==(Object)true)
				{
					ret=executeStatements(else_stmt.getStatements());
					is_need_else=false;
				}
			}
		}
		//执行else分支
		if(is_need_else && if_stmt.getElseItem() != null)
		{
			for (int i = 0; i < if_stmt.getElseItem().getStatements().size(); i++) {
				ret=executeStatements(if_stmt.getElseItem().getStatements());
			}
		}
		recoverStack(stackSize);
		return ret;
	}
	
	private static int executeWhile(SQLStatement stmt)
	{
		int ret=SUCCESS;
		int stackSize=stack.size();
		MySqlWhileStatement while_stmt=(MySqlWhileStatement)stmt;
		SQLExpr condition=while_stmt.getCondition();
		while(true && ret==SUCCESS)
		{
			if(executeExpression(condition)==(Object)true)
			{
				ret=executeStatements(while_stmt.getStatements());
			}
			else
			{
				break;
			}
		}		
		recoverStack(stackSize);
		return ret;
	}
	
	private static int executeLoop(SQLStatement stmt)
	{
		int ret=SUCCESS;
		int stackSize=stack.size();
		MySqlLoopStatement loop_stmt=(MySqlLoopStatement)stmt;
		while(true && ret==SUCCESS)
		{
			ret=executeStatements(loop_stmt.getStatements());
			if(ret==LEAVE)
			{
				break;
			}
		}
		recoverStack(stackSize);
		return ret;
	}
	
	private static int executeRepeat(SQLStatement stmt)
	{
		int ret=SUCCESS;
		int stackSize=stack.size();
		MySqlRepeatStatement repeat_stmt=(MySqlRepeatStatement)stmt;
		SQLExpr search_conditon=repeat_stmt.getCondition();
		while(true)
		{
			ret=executeStatements(repeat_stmt.getStatements());
			if(ret!=SUCCESS)
			{
				break;
			}
			else if(executeExpression(search_conditon)==(Object)true)
			{
				break;
			}
		}
		recoverStack(stackSize);
		return ret;
	}	
	private static int executeLeave(SQLStatement stmt)
	{
		int ret=LEAVE;
		MySqlLeaveStatement leave_stmt=(MySqlLeaveStatement)stmt;
		wantLeaveLable = leave_stmt.getLabelName();
		return ret;
	}
	
	private static int executeCase(SQLStatement stmt)
	{
		int ret=SUCCESS;
		int stackSize=stack.size();
		boolean is_need_else=true;
		MySqlCaseStatement case_stmt=(MySqlCaseStatement)stmt;
		SQLExpr condition=case_stmt.getCondition();
		Object case_value=executeExpression(condition);
		for (int i = 0; i < case_stmt.getWhenList().size(); i++) {
			MySqlWhenStatement when_stmt=case_stmt.getWhenList().get(i);
			SQLExpr when_condition=when_stmt.getCondition();
			Object when_value=executeExpression(when_condition);	
			if(case_value.equals(when_value))
			{
				ret=executeStatements(when_stmt.getStatements());
				is_need_else=false;
				break;
			}
		}
		if(is_need_else)
		{
			ret=executeStatements(case_stmt.getElseItem().getStatements());
		}
		recoverStack(stackSize);
		return ret;
	}
	
	private static int executeDeclare(SQLStatement stmt)
	{
		int ret=SUCCESS;
		MySqlDeclareStatement declare_stmt=(MySqlDeclareStatement)stmt;
		SQLDataType type=declare_stmt.getType();
		SQLExpr default_expr=declare_stmt.getDefaultValue();
		List<SQLExpr> value_list=declare_stmt.getVarList();
		Object defalut_value=null;
		if(default_expr!=null)
		{
			defalut_value=executeExpression(default_expr);	
		}
		Map<String,Object> map=new HashMap<String, Object>();
		for (int i = 0; ret==SUCCESS && i < value_list.size(); i++) {
			String varName=value_list.get(i).toString();
			VariableObject value=new VariableObject(type, defalut_value);
			//System.out.println("[Declare Var Name : " + varName +", Type:"+type.getName()+"]");
			if(FindVariable(varName))
			{
				//have been declare this varName
				ret=OTHER_ERROR;
			}
			else
			{
				map.put(varName, value);
				procedureLog("[Declare Var Name : " + varName +", Type:"+type.getName()+"]",LogLevel.DEBUG);
			}
		}
		stack.push(map);
		return ret;
	}
	
	private static int executeAssign(SQLStatement stmt)
	{
		int ret=SUCCESS;
		SQLSetStatement set_stmt=(SQLSetStatement)stmt;
		List<SQLAssignItem> assign_list=set_stmt.getItems();
		for (int i = 0; ret==SUCCESS && i < assign_list.size(); i++) {
			SQLAssignItem item=assign_list.get(i);
			SQLExpr target=item.getTarget();
			SQLExpr value_expr=item.getValue();
			if (target instanceof SQLVariantRefExpr) {
				SQLVariantRefExpr name_expr=(SQLVariantRefExpr)target;
				String var_name=name_expr.getName();
				if(FindVariable(var_name))
				{
					Object value=executeExpression(value_expr);
					//TODO type check
					//update the variable value
					//System.out.println("[Assign Variable Name :"+var_name +", Value:"+value+"]");
					if(!SetVariable(var_name, value))
					{
						ret=OTHER_ERROR;
					}
					else
					{
						procedureLog("[Assign Variable Name :"+var_name +", Value:"+value+"]",LogLevel.DEBUG);
					}
				}
				else
				{
					procedureLog("[Assign Variable Name "+var_name+" not foud.",LogLevel.ERROR);
					ret=OTHER_ERROR;
				}
	        }
			else
			{
				ret=OTHER_ERROR;
			}
		}
		return ret;
	}
	
	@SuppressWarnings("unused")
	private static int executeOpen(SQLStatement stmt)
	{
		return 0;
	}
	
	@SuppressWarnings("unused")
	private static int executeFetch(SQLStatement stmt)
	{
		return 0;
	}
	
	@SuppressWarnings("unused")
	private static int executeClose(SQLStatement stmt)
	{
		return 0;
	}
	
	private static int executeSQL(SQLStatement stmt)
	{
		int ret=SUCCESS;
		String sql=SQLUtils.toMySqlString(stmt);
		switch(MySqlStatementType.getType(stmt))
		{
			case DELETE:
				try {
					MySqlDeleteStatement delete=(MySqlDeleteStatement)stmt;
					Statement statement=connection.createStatement();
					statement.executeUpdate(sql);
					procedureLog("[execute delete sql]"+sql,LogLevel.DEBUG);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case INSERT:
				try {
					Statement statement=connection.createStatement();
					statement.executeUpdate(sql);
					procedureLog("[execute insert sql]"+sql,LogLevel.DEBUG);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					ret=SQL_EXEC_ERROR;
				}
				break;
			case UPDATE:
				try {
					Statement statement=connection.createStatement();
					statement.executeUpdate(sql);
					procedureLog("[execute update sql]"+sql,LogLevel.DEBUG);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case SELECT:
				//TODO 应该不允许执行单独的sql
				procedureLog("[execute select sql]"+sql,LogLevel.DEBUG);
				break;
			default:
				procedureLog("[stmt type error]"+sql,LogLevel.DEBUG);
				break;
		}
		return ret;
	}
	
	private static String getStoreProcedureSource(String name)
	{
		String source="";
		try
		{
			Statement mysqlStmt=connection.createStatement();
			String sql=String.format("select * from __store_procedure where procedureName=\"%s\"",name);
			ResultSet rs=mysqlStmt.executeQuery(sql);
			
			if(rs.next())
			{
				source= rs.getString("source");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return source;
	}
	
	private static int executeSelectInto(SQLStatement stmt)
	{
		int ret=SUCCESS;
		MySqlSelectIntoStatement selectinto_stmt=(MySqlSelectIntoStatement)stmt;
		String sql=SQLUtils.toSQLString(selectinto_stmt.getSelect(),JdbcConstants.MYSQL);
		try {
			Statement mysqlStmt=connection.createStatement();
			ResultSet rs=mysqlStmt.executeQuery(sql);
			ResultSetMetaData rsmd = rs.getMetaData();
			Object value=null;
			if(selectinto_stmt.getVarList().size()!=rsmd.getColumnCount())//check the insert var_list size is equal resultSet cloumn count
			{
				ret=OTHER_ERROR;
			}
			else if(rs.next())
			{	
				int columnIndex=1;
				for (SQLExpr target : selectinto_stmt.getVarList()) {
					if (target instanceof SQLVariantRefExpr) {
						SQLVariantRefExpr name_expr=(SQLVariantRefExpr)target;
						String var_name=name_expr.getName();
						value=rs.getObject(columnIndex++);
						if(FindVariable(var_name))
						{
							if(!SetVariable(var_name, value))
							{
								ret=OTHER_ERROR;
							}
						}
					}
					else
					{
						ret=OTHER_ERROR;
						System.out.println("target not instanceof SQLVariantRefExpr");
					}
				}
				if(rs.next())//the record for "select into" only allow one record
				{
					ret=OTHER_ERROR;
				}
			}
		} catch (Exception e) {
			ret=OTHER_ERROR;
		}
		return ret;
	}
	
	@SuppressWarnings("unused")
	private static int executeFunction(SQLStatement stmt)
	{
		return 0;
	}
	
	
	private static Object executeExpression(SQLExpr expr)
	{
		//compute expr
		Object value=SQLEvalVisitorUtils.eval(JdbcConstants.MYSQL,expr,stack);
		return value;
	}
	
	private static boolean FindVariable(String varName)
	{
		boolean find=false;
		for (Map<String,Object> item : stack) {
			if(item.containsKey(varName))
			{
				find=true;
				break;
			}
		}
		return find;
	}
	
	/**
	 * update stack variable, search from stack top to stack bottom
	 * @param varName
	 * @param value
	 * @return
	 */
	private static boolean SetVariable(String varName, Object value)
	{
		boolean update=false;
		for (int i = stack.size()-1; i >=0; i--) {
			
			if(stack.elementAt(i).containsKey(varName))
			{
				((VariableObject)stack.elementAt(i).get(varName)).setValue(value);
				update=true;
				break;
			}
		}
		return update;
	}
	
	private static void recoverStack(int oldSize)
	{
		while(oldSize<stack.size())
		{
			Map<String,Object> map=stack.pop();
			if(map!=null)
			{
				//procedureLog("pop begin ========================",LogLevel.INFO);
				for (Entry<String, Object> item : map.entrySet()) {
					
					procedureLog("Var Name:"+item.getKey()+" pop",LogLevel.DEBUG);
				}
				//procedureLog("pop end ==========================",LogLevel.INFO);
			}
		}
	}
	@SuppressWarnings("unused")
	private static void showStack()
	{
		procedureLog("begin*******************************",LogLevel.DEBUG);
		for (int i = 0; i < stack.size(); i++) {
			
			Map<String,Object> map=stack.elementAt(i);
			
			if(map!=null)
			{
				procedureLog(i+" ==========================",LogLevel.DEBUG);
				for (Entry<String, Object> item : map.entrySet()) {
					
					procedureLog("Var Name:"+item.getKey(),LogLevel.DEBUG);
				}
				procedureLog(i+" ==========================",LogLevel.DEBUG);
			}
		}
		
		procedureLog("end*********************************\n",LogLevel.DEBUG);
	}
	
	public static void cleanStack()
	{
		stack.clear();
	}
	
	private static Connection getDruidConnection()
	{
		Properties p = new Properties();
		p.put("driverClassName", "com.mysql.jdbc.Driver");
		p.put("url", "jdbc:mysql://127.0.0.1:3306/sp");
		p.put("username", "root");
		p.put("password", "root");
		p.put("filters", "stat");
		p.put("initialSize", "2");
		p.put("maxActive", "300");
		p.put("maxWait", "60000");
		p.put("timeBetweenEvictionRunsMillis", "60000");
		p.put("minEvictableIdleTimeMillis", "300000");
		p.put("validationQuery", "SELECT 1");
		p.put("testWhileIdle", "true");
		p.put("testOnBorrow", "false");
		p.put("testOnReturn", "false");
		p.put("poolPreparedStatements", "false");
		p.put("maxPoolPreparedStatementPerConnectionSize", "200");
		
		 try {
			DataSource ds = DruidDataSourceFactory.createDataSource(p);
			Connection conn = ds.getConnection();  
			return conn;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return null;
		}
	}
	
	private static void procedureLog(String info,LogLevel level)
	{
		StackTraceElement ste = new Throwable().getStackTrace()[1];
        String log=ste.getFileName() + ":" + ste.getLineNumber()+" "+ste.getMethodName()+" "+info;
        
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        System.out.print(df.format(new Date())+" ");
        if(level==LogLevel.DEBUG)
        {
        	System.out.print("[DEBUG] ");
        }
        if(level==LogLevel.INFO)
        {
        	System.out.print("[INFO] ");
        }
        if(level==LogLevel.WARN)
        {
        	System.out.print("[WARN] ");
        }
        if(level==LogLevel.ERROR)
        {
        	System.out.print("[ERROR] ");
        }
        System.out.println(log);
	}
	
	private static void rollBack()
	{
		try {
			connection.rollback();
		} catch (SQLException e) {
			e.printStackTrace();
		}//commit
	}
	private static void commit()
	{
		try {
			connection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public enum LogLevel
	{
		DEBUG,
		INFO,
		WARN,
		ERROR,
	}
}
