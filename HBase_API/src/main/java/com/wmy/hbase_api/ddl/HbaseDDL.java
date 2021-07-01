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
