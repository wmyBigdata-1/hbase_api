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
