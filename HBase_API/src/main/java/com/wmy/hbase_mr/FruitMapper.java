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
