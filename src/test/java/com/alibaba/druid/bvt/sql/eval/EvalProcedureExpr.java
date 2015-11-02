package com.alibaba.druid.bvt.sql.eval;

import junit.framework.TestCase;

import org.junit.Assert;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.visitor.SQLEvalVisitorUtils;
import com.alibaba.druid.util.JdbcConstants;


public class EvalProcedureExpr extends TestCase {
    public void test_if() throws Exception {
    	
    	SQLExpr condition=SQLUtils.toSQLExpr("3>2", JdbcConstants.MYSQL);
    	
        Assert.assertEquals(true,SQLEvalVisitorUtils.eval(JdbcConstants.MYSQL, condition));
        
    }
    
}
