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
