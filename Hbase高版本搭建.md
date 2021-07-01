---
title: Hbase高版本搭建
top: false
cover: false
toc: true
mathjax: false
date: 2021-06-28 07:06:36
author: 情深骚明
img:
coverImg:
password:
summary:
tags: hbase
categories: hbase
---

# hbase

## Hbase2.3.5 部署

参考安装：https://blog.csdn.net/daqu1314/article/details/117257448

### 基本配置

~~~properties
tar -zxvf hbase-2.3.5-bin.tar.gz
rm -rf hbase-2.3.5-bin.tar.gz

vim /etc/profile
# hbase
export HBASE_HOME=/opt/module/hbase-2.3.5
export PATH=$PATH:$HBASE_HOME/bin
source /etc/profile

vim hbase-env.sh

export HBASE_LOG_DIR=${HBASE_HOME}/logs
export JAVA_HOME=/opt/module/jdk1.8.0_212
export HBASE_MANAGES_ZK=false
export HADOOP_HOME=/opt/module/hadoop-3.1.3

vim hbase-site.xml

<configuration>
 <property>
     <name>hbase.rootdir</name>
     <value>hdfs://yaxin01:9820/hbase</value>
 </property>
 
 <property>
     <name>hbase.cluster.distributed</name>
     <value>true</value>
 </property>
 
 <property>
     <name>hbase.zookeeper.quorum</name>
     <value>yaxin01,yaxin02,yaxin03</value>
 </property>
 
 <property>
     <name>zookeeper.znode.parent</name>
     <value>/hbase</value>
 </property>
 
  <property>
     <name>hbase.tmp.dir</name>
     <value>/opt/module/hbase-2.3.5/data</value>
 </property>
 
 <property>
     <name>hbase.unsafe.stream.capability.enforce</name>
     <value>false</value>
 </property>
</configuration>

regionservers
yaxin01
yaxin02
yaxin03

scp -r core-site.xml hdfs-site.xml /opt/module/hbase-2.3.5/conf/

scp -r hbase-2.3.5 yaxin02:`pwd`
scp -r hbase-2.3.5 yaxin03:`pwd`


scp -r /etc/profile yaxin02:/etc
scp -r /etc/profile yaxin03:/etc
source /etc/profile
~~~

### 启动服务

~~~properties
start-hbase.sh 在那台启动，那台就是hbasemaster
hbase-daemon.sh start master
hbase-daemon.sh start regionserver

hbase-daemon.sh stop master
hbase-daemon.sh stop regionserver
~~~

### 进程情况

~~~properties
------------------------ jps yaxin01 ------------------------
32112 JarBootstrapMain
23873 DataNode
24162 NodeManager
23701 NameNode
32389 Jps
31738 HMaster
19180 QuorumPeerMain
------------------------ jps yaxin02 ------------------------
16288 Jps
11478 ResourceManager
11289 DataNode
11625 NodeManager
15945 HRegionServer
9598 QuorumPeerMain
------------------------ jps yaxin03 ------------------------
9601 DataNode
9681 SecondaryNameNode
13602 HRegionServer
8085 QuorumPeerMain
13964 Jps
9791 NodeManager
~~~

### 端口

http://yaxin01:16010/

## Phoneix 连接 Hbase2.3.5

这个展示没有找到适合Hbase的连接

## HBase 使用方式

重要的是Java api，hbase就是一个数据库

### Hbase shell

参照：https://www.yuque.com/chenshiba/glon0c/clnus6

#### 基础命令集

~~~properties
list | exit | help | status | whoami

操作的类型
general | ddl | namespace | dml | tools | replication | snapshots | configuration | quotas | security
procedures | visibility labels | rsgroup
~~~

#### 创建命令空间

表名和列表必须都是字符串的形式，必须都是单引号的形式。

~~~properties
创建表空间
create_namespace 'yaxin'

创建表
create 'yaxin:yaxin',{'NAME'=>'cf1',VERSIONS=>1},{'NAME'=>'cf2',VERSIONS=>3},{'NAME'=>'info',VERSIONS=>2}

create 'yaxin:member','member_id','address','info'

获取表的描述，两种方式，稍微借助SQL的一些关键字
list
describe 'yaxin:member'
desc 'yaxin:member' 
desc 'yaxin:member' 

disable 'yaxin:member' 


删除列簇member_id，只能删除列簇，不能添加列簇
alter 'yaxin:member' ,{NAME=>'member_id',METHOD=>'delete'}

删除表
disable 'yaxin:member' 
drop 'yaxin:member' 

注意：在hbase客户端的时候，我们可以善于利用hbase的help命令

查询表是否存在：exists 'yaxin:member'
查看HDFS文件存储路径：/hbase/data

插入数据：
member ---> 表名 guojing ---> rowkey info ---> 列簇
put 'yaxin:member','guojing','info:age','24'
put 'yaxin:member','guojing','info:birthday','1987-06-17'
put 'yaxin:member','guojing','info:company','alibaba'
put 'yaxin:member','guojing','address:country','china'
put 'yaxin:member','guojing','address:city','hangzhou'
put 'yaxin:member','guojing','address:province','zhejiang'

put 'yaxin:member','yangkang','info:birthday','1987-04-17'
put 'yaxin:member','yangkang','info:favorite','movie'
put 'yaxin:member','yangkang','info:company','alibaba'
put 'yaxin:member','yangkang','address:country','china'
put 'yaxin:member','yangkang','address:province','guangdong'
put 'yaxin:member','yangkang','address:city','jieyang'
put 'yaxin:member','yangkang','address:town','xianqiao'

put 'yaxin:member','xiaolongnv','info:age','18'
put 'yaxin:member','xiaolongnv','info:sex','0'

获取数据，表的数据都在内存当中，如果想要落地，只能收到落地：flush 'yaxin:member'，没有达到阈值，所以只能flush
get 'yaxin:member','guojing'
~~~

#### 查看HDFS上的文件

![HDFS上的文件](Hbase高版本搭建/image-20210628110820260.png)

可以看出，里面是没有数据，落地到StoreFile对应的Block

现在的数据都在内存中，手动讲内存中的数据落地到Block中：flush 'yaxin:member'

