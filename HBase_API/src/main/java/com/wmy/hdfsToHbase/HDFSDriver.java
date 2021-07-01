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
