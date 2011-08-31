/** 
 * @author Couchbase <info@couchbase.com>
 * @copyright 2011 Couchbase, Inc.
 * All rights reserved.
 */

package com.couchbase.sqoop.mapreduce.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.db.DBWritable;

import com.cloudera.sqoop.config.ConfigurationHelper;
import com.cloudera.sqoop.mapreduce.db.DBInputFormat.DBInputSplit;

/**
 * A InputFormat that reads input data from Couchbase Server
 * <p>
 * CouchbaseInputFormat emits LongWritables containing a record 
 * number as key and DBWritables as value.
 *
 */
public class CouchbaseInputFormat<T extends DBWritable> extends
    InputFormat<LongWritable, T> implements Configurable {

  private String tableName;

  private CouchbaseConfiguration dbConf;

  @Override
  public void setConf(Configuration conf) {
    dbConf = new CouchbaseConfiguration(conf);
    tableName = dbConf.getInputTableName();
  }

  @Override
  public Configuration getConf() {
    return dbConf.getConf();
  }

  public CouchbaseConfiguration getDBConf() {
    return dbConf;
  }

  @Override
  /** {@inheritDoc} */
  public RecordReader<LongWritable, T> createRecordReader(InputSplit split,
      TaskAttemptContext context) throws IOException, InterruptedException {
    return createRecordReader(context.getConfiguration());
  }

  @SuppressWarnings("unchecked")
  public RecordReader<LongWritable, T> createRecordReader(Configuration conf) 
      throws IOException, InterruptedException {
    Class<T> inputClass = (Class<T>) (dbConf.getInputClass());
    return new CouchbaseRecordReader<T>(inputClass, conf, getDBConf(), 
        tableName);
  }

  @Override
  public List<InputSplit> getSplits(JobContext job) throws IOException,
      InterruptedException {
    long count = 2;
    int chunks = ConfigurationHelper.getJobNumMaps(job);
    long chunkSize = (count / chunks);

    List<InputSplit> splits = new ArrayList<InputSplit>();

    // Split the rows into n-number of chunks and adjust the last chunk
    // accordingly
    for (int i = 0; i < chunks; i++) {
      DBInputSplit split;

      if ((i + 1) == chunks) {
        split = new DBInputSplit(i * chunkSize, count);
      } else {
        split = new DBInputSplit(i * chunkSize, (i * chunkSize) + chunkSize);
      }
      splits.add(split);
    }
    return splits;
  }
}
