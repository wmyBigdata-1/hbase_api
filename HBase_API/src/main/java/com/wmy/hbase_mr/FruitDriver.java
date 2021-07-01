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
