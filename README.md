# druid_procedure
基于druid的一个存储过程模拟执行

关于druid对存储过程的识别的部分
具体修改请看commit
https://github.com/alibaba/druid/pull/954
https://github.com/alibaba/druid/pull/958
https://github.com/alibaba/druid/pull/959
https://github.com/alibaba/druid/pull/963
https://github.com/alibaba/druid/pull/964
https://github.com/alibaba/druid/pull/976




模拟执行部分
目录src\main\java\com\alibaba\druid\sql\dialect\mysql\executor
是模拟执行的类

目录src\test\java\com\alibaba\druid\bvt\sql\mysql下
MySqlCreateProcedureTest.java
MySqlCreateProcedureTest1.java
MySqlCreateProcedureTest2.java
MySqlCreateProcedureTest3.java
MySqlCreateProcedureTest4.java
MySqlCreateProcedureTest5.java
MySqlCreateProcedureTest6.java
是测试存储过程语法识别的


MySqlExecuteProcedureTest0.java是测试存储过程执行的
占时不支持sql语句中带变量