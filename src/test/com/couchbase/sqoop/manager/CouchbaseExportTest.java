/** 
 * @author Couchbase <info@couchbase.com>
 * @copyright 2011 Couchbase, Inc.
 * All rights reserved.
 */

package com.couchbase.sqoop.manager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import net.spy.memcached.MemcachedClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.cloudera.sqoop.SqoopOptions;
import com.cloudera.sqoop.TestExport;
import com.couchbase.sqoop.manager.CouchbaseManager;

public class CouchbaseExportTest extends TestExport {

  private CouchbaseManager manager;

  private MemcachedClient mc;

  @Override
  protected Connection getConnection() {
    return null;
  }

  @Override
  protected boolean useHsqldbTestServer() {
    return true;
  }

  @Override
  protected String getConnectString() {
    return CouchbaseUtils.CONNECT_STRING;
  }

  @Before
  public void setUp() {
    getConf().set("connection.manager", CouchbaseUtils.COUCHBASE_CONN_MANAGER);
    super.setUp();

    SqoopOptions options = new SqoopOptions(CouchbaseUtils.CONNECT_STRING,
        getTableName());
    options.setConnManagerClassName(CouchbaseUtils.COUCHBASE_CONN_MANAGER);
    this.manager = new CouchbaseManager(options);
    
    String connStr = CouchbaseUtils.CONNECT_STRING;
    String user = CouchbaseUtils.COUCHBASE_USER_NAME;
    String pass = CouchbaseUtils.COUCHBASE_USER_PASS;

    try {
      mc = new MemcachedClient(Arrays.asList(new URI(connStr)), user, user, pass);
    } catch (IOException e) {
      fail("Couldn't connect to Couchbase Server");
    } catch (URISyntaxException e) {
      fail("Invalid URI for Couchbase Server");
    }
  }

  @After
  public void tearDown() {
    super.tearDown();
    mc.shutdown();

    if (null != manager) {
      try {
        manager.close();
        manager = null;
      } catch (SQLException sqlE) {
        LOG.error("Got SQLException: " + sqlE.toString());
        fail("Got SQLException: " + sqlE.toString());
      }
    }
  }

  @Override
  protected String [] getCodeGenArgv(String... extraArgs) {
    String [] moreArgs = new String[extraArgs.length + 6];
    int i = 0;

    for (i = 0; i < extraArgs.length; i++) {
      moreArgs[i] = extraArgs[i];
    }

    // Add username and password args.
    moreArgs[i++] = "--username";
    moreArgs[i++] = CouchbaseUtils.COUCHBASE_USER_NAME;
    moreArgs[i++] = "--password";
    moreArgs[i++] = CouchbaseUtils.COUCHBASE_USER_PASS;
    moreArgs[i++] = "--connection-manager";
    moreArgs[i++] = CouchbaseUtils.COUCHBASE_CONN_MANAGER;

    return super.getCodeGenArgv(moreArgs);
  }

  @Override
  protected String [] getArgv(boolean includeHadoopFlags,
    int rowsPerStatement, int statementsPerTx, String... additionalArgv) {

    String [] subArgv = newStrArray(additionalArgv,
        "--username", CouchbaseUtils.COUCHBASE_USER_NAME,
        "--password", CouchbaseUtils.COUCHBASE_USER_PASS,
        "--connection-manager", CouchbaseUtils.COUCHBASE_CONN_MANAGER);
    return super.getArgv(includeHadoopFlags, rowsPerStatement,
        statementsPerTx, subArgv);
  }

  @Override
  public void createTable(ColumnGenerator... extraColumns) throws SQLException {
    // Couchbase doesn't use tables.
  }

  @Override
  protected void verifyExport(int expectedNumRecords)
      throws IOException, SQLException {
    try {
      for (int i = 0; i < expectedNumRecords; i++) {
        if (mc.get(i + "") == null) {
          fail("Failed to get an exported key from Couchbase: Tried 5 times");
        }
      }
      if (!mc.flush().get().booleanValue()) {
        fail("Unable to flush keys");
      }
    } catch (InterruptedException e) {
      fail("Interrupted while checking keys");
    } catch (ExecutionException e) {
      fail("Operation failed to flush keys");
    }
  }

  @Override
  public void testNumericTypes() throws IOException, SQLException {
    // Couchbase only reads strings.
  }

  @Override
  public void testDatesAndTimes() throws IOException, SQLException {
    // Couchbase only reads strings.
  }

  @Override
  public void testBigIntCol() throws IOException, SQLException {
    // Couchbase only has two columns.
  }

  @Override
  public void testIntCol() throws IOException, SQLException {
    // Couchbase only has two columns.
  }

  @Test
  @Override
  public void testMultiTransactionWithStaging() {
    // CouchSqoop doesn't support staging tables
  }

  @Test
  @Override
  public void testMultiMapTextExportWithStaging() {
    // CouchSqoop doesn't support staging tables
  }
}
