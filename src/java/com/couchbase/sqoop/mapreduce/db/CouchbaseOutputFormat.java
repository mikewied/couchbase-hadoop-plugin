/** 
 * @author Couchbase <info@couchbase.com>
 * @copyright 2011 Couchbase, Inc.
 * All rights reserved.
 */

package com.couchbase.sqoop.mapreduce.db;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import net.spy.memcached.MemcachedClient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.OutputCommitter;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.db.DBWritable;
import org.apache.hadoop.mapreduce.lib.output.FileOutputCommitter;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.cloudera.sqoop.mapreduce.db.DBConfiguration;

/**
 * A OutputFormat that sends the reduce output to a Couchbase server.
 * <p>
 * {@link CocuhbaseOutputFormat} accepts &lt;key,value&gt; pairs, where
 * key has a type extending DBWritable. Returned {@link RecordWriter}
 * writes <b>only the key</b> to the database with a batch tap stream.
 */
public class CouchbaseOutputFormat<K extends DBWritable, V>
    extends OutputFormat<K, V> {
  public static final Log LOG = LogFactory.getLog(
      CouchbaseOutputFormat.class.getName());

  @Override
  public void checkOutputSpecs(JobContext context) throws IOException,
      InterruptedException {
    Configuration conf = context.getConfiguration();

    // Sanity check all the configuration values we need.
    if (null == conf.get(DBConfiguration.URL_PROPERTY)) {
      throw new IOException("Database connection URL is not set.");
    }
  }

  @Override
  public OutputCommitter getOutputCommitter(TaskAttemptContext context)
      throws IOException, InterruptedException {
    return new FileOutputCommitter(FileOutputFormat.getOutputPath(context),
        context);
  }

  @Override
  public RecordWriter<K, V> getRecordWriter(TaskAttemptContext context)
      throws IOException, InterruptedException {
    return new CouchbaseRecordWriter(
        new CouchbaseConfiguration(context.getConfiguration()));
  }

  /**
   * A RecordWriter that writes the reduce output to a Couchbase
   * server.
   */
  public class CouchbaseRecordWriter extends RecordWriter<K, V> {

    class KV {
      public String key;
      public String value;
      public Future<Boolean> status;

      public KV(String key, String value, Future<Boolean> status) {
        this.key = key;
        this.value = value;
        this.status = status;
      }
    }

    private MemcachedClient client;

    private BlockingQueue<KV> opQ;

    public CouchbaseRecordWriter(CouchbaseConfiguration dbConf) {
      String user = dbConf.getUsername();
      String pass = dbConf.getPassword();
      String url = dbConf.getUrlProperty();
      opQ = new LinkedBlockingQueue<KV>();
      try {
        client = new MemcachedClient(Arrays.asList(new URI(url)),
            user, user, pass);
      } catch (IOException e) {
        client.shutdown();
        e.printStackTrace();
      } catch (URISyntaxException e) {
        client.shutdown();
        e.printStackTrace();
      }
    }

    @Override
    public void close(TaskAttemptContext context) throws IOException,
        InterruptedException {
      while (opQ.size() > 0) {
        drainQ();
      }
      client.shutdown();
    }
    
    private void drainQ() {
      Queue<KV> list = new LinkedList<KV>();
      opQ.drainTo(list, opQ.size());

      KV kv;
      while ((kv = list.poll()) != null) {
        try {
          if (!kv.status.get().booleanValue()) {
            opQ.add(new KV(kv.key, kv.value, client.set(kv.key, 0, kv.value)));
            LOG.info("Failed");
          }
        } catch (RuntimeException e) {
          LOG.info("RuntimeException " + e.getMessage());
          opQ.add(new KV(kv.key, kv.value, client.set(kv.key, 0, kv.value)));
          while ((kv = list.poll()) != null) {
            opQ.add(new KV(kv.key, kv.value, client.set(kv.key, 0, kv.value)));
          }
        } catch (Exception e) {
          LOG.info("Exception " + e.getMessage());
          opQ.add(new KV(kv.key, kv.value, client.set(kv.key, 0, kv.value)));
          while ((kv = list.poll()) != null) {
            opQ.add(new KV(kv.key, kv.value, client.set(kv.key, 0, kv.value)));
          }
        }
      }
    }

    @Override
    public void write(K key, V value) throws IOException, InterruptedException {
      String k = null;
      String v = null;
      Writable w = (Writable)key;
      ByteArrayOutputStream in = new ByteArrayOutputStream();
      DataOutput dataIn = new DataOutputStream(in);
      w.write(dataIn);
      byte[] b = in.toByteArray();
      
      if (opQ.size() > 10000) {
        drainQ();
      }

      int i = 0;
      if (b[i] == 0) {
        int klen = b[++i];
        byte[] mkey = new byte[klen];
        for (int j = 0; j < klen; j++) {
          mkey[j] = b[++i];
        }
        k = new String(mkey);
      }
      if (b[++i] == 0) {
        int vlen = b[++i];
        byte[] mvalue = new byte[vlen];
        for (int j = 0; j < vlen; j++) {
          mvalue[j] = b[++i];
        }
        v = new String(mvalue);
      }
      opQ.add(new KV(k, v, client.set(k, 0, v)));
    }
  }
}
