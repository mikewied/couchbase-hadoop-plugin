/** 
 * @author Couchbase <info@couchbase.com>
 * @copyright 2011 Couchbase, Inc.
 * All rights reserved.
 */

package com.couchbase.sqoop.mapreduce.db;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.lib.db.DBWritable;

import com.cloudera.sqoop.mapreduce.db.DBConfiguration;
import com.cloudera.sqoop.mapreduce.db.DBInputFormat.NullDBWritable;

/**
 * A container for configuration property names for jobs with Couchbase 
 * input/output.
 *
 * The job can be configured using the static methods in this class,
 * {@link CouchbaseInputFormat}, and {@link CouchbaseOutputFormat}.
 * Alternatively, the properties can be set in the configuration with proper
 * values.
 */
public class CouchbaseConfiguration {

  private Configuration conf;

  public CouchbaseConfiguration(Configuration job) {
    this.conf = job;
  }

  public Configuration getConf() {
    return conf;
  }

  public Class<?> getInputClass() {
    return conf.getClass(DBConfiguration.INPUT_CLASS_PROPERTY,
        NullDBWritable.class);
  }

  public void setInputClass(Class<? extends DBWritable> inputClass) {
    conf.setClass(DBConfiguration.INPUT_CLASS_PROPERTY, inputClass,
        DBWritable.class);
  }

  public String getUsername() {
    return conf.get(DBConfiguration.USERNAME_PROPERTY, "default");
  }

  public void setUsername(String username) {
    conf.set(DBConfiguration.USERNAME_PROPERTY, username);
  }

  public String getPassword() {
    return conf.get(DBConfiguration.PASSWORD_PROPERTY, "");
  }

  public void setPassword(String password) {
    conf.set(DBConfiguration.PASSWORD_PROPERTY, password);
  }

  public String getUrlProperty() {
    return conf.get(DBConfiguration.URL_PROPERTY);
  }

  public void setUrlProperty(String url) {
    conf.set(DBConfiguration.URL_PROPERTY, url);
  }

  public String getInputTableName() {
    return conf.get(DBConfiguration.INPUT_TABLE_NAME_PROPERTY);
  }

  public void setOutputTableName(String tableName) {
    conf.set(DBConfiguration.OUTPUT_TABLE_NAME_PROPERTY, tableName);
  }
}
