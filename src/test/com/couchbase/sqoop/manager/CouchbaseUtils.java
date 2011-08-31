/** 
 * @author Couchbase <info@couchbase.com>
 * @copyright 2011 Couchbase, Inc.
 * All rights reserved.
 */

package com.couchbase.sqoop.manager;

import com.cloudera.sqoop.SqoopOptions;

public class CouchbaseUtils {
  public static final String CONNECT_STRING =
      System.getProperty("sqoop.test.couchbase.connectstring",
      "http://localhost:8091/pools");
  
  public static final String COUCHBASE_USER_NAME = "default";
  
  public static final String COUCHBASE_USER_PASS = "";
  
  public static final String COUCHBASE_CONN_MANAGER = 
      "com.couchbase.sqoop.manager.CouchbaseManager";

  private CouchbaseUtils() { }

  public static void setCouchbaseAuth(SqoopOptions options) {
    options.setUsername(COUCHBASE_USER_NAME);
    options.setPassword(COUCHBASE_USER_PASS);
  }
}