~~~properties
那些数据都是乱码的，写的时候，数据块中有Header(Entry) ---> Key-Value
不能直接get 表面，必须输入一个主键，主键类似于ID号，RowKey，
~~~

### 数据模型

RowKey是一行数据的唯一标识。

RowKey + 列簇 ===> 这个就是一行数据

![数据模型](Hbase高版本搭建/image-20210628111649705.png)

查询的时候，可以精确到某个列簇下的某个列。。。

### 更新列的数据

~~~properties
put 'yaxin:member','guojing','info:age','9999'
~~~

![更新之后的数据](Hbase高版本搭建/image-20210628111943862.png)

~~~properties
获取到某个指定的时间戳的数
get 'yaxin:member','guojing',{NAME=>'info:age',TIMESTAMP=>'1624807128'}
~~~

是按照字典的顺序排序的，全表扫描的时候一般都是和过滤器就行使用

### 查询

~~~properties
scan 'yaxin:member',{COLUMNS=>'info'}指定列簇
scan 'yaxin:member',{COLUMNS=>'info'}

指定版本
scan 'yaxin:member',{COLUMNS=>'info',VERSIONS=>5}

开启Raw模式，会把那些已经添加删除标识但是未实际删除的数据都显示出来
scan 'yaxin:member',{COLUMNS=>'info',RAW=>true}

列的过滤
scan 'yaxin:member',{COLUMNS=>['info','address']}

可以指定到列簇或者列名
也可以使用类似于where的方式
scan 'yaxin:member',{COLUMNS=>'info',STARTROW=>'guojing',ENDROW=>'yangkang'}
scan 'yaxin:member',{FILTER=>"PrefixFilter('y')"}
scan 'yaxin:member',{TIMERANGE=>[1624807128,1624807200]}
~~~

### Region操作

~~~properties
1）移动region
2）开启关闭region
3）手动split
4）手动split
4）触发major compation
~~~

### Hbase shell 总结

~~~properties
创建命名空间
查看命名空间
修改命名空间得信息
DML和DDL操作
~~~

### Hbase API

~~~properties
package com.wmy.hbase_api.ddl;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

/**
 * ClassName:HbaseDDL
 * Package:com.wmy.hbase_api.ddl
 *
 * @date:2021/6/28 14:09
 * @author:数仓开发工程师
 * @email:2647716549@qq.com
 * @Description: hbase ddl languages
 */
public class HbaseDDL {

    // 代码优化
    private static Admin admin = null;
    private static Connection conn = null;
    private static Configuration conf = null;

