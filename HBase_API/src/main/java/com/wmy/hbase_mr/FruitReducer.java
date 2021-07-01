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