    static {
        try {
            // 获取HBase配置文件
            // 新的API：HBaseConfiguration#create()
            conf = HBaseConfiguration.create();

            // 报错误：org.apache.hadoop.security.HadoopKerberosName.setRuleMechanism(Ljava/lang/String;)V
            // 导入这个依赖就不会报错误了：hadoop-auth
            conf.set("hbase.zookeeper.quorum", "192.168.22.140");

            // 获取连接
            conn = ConnectionFactory.createConnection(conf);
            admin = conn.getAdmin();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭的方法
     * @param conn 连接
     * @param admin 操作
     */
    public static void close(Connection conn, Admin admin) {
        if (conn != null || admin != null) {
            try {
                conn.close();
                admin.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 判断表是否存在
     * @param tableName 表名
     * @return
     * @throws IOException
     */
    public static boolean tableExists(String tableName) throws IOException {
        // 判断表是否存在
        boolean tableExists = admin.tableExists(TableName.valueOf(tableName));

        // 关闭资源
        admin.close();

        return tableExists;
    }

    /**
     * 创建表
     * @param tableName 表名
     * @param cfs 列簇
     * @throws IOException
     */
    public static void createTable(String tableName, String... cfs) throws IOException {
        if (tableExists(tableName)) {
            System.out.println("表名已存在：" + tableName);
        }

        // 创建表操作
        HTableDescriptor hTableDescriptor = new HTableDescriptor(TableName.valueOf(tableName));
        for (String cf : cfs) {
            HColumnDescriptor hColumnDescriptor = new HColumnDescriptor(cf);
            hTableDescriptor.addFamily(hColumnDescriptor);
        }

        // 创建表操作
        admin.createTable(hTableDescriptor);
        System.out.println("表创建成功！！！");
        admin.close();
    }

    /**
     * 删除表的操作
     * @param tableName 表名
     * @throws IOException
     */
    public static void deleteTable(String tableName) throws IOException {
        // 下线表
        admin.disableTable(TableName.valueOf(tableName));
        // 删除表
        admin.deleteTable(TableName.valueOf(tableName));
        System.out.println("表已删除成功！！！");
        admin.close();
    }


    /**
     * 增删改查
     * 如果进行批量插入：插入的数据都是集合，rowkey也得是多个，cn是多个，使用一个集合的方式来进行插入就行
     * 实际过程当中，循环调用，在控制台上打印，封装成putList集合，统一调用一个put对象
     * @param tableName 表名
     * @param rowKey    rowkey
     * @param cf        列簇
     * @param cn        列名
     * @param value     值
     */
    public static void putData(String tableName, String rowKey, String cf, String cn, String value) throws Exception{
        // 获取表对象
        // 如何去查看过时的方法，可以看它的注释文档就可以明白：ConnectionFactory
        Table table = conn.getTable(TableName.valueOf(tableName));
        Put put = new Put(Bytes.toBytes(rowKey));
        put.addColumn(Bytes.toBytes(cf), Bytes.toBytes(cn), Bytes.toBytes(value));
        table.put(put);
        table.close();
        System.out.println("插入数据成功！！！");
    }

    /**
     * 删除操作：是否可以删除粒度到列勒
     * delete：可以删除单条数据和删除多条数据
     * @param tableName
     * @param rowKey
     * @param cf
     * @param cn
     */
    public static void delete(String tableName, String rowKey, String cf, String cn) throws IOException {
        // 创建Table对象
        Table table = conn.getTable(TableName.valueOf(tableName));

        // 创建delete对象
        // 一个rowKey对应一个Delete对象
        Delete delete = new Delete(Bytes.toBytes(rowKey));

        // 删除指定列的所有版本，这个是最常用的。column在添加数据的更新数据的时候，不是真正的删除数据
        // 多个版本的时候，可以删除指定版本，要么都指定addColumns
        //delete.addColumns(Bytes.toBytes(cf), Bytes.toBytes(cn));
        // 可以更细粒度的去控制如何去删除这些数据
        table.delete(delete);
        table.close();
    }

    // 全表扫描
    public static void scanTable(String tableName) throws IOException {
        // 获取表对象
        Table table = conn.getTable(TableName.valueOf(tableName));

        // 构建扫描器
        Scan scan = new Scan();

        ResultScanner results = table.getScanner(scan);
        for (Result result : results) {
            Cell[] cells = result.rawCells();
            for (Cell cell : cells) {
                System.out.println(Bytes.toString(CellUtil.cloneRow(cell)) + "\t" +
                        Bytes.toString(CellUtil.cloneFamily(cell)) + "\t" +
                        Bytes.toString(CellUtil.cloneQualifier(cell)) + "\t" +
                        Bytes.toString(CellUtil.cloneValue(cell)));
            }
        }

    }

    // 获取指定列簇：列的数据
    public static void getData(String tableName, String rowKey, String cf, String cn) throws IOException {
        // 获取表对象
        Table table = conn.getTable(TableName.valueOf(tableName));
        // 创建一个Get对象
        Get get = new Get(Bytes.toBytes(rowKey));
        get.addColumn(Bytes.toBytes(cf), Bytes.toBytes(cn));
        get.readVersions(5);

        // 获取数据库的操作
        Result result = table.get(get);
        Cell[] cells = result.rawCells();
        for (Cell cell : cells) {
            System.out.println(Bytes.toString(CellUtil.cloneRow(cell)) + "\t" +
                    Bytes.toString(CellUtil.cloneFamily(cell)) + "\t" +
                    Bytes.toString(CellUtil.cloneQualifier(cell)) + "\t" +
                    Bytes.toString(CellUtil.cloneValue(cell)));
        }
    }

    // 测试代码
    public static void main(String[] args) throws Exception {
        // 判断表是否存在
        //System.out.println(tableExists("staff"));

        // 创建表
        //createTable("staff","info");

        // 删除表
        //deleteTable("staff");

        // 添加数据
        //putData("staff", "1001", "info", "name", "吴明洋");

        // 删除数据
        //delete("staff", "1001", null, null);

        // 全表扫描
        //scanTable("staff");

        // 获取到指定列
        getData("staff","1001","info","name");

        // 最后进行统一的关闭
        close(conn, admin);
    }
}

~~~

HBASE只是一个存储工具，并不能进行一个分析，分析的是HIVE，只要是分析框架和计算引擎都是可以来集成HBASE

分析完的数据在写到HBASE

## HBASE和MR集合

### Map Reduce要持有HBASE的一些jar包

~~~properties
[root@yaxin01 ~]# hbase mapredcp
/opt/module/hbase-2.3.5/lib/shaded-clients/hbase-shaded-mapreduce-2.3.5.jar:
/opt/module/hbase-2.3.5/lib/client-facing-thirdparty/audience-annotations-0.5.0.jar:
/opt/module/hbase-2.3.5/lib/client-facing-thirdparty/commons-logging-1.2.jar:
/opt/module/hbase-2.3.5/lib/client-facing-thirdparty/htrace-core4-4.2.0-incubating.jar:
/opt/module/hbase-2.3.5/lib/client-facing-thirdparty/log4j-1.2.17.jar:
/opt/module/hbase-2.3.5/lib/client-facing-thirdparty/slf4j-api-1.7.30.jar
~~~

### 环境变量配置的导入

~~~properties
export HBASE_HOME=/opt/module/hbase-2.3.5
export HADOOP_HOME=/opt/module/hadoop-3.1.3
export HADOOP_CLASSPATH=`${HBASE_HOME}/bin/hbase mapredcp`
~~~

### 配置永久生效

~~~properties
# hadoop-path
export HADOOP_HOME=/opt/module/hadoop-3.1.3
export HADOOP_CONF_DIR=/opt/module/hadoop-3.1.3/etc/hadoop
export HADOOP_CLASSPATH=`hadoop classpath`
export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin

# hbase
export HBASE_HOME=/opt/module/hbase-2.3.5
export PATH=$PATH:$HBASE_HOME/bin
~~~

并在hadoop-env.sh 中配置，（注意：在for循环之后配置）

export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:/opt/module/hbase-2.3.5/lib/*

然后重启hadoop,zookeeper,hbase

### 运行官方案例

这个不能用，提交jar包的时候

### tsv和csv的区别

~~~java
csv是以逗号分隔，tsc是按\t分隔的，Linux的后缀名是没有意义的

[root@yaxin01 hbase-2.3.5]# cat fruit.tsv 
1001	Apple	Red
1002	Pear	Yellow
1003	Pineapple	Yellow

hdfs dfs -put fruit.tsv /
~~~

### 执行MapReduce创建表

~~~properties
Hbase(main):001:0> create 'fruit','info'

yarn jar /opt/module/hbase-2.3.5/lib/hbase-server-1.3.1.jar \
importtsv \
-Dimporttsv.columns=HBASE_ROW_KEY,info:name,info:color fruit \
hdfs://yaxin01:9820/input_fruit


put 'fruit','1001','info:name','Apple'
put 'fruit','1001','info:color','Red'
put 'fruit','1002','info:name','Pear'
put 'fruit','1002','info:color','Yellow'
put 'fruit','1003','info:name','Pineapple'
put 'fruit','1003','info:color','Yellow'
~~~

经过测试，这个也是不能使用的

~~~properties
hbase(main):009:0> scan 'fruit'
ROW                         COLUMN+CELL                                                                 
 1001                       column=info:color, timestamp=2021-06-28T19:59:08.242, value=Red             
 1001                       column=info:name, timestamp=2021-06-28T19:59:08.205, value=Apple            
 1002                       column=info:color, timestamp=2021-06-28T19:59:08.343, value=Yellow          
 1002                       column=info:name, timestamp=2021-06-28T19:59:08.290, value=Pear             
 1003                       column=info:color, timestamp=2021-06-28T19:59:09.122, value=Yellow          
 1003                       column=info:name, timestamp=2021-06-28T19:59:08.402, value=Pineapple        
3 row(s)
~~~

### 从HBASE中读写到HBASE

#### 依赖

~~~properties
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.wmy</groupId>
    <artifactId>HBase_API</artifactId>
    <version>1.0-SNAPSHOT</version>
    <properties>
        <hbase.version>2.3.5</hbase.version>
        <hadoop.version>3.1.3</hadoop.version>
    </properties>

    <dependencies>
        <!-- https://mvnrepository.com/artifact/org.apache.hbase/hbase-client -->
        <dependency>
            <groupId>org.apache.hbase</groupId>
            <artifactId>hbase-client</artifactId>
            <version>${hbase.version}</version>
        </dependency>

        <!-- https://mvnrepository.com/artifact/org.apache.hbase/hbase-common -->
        <dependency>
            <groupId>org.apache.hbase</groupId>
            <artifactId>hbase-common</artifactId>
            <version>${hbase.version}</version>
        </dependency>

        <!-- https://mvnrepository.com/artifact/org.apache.hbase/hbase-server -->
        <dependency>
            <groupId>org.apache.hbase</groupId>
            <artifactId>hbase-server</artifactId>
            <version>${hbase.version}</version>
        </dependency>

        <dependency>
            <groupId>org.apache.hbase</groupId>
            <artifactId>hbase-mapreduce</artifactId>
            <version>${hbase.version}</version>
        </dependency>

        <!-- https://mvnrepository.com/artifact/org.apache.hadoop/hadoop-client -->
        <dependency>
            <groupId>org.apache.hadoop</groupId>
            <artifactId>hadoop-client</artifactId>
            <version>${hadoop.version}</version>
        </dependency>

        <!-- https://mvnrepository.com/artifact/org.apache.hadoop/hadoop-common -->
        <dependency>
            <groupId>org.apache.hadoop</groupId>
            <artifactId>hadoop-common</artifactId>
            <version>${hadoop.version}</version>
        </dependency>

        <!-- https://mvnrepository.com/artifact/org.apache.hadoop/hadoop-hdfs -->
        <dependency>
            <groupId>org.apache.hadoop</groupId>
            <artifactId>hadoop-hdfs</artifactId>
            <version>${hadoop.version}</version>
        </dependency>

        <dependency>
            <groupId>org.apache.hadoop</groupId>
            <artifactId>hadoop-auth</artifactId>
            <version>${hadoop.version}</version>
        </dependency>


        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.1</version>
        </dependency>

    </dependencies>

    <!--  Maven自动打包插件  -->
    <build>
        <plugins>
            <!-- 在maven项目中既有java又有scala代码时配置 maven-scala-plugin 插件打包时可以将两类代码一起打包 -->
            <plugin>
                <groupId>org.scala-tools</groupId>
                <artifactId>maven-scala-plugin</artifactId>
                <version>2.15.2</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>compile</goal>
                            <goal>testCompile</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

            <!-- maven 打jar包需要插件 -->
            <plugin>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>2.6</version>
                <configuration>
                    <!-- 设置false后是去掉 MySpark-1.0-SNAPSHOT-jar-with-dependencies.jar 后的 “-jar-with-dependencies” -->
                    <!--<appendAssemblyId>false</appendAssemblyId>-->
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                    <!--                    <archive>-->
                    <!--                        <manifest>-->
                    <!--                            <mainClass>wmy.bigdata.musicProject.ods.ProduceClientLogToHDFS</mainClass>-->
                    <!--                        </manifest>-->
                    <!--                    </archive>-->
                </configuration>
                <executions>
                    <execution>
                        <id>make-assembly</id>
                        <phase>package</phase>
                        <goals>
                            <goal>assembly</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>8</source>
                    <target>8</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
~~~

#### Mapper

~~~java
package com.wmy.hbase_mr;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

/**
 * ClassName:FruitMapper
 * Package:com.wmy.hbase_mr
 *
 * @date:2021/6/29 7:55
 * @author:数仓开发工程师
 * @email:2647716549@qq.com
 * @Description:
 */
// Put 这个对象是可以随便写的
public class FruitMapper extends TableMapper<ImmutableBytesWritable, Put> {
    // 将HBASE中的表迁移到MR中
    @Override
    protected void map(ImmutableBytesWritable key, Result value, Context context) throws IOException, InterruptedException {
        Put put = new Put(key.get());
        Cell[] cells = value.rawCells();
        for (Cell cell : cells) {
            if ("name".equals(Bytes.toString(CellUtil.cloneQualifier(cell)))) {
                put.add(cell);
            }
        }
        context.write(key,put); // 将数据给写出去
    }
}

~~~

#### Reducer

~~~java
package com.wmy.hbase_mr;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.io.NullWritable;

import java.io.IOException;
import java.util.Iterator;

/**
 * ClassName:FruitReducer
 * Package:com.wmy.hbase_mr
 *
 * @date:2021/6/29 7:55
 * @author:数仓开发工程师
 * @email:2647716549@qq.com
 * @Description:
 */
public class FruitReducer extends TableReducer<ImmutableBytesWritable, Put, NullWritable> {
    @Override
    protected void reduce(ImmutableBytesWritable key, Iterable<Put> values, Context context) throws IOException, InterruptedException {
        // 遍历写出
        for (Put value : values) {
            context.write(NullWritable.get(), value);
        }
    }
}

~~~

#### Driver

~~~java
package com.wmy.hbase_mr;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * ClassName:FruitDriver
 * Package:com.wmy.hbase_mr
 *
 * @date:2021/6/29 7:55
 * @author:数仓开发工程师
 * @email:2647716549@qq.com
 * @Description:
 */
public class FruitDriver extends Configuration implements Tool {
    private Configuration conf = null;

    public int run(String[] args) throws Exception {
        // 封装的jar，Map Reduce 类的信息
        Job job = Job.getInstance(conf);

        // 指定Driver类的信息
        job.setJarByClass(FruitDriver.class);

        // 指定Mapper类的信息
        job.setMapperClass(FruitMapper.class);
        TableMapReduceUtil.initTableMapperJob("fruit",new Scan(),FruitMapper.class, ImmutableBytesWritable.class, Put.class,job);

        // 指定Reduce类的信息
        TableMapReduceUtil.initTableReducerJob("fruit_mr",FruitReducer.class,job);

        boolean result = job.waitForCompletion(true);
        return result ? 0 : 1;
    }

    public void setConf(Configuration conf) {
        this.conf = conf;
    }

    public Configuration getConf() {
        return conf;
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = HBaseConfiguration.create();
        int run = ToolRunner.run(conf, new FruitDriver(), args);
        System.exit(run);
    }
}

~~~

#### hbase建表和运行jar包

~~~properties
hbase(main):003:0> create 'fruit_mr','info'
Created table fruit_mr
Took 0.7161 seconds                                                                                     
=> Hbase::Table - fruit_mr
hbase(main):004:0> list
TABLE                                                                                                   
fruit                                                                                                   
fruit_mr                                                                                                
staff                                                                                                   
3 row(s)
Took 0.0108 seconds                                                                                     
=> ["fruit", "fruit_mr", "staff"]
hbase(main):005:0> scan 'fruit_mr'
ROW                         COLUMN+CELL                                                                 
 1001                       column=info:name, timestamp=2021-06-28T19:59:08.205, value=Apple            
 1002                       column=info:name, timestamp=2021-06-28T19:59:08.290, value=Pear             
 1003                       column=info:name, timestamp=2021-06-28T19:59:08.402, value=Pineapple        
3 row(s)
Took 0.0530 seconds 

yarn jar HBase_API-1.0-SNAPSHOT-jar-with-dependencies.jar com.wmy.hbase_mr.FruitDriver
~~~

HBase读和写操作都是可以使用Java API来进行实现的

### 从HDFS中读写到HBASE

相对路径是HDFS，默认的是file:///路径了

#### HDFSMapper

~~~java
package com.wmy.hdfsToHbase;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

/**
 * ClassName:HDFSMapper
 * Package:com.wmy.hdfsToHbase
 *
 * @date:2021/6/29 9:14
 * @author:数仓开发工程师
 * @email:2647716549@qq.com
 * @Description: 业务尽量是放到Map端
 */
public class HDFSMapper extends Mapper<LongWritable, Text, NullWritable, Put> {
    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        // 获取一行数据
        String line = value.toString();

        // 切割
        String[] fields = line.split("\t");

        // 封装成Put对象
        Put put = new Put(Bytes.toBytes(fields[0]));

        // 往put对象放一些属性字段
        put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"), Bytes.toBytes(fields[1]));
        put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("color"), Bytes.toBytes(fields[2]));

        // 写出去
        context.write(NullWritable.get(), put); // 这个没有办法往外面提取，只能在这个方法里面
    }
}

~~~

#### HDFSReducer

~~~java
package com.wmy.hdfsToHbase;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.io.NullWritable;

import java.io.IOException;

/**
 * ClassName:HDFSReducer
 * Package:com.wmy.hdfsToHbase
 *
 * @date:2021/6/29 9:14
 * @author:数仓开发工程师
 * @email:2647716549@qq.com
 * @Description:
 */
public class HDFSReducer extends TableReducer<NullWritable, Put, NullWritable> {
    @Override
    protected void reduce(NullWritable key, Iterable<Put> values, Context context) throws IOException, InterruptedException {
        for (Put value : values) {
            context.write(NullWritable.get(),value);
        }
    }
}

~~~

#### HDFSDriver

~~~~java
package com.wmy.hdfsToHbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * ClassName:HDFSDriver
 * Package:com.wmy.hdfsToHbase
 *
 * @date:2021/6/29 9:14
 * @author:数仓开发工程师
 * @email:2647716549@qq.com
 * @Description:
 */
public class HDFSDriver extends Configuration implements Tool {
    private Configuration conf = null;

    @Override
    public int run(String[] args) throws Exception {
        // 获取JOb对象
        Job job = Job.getInstance(conf);

        // 设置Driver类信息
        job.setJarByClass(HDFSDriver.class);

        // 设置Mapper类的信息
        job.setMapperClass(HDFSMapper.class);
        job.setMapOutputKeyClass(NullWritable.class);
        job.setMapOutputValueClass(Put.class);

        // 设置Reducer类的信息
        TableMapReduceUtil.initTableReducerJob(
                "fruit_hdfs",
                HDFSReducer.class,
                job
        );

        // 设置输入路径
        FileInputFormat.setInputPaths(job, args[0]);

        // 提交这个任务
        boolean result = job.waitForCompletion(true);
        return result ? 0 : 1;
    }

    @Override
    public void setConf(Configuration conf) {
        this.conf = conf;
    }

    @Override
    public Configuration getConf() {
        return conf;
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = HBaseConfiguration.create();
        int run = ToolRunner.run(conf, new HDFSDriver(), args);
        System.exit(run);
    }
}

~~~~

#### Hbase shell 建表和提交jar包

~~~properties
hbase(main):006:0> create 'fruit_hdfs','info'
Created table fruit_hdfs
Took 0.6395 seconds                                                                                     
=> Hbase::Table - fruit_hdfs

yarn jar HBase_API-1.0-SNAPSHOT-jar-with-dependencies.jar com.wmy.hdfsToHbase.HDFSDriver /fruit.tsv

~~~

#### 查看结果

~~~properties
hbase(main):007:0> scan 'fruit_hdfs'
ROW                         COLUMN+CELL                                                                 
 1001                       column=info:color, timestamp=2021-06-28T21:35:43.283, value=Red             
 1001                       column=info:name, timestamp=2021-06-28T21:35:43.283, value=Apple            
 1002                       column=info:color, timestamp=2021-06-28T21:35:43.283, value=Yellow          
 1002                       column=info:name, timestamp=2021-06-28T21:35:43.283, value=Pear             
 1003                       column=info:color, timestamp=2021-06-28T21:35:43.283, value=Yellow          
 1003                       column=info:name, timestamp=2021-06-28T21:35:43.283, value=Pineapple        
3 row(s)
Took 0.1366 seconds
~~~

![HBASE web端](Hbase高版本搭建/image-20210629093915433.png)

### 总结

~~~properties
在工作中的，使用的都是新的API，新的API 的传入的参数可能都是不一样的。
~~~

## HBASE和HIVE集合

#### HBASE和HIVE的对比

~~~properties
hive：
	是一个纯粹的一个分析框架，因为它练元数据都是不存的。
	HBASE是一个纯粹的也给存储框架
ELK：
	数据清洗，有专门的ETL工程师，按照业务线，写MR和SQL，不要去做，而且还得经常加班，一般都是晚上做
	业务线：取数操作，技术含量比较低
核心：
	MR
	Hive是也给分析框架
	HBASE是一个存储框架 ---> 面向列的非关系型数据库，也是可以存关系型数据库
	HBASE延迟低，可以做在线业务，但是一般很少，现在都是做Kyin这个引擎
	
~~~

#### 环境准备

~~~properties
写Hive SQL来分析数据，hive 读 HBASE数据
hive得有HBASE的jar包

# hive-path
export HIVE_HOME=/opt/module/hive-3.1.2
export PATH=$PATH:$HIVE_HOME/bin

# hbase
export HBASE_HOME=/opt/module/hbase-2.3.5
export PATH=$PATH:$HBASE_HOME/bin

ln -s $HBASE_HOME/lib/hbase-common-2.3.5.jar $HIVE_HOME/lib/hbase-common-2.3.5.jar
ln -s $HBASE_HOME/lib/hbase-server-2.3.5.jar $HIVE_HOME/lib/hbase-server-2.3.5.jar
ln -s $HBASE_HOME/lib/hbase-client-2.3.5.jar $HIVE_HOME/lib/hbase-client-2.3.5.jar
ln -s $HBASE_HOME/lib/hbase-protocol-2.3.5.jar $HIVE_HOME/lib/hbase-protocol-2.3.5.jar
ln -s $HBASE_HOME/lib/hbase-it-2.3.5.jar $HIVE_HOME/lib/hbase-it-2.3.5.jar
ln -s $HBASE_HOME/lib/hbase-hadoop2-compat-2.3.5.jar $HIVE_HOME/lib/hbase-hadoop2-compat-2.3.5.jar
ln -s $HBASE_HOME/lib/hbase-hadoop-compat-2.3.5.jar $HIVE_HOME/lib/hbase-hadoop-compat-2.3.5.jar

这个是没有的 -------------------
ln -s $HBASE_HOME/lib/htrace-core-3.1.0-incubating.jar$HIVE_HOME/lib/htrace-core-3.1.0-incubating.jar

rm -rf hbase-common-2.3.5.jar
rm -rf hbase-server-2.3.5.jar
rm -rf hbase-client-2.3.5.jar
rm -rf hbase-protocol-2.3.5.jar
rm -rf hbase-it-2.3.5.jar
rm -rf hbase-hadoop2-compat-2.3.5.jar
rm -rf hbase-hadoop-compat-2.3.5.jar
rm -rf htrace-core-3.1.0-incubating.jar
~~~



#### 下载htrace

![Maven 官网去下载](Hbase高版本搭建/image-20210629100336660.png)

#### 配置hive-site.xml

~~~properties
<property>
 <name>hive.zookeeper.quorum</name>
 <value>yaxin01,yaxin02,yaxin03</value>
 <description>The list of ZooKeeper servers to talk to. This is only needed for read/write locks.</description>
</property>

<property>
 <name>hive.zookeeper.client.port</name>
 <value>2181</value>
 <description>The port of ZooKeeper servers to talk to. This is only needed for read/write locks.</description>
</property>
~~~

#### 重新启动

重新启动 hbase zookeeper haddoop

并开启hadoop zookeeper hive-metastore.sh hbase

#### 案例：hive和hbase映射

##### 创建表

~~~sql
hive (yaxin)> CREATE TABLE hive_hbase_emp_table(
            > empno int,
            > ename string,
            > job string,
            > mgr int,
            > hiredate string,
            > sal double,
            > comm double,
            > deptno int)
            > STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
            > WITH SERDEPROPERTIES ("hbase.columns.mapping" =
            > ":key,info:ename,info:job,info:mgr,info:hiredate,info:sal,info:co
            > mm,info:deptno")
            > TBLPROPERTIES ("hbase.table.name" = "hbase_emp_table");
OK
Time taken: 4.548 seconds
hive (yaxin)> CREATE TABLE emp(
            > empno int,
            > ename string,
            > job string,
            > mgr int,
            > hiredate string,
            > sal double,
            > comm double,
            > deptno int)
            > row format delimited fields terminated by '\t';
OK
Time taken: 0.141 seconds
~~~

##### 数据展示

emp.txt 的数据

~~~properties
7369	SMITH	CLERK	7902	1980/12/17	800		20
7499	ALLEN	SALESMAN	7698	1981/2/20	1600	300	30
7521	WARD	SALESMAN	7698	1981/2/22	1250	500	30
7566	JONES	MANAGER	7839	1981/4/2	2975		20
7654	MARTIN	SALESMAN	7698	1981/9/28	1250	1400	30
7698	BLAKE	MANAGER	7839	1981/5/1	2850		30
7782	CLARK	MANAGER	7839	1981/6/9	2450		10
7788	SCOTT	ANALYST	7566	1987/4/19	3000		20
7839	KING	PRESIDENT		1981/11/17	5000		10
7844	TURNER	SALESMAN	7698	1981/9/8	1500	0	30
7876	ADAMS	CLERK	7788	1987/5/23	1100		20
7900	JAMES	CLERK	7698	1981/12/3	950		30
7902	FORD	ANALYST	7566	1981/12/3	3000		20
7934	MILLER	CLERK	7782	1982/1/23	1300		10
~~~

##### 插入数据

~~~properties
load data local inpath '/opt/module/emp.txt' into table emp;
insert into table hive_hbase_emp_table select * from emp;
~~~

##### 成功展示

~~~properties
hive (yaxin)> select * from hive_hbase_emp_table;
OK
hive_hbase_emp_table.empno	hive_hbase_emp_table.ename	hive_hbase_emp_table.job	hive_hbase_emp_table.mgr	hive_hbase_emp_table.hiredate	hive_hbase_emp_table.sal	hive_hbase_emp_table.comm	hive_hbase_emp_table.deptno
7369	SMITH	CLERK	7902	1980/12/17	800.0	NULL	20
7499	ALLEN	SALESMAN	7698	1981/2/20	1600.0	300.0	30
7521	WARD	SALESMAN	7698	1981/2/22	1250.0	500.0	30
7566	JONES	MANAGER	7839	1981/4/2	2975.0	NULL	20
7654	MARTIN	SALESMAN	7698	1981/9/28	1250.0	1400.0	30
7698	BLAKE	MANAGER	7839	1981/5/1	2850.0	NULL	30
7782	CLARK	MANAGER	7839	1981/6/9	2450.0	NULL	10
7788	SCOTT	ANALYST	7566	1987/4/19	3000.0	NULL	20
7839	KING	PRESIDENT	NULL	1981/11/17	5000.0	NULL	10
7844	TURNER	SALESMAN	7698	1981/9/8	1500.0	0.0	30
7876	ADAMS	CLERK	7788	1987/5/23	1100.0	NULL	20
7900	JAMES	CLERK	7698	1981/12/3	950.0	NULL	30
7902	FORD	ANALYST	7566	1981/12/3	3000.0	NULL	20
7934	MILLER	CLERK	7782	1982/1/23	1300.0	NULL	10
Time taken: 0.463 seconds, Fetched: 14 row(s)


hbase(main):003:0> scan 'hbase_emp_table''
ROW                         COLUMN+CELL                                                                 
 7369                       column=info:deptno, timestamp=2021-06-29T01:21:03.517, value=20             
 7369                       column=info:ename, timestamp=2021-06-29T01:21:03.517, value=SMITH           
 7369                       column=info:hiredate, timestamp=2021-06-29T01:21:03.517, value=1980/12/17   
 7369                       column=info:job, timestamp=2021-06-29T01:21:03.517, value=CLERK             
 7369                       column=info:mgr, timestamp=2021-06-29T01:21:03.517, value=7902              
 7369                       column=info:sal, timestamp=2021-06-29T01:21:03.517, value=800.0             
 7499                       column=info:co\x0Amm, timestamp=2021-06-29T01:21:03.517, value=300.0        
 7499                       column=info:deptno, timestamp=2021-06-29T01:21:03.517, value=30             
 7499                       column=info:ename, timestamp=2021-06-29T01:21:03.517, value=ALLEN           
 7499                       column=info:hiredate, timestamp=2021-06-29T01:21:03.517, value=1981/2/20    
 7499                       column=info:job, timestamp=2021-06-29T01:21:03.517, value=SALESMAN          
 7499                       column=info:mgr, timestamp=2021-06-29T01:21:03.517, value=7698              
 7499                       column=info:sal, timestamp=2021-06-29T01:21:03.517, value=1600.0            
 7521                       column=info:co\x0Amm, timestamp=2021-06-29T01:21:03.517, value=500.0        
 7521                       column=info:deptno, timestamp=2021-06-29T01:21:03.517, value=30             
 7521                       column=info:ename, timestamp=2021-06-29T01:21:03.517, value=WARD            
 7521                       column=info:hiredate, timestamp=2021-06-29T01:21:03.517, value=1981/2/22    
 7521                       column=info:job, timestamp=2021-06-29T01:21:03.517, value=SALESMAN          
 7521                       column=info:mgr, timestamp=2021-06-29T01:21:03.517, value=7698              
 7521                       column=info:sal, timestamp=2021-06-29T01:21:03.517, value=1250.0            
 7566                       column=info:deptno, timestamp=2021-06-29T01:21:03.517, value=20             
 7566                       column=info:ename, timestamp=2021-06-29T01:21:03.517, value=JONES           
 7566                       column=info:hiredate, timestamp=2021-06-29T01:21:03.517, value=1981/4/2     
 7566                       column=info:job, timestamp=2021-06-29T01:21:03.517, value=MANAGER           
 7566                       column=info:mgr, timestamp=2021-06-29T01:21:03.517, value=7839              
 7566                       column=info:sal, timestamp=2021-06-29T01:21:03.517, value=2975.0            
 7654                       column=info:co\x0Amm, timestamp=2021-06-29T01:21:03.517, value=1400.0       
 7654                       column=info:deptno, timestamp=2021-06-29T01:21:03.517, value=30             
 7654                       column=info:ename, timestamp=2021-06-29T01:21:03.517, value=MARTIN          
 7654                       column=info:hiredate, timestamp=2021-06-29T01:21:03.517, value=1981/9/28    
 7654                       column=info:job, timestamp=2021-06-29T01:21:03.517, value=SALESMAN          
 7654                       column=info:mgr, timestamp=2021-06-29T01:21:03.517, value=7698              
 7654                       column=info:sal, timestamp=2021-06-29T01:21:03.517, value=1250.0            
 7698                       column=info:deptno, timestamp=2021-06-29T01:21:03.517, value=30             
 7698                       column=info:ename, timestamp=2021-06-29T01:21:03.517, value=BLAKE           
 7698                       column=info:hiredate, timestamp=2021-06-29T01:21:03.517, value=1981/5/1     
 7698                       column=info:job, timestamp=2021-06-29T01:21:03.517, value=MANAGER           
 7698                       column=info:mgr, timestamp=2021-06-29T01:21:03.517, value=7839              
 7698                       column=info:sal, timestamp=2021-06-29T01:21:03.517, value=2850.0            
 7782                       column=info:deptno, timestamp=2021-06-29T01:21:03.517, value=10             
 7782                       column=info:ename, timestamp=2021-06-29T01:21:03.517, value=CLARK           
 7782                       column=info:hiredate, timestamp=2021-06-29T01:21:03.517, value=1981/6/9     
 7782                       column=info:job, timestamp=2021-06-29T01:21:03.517, value=MANAGER           
 7782                       column=info:mgr, timestamp=2021-06-29T01:21:03.517, value=7839              
 7782                       column=info:sal, timestamp=2021-06-29T01:21:03.517, value=2450.0            
 7788                       column=info:deptno, timestamp=2021-06-29T01:21:03.517, value=20             
 7788                       column=info:ename, timestamp=2021-06-29T01:21:03.517, value=SCOTT           
 7788                       column=info:hiredate, timestamp=2021-06-29T01:21:03.517, value=1987/4/19    
 7788                       column=info:job, timestamp=2021-06-29T01:21:03.517, value=ANALYST           
 7788                       column=info:mgr, timestamp=2021-06-29T01:21:03.517, value=7566              
 7788                       column=info:sal, timestamp=2021-06-29T01:21:03.517, value=3000.0            
 7839                       column=info:deptno, timestamp=2021-06-29T01:21:03.517, value=10             
 7839                       column=info:ename, timestamp=2021-06-29T01:21:03.517, value=KING            
 7839                       column=info:hiredate, timestamp=2021-06-29T01:21:03.517, value=1981/11/17   
 7839                       column=info:job, timestamp=2021-06-29T01:21:03.517, value=PRESIDENT         
 7839                       column=info:sal, timestamp=2021-06-29T01:21:03.517, value=5000.0            
 7844                       column=info:co\x0Amm, timestamp=2021-06-29T01:21:03.517, value=0.0          
 7844                       column=info:deptno, timestamp=2021-06-29T01:21:03.517, value=30             
 7844                       column=info:ename, timestamp=2021-06-29T01:21:03.517, value=TURNER          
 7844                       column=info:hiredate, timestamp=2021-06-29T01:21:03.517, value=1981/9/8     
 7844                       column=info:job, timestamp=2021-06-29T01:21:03.517, value=SALESMAN          
 7844                       column=info:mgr, timestamp=2021-06-29T01:21:03.517, value=7698              
 7844                       column=info:sal, timestamp=2021-06-29T01:21:03.517, value=1500.0            
 7876                       column=info:deptno, timestamp=2021-06-29T01:21:03.517, value=20             
 7876                       column=info:ename, timestamp=2021-06-29T01:21:03.517, value=ADAMS           
 7876                       column=info:hiredate, timestamp=2021-06-29T01:21:03.517, value=1987/5/23    
 7876                       column=info:job, timestamp=2021-06-29T01:21:03.517, value=CLERK             
 7876                       column=info:mgr, timestamp=2021-06-29T01:21:03.517, value=7788              
 7876                       column=info:sal, timestamp=2021-06-29T01:21:03.517, value=1100.0            
 7900                       column=info:deptno, timestamp=2021-06-29T01:21:03.517, value=30             
 7900                       column=info:ename, timestamp=2021-06-29T01:21:03.517, value=JAMES           
 7900                       column=info:hiredate, timestamp=2021-06-29T01:21:03.517, value=1981/12/3    
 7900                       column=info:job, timestamp=2021-06-29T01:21:03.517, value=CLERK             
 7900                       column=info:mgr, timestamp=2021-06-29T01:21:03.517, value=7698              
 7900                       column=info:sal, timestamp=2021-06-29T01:21:03.517, value=950.0             
 7902                       column=info:deptno, timestamp=2021-06-29T01:21:03.517, value=20             
 7902                       column=info:ename, timestamp=2021-06-29T01:21:03.517, value=FORD            
 7902                       column=info:hiredate, timestamp=2021-06-29T01:21:03.517, value=1981/12/3    
 7902                       column=info:job, timestamp=2021-06-29T01:21:03.517, value=ANALYST           
 7902                       column=info:mgr, timestamp=2021-06-29T01:21:03.517, value=7566              
 7902                       column=info:sal, timestamp=2021-06-29T01:21:03.517, value=3000.0            
 7934                       column=info:deptno, timestamp=2021-06-29T01:21:03.517, value=10             
 7934                       column=info:ename, timestamp=2021-06-29T01:21:03.517, value=MILLER          
 7934                       column=info:hiredate, timestamp=2021-06-29T01:21:03.517, value=1982/1/23    
 7934                       column=info:job, timestamp=2021-06-29T01:21:03.517, value=CLERK             
 7934                       column=info:mgr, timestamp=2021-06-29T01:21:03.517, value=7782              
 7934                       column=info:sal, timestamp=2021-06-29T01:21:03.517, value=1300.0            
14 row(s)
~~~



![成功展示](Hbase高版本搭建/image-20210629132121058.png)

#### 案例：hbase和hive映射

##### hive中创建表

~~~sql
hive (yaxin)> CREATE EXTERNAL TABLE relevance_hbase_emp(
            > empno int,
            > ename string,
            > job string,
            > mgr int,
            > hiredate string,
            > sal double,
            > comm double,
            > deptno int)
            > STORED BY
            > 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
            > WITH SERDEPROPERTIES ("hbase.columns.mapping" =
            > ":key,info:ename,info:job,info:mgr,info:hiredate,info:sal,info:co
            > mm,info:deptno")
            > TBLPROPERTIES ("hbase.table.name" = "hbase_emp_table");
OK
Time taken: 0.353 seconds
~~~

##### hive中查询数据

~~~sql
hive (yaxin)> select * from relevance_hbase_emp;
OK
relevance_hbase_emp.empno	relevance_hbase_emp.ename	relevance_hbase_emp.job	relevance_hbase_emp.mgr	relevance_hbase_emp.hiredate	relevance_hbase_emp.sal	relevance_hbase_emp.comm	relevance_hbase_emp.deptno
7369	SMITH	CLERK	7902	1980/12/17	800.0	NULL	20
7499	ALLEN	SALESMAN	7698	1981/2/20	1600.0	300.0	30
7521	WARD	SALESMAN	7698	1981/2/22	1250.0	500.0	30
7566	JONES	MANAGER	7839	1981/4/2	2975.0	NULL	20
7654	MARTIN	SALESMAN	7698	1981/9/28	1250.0	1400.0	30
7698	BLAKE	MANAGER	7839	1981/5/1	2850.0	NULL	30
7782	CLARK	MANAGER	7839	1981/6/9	2450.0	NULL	10
7788	SCOTT	ANALYST	7566	1987/4/19	3000.0	NULL	20
7839	KING	PRESIDENT	NULL	1981/11/17	5000.0	NULL	10
7844	TURNER	SALESMAN	7698	1981/9/8	1500.0	0.0	30
7876	ADAMS	CLERK	7788	1987/5/23	1100.0	NULL	20
7900	JAMES	CLERK	7698	1981/12/3	950.0	NULL	30
7902	FORD	ANALYST	7566	1981/12/3	3000.0	NULL	20
7934	MILLER	CLERK	7782	1982/1/23	1300.0	NULL	10
~~~

