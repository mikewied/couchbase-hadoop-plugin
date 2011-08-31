/** 
 * @author Couchbase <info@couchbase.com>
 * @copyright 2011 Couchbase, Inc.
 * All rights reserved.
 */

package com.couchbase.sqoop.manager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hsqldb.Types;

import com.cloudera.sqoop.SqoopOptions;
import com.cloudera.sqoop.manager.ConnManager;
import com.cloudera.sqoop.manager.ExportJobContext;
import com.cloudera.sqoop.manager.ImportJobContext;
import com.cloudera.sqoop.mapreduce.DataDrivenImportJob;
import com.cloudera.sqoop.mapreduce.HBaseImportJob;
import com.cloudera.sqoop.mapreduce.ImportJobBase;
import com.cloudera.sqoop.mapreduce.JdbcExportJob;
import com.cloudera.sqoop.util.ExportException;
import com.cloudera.sqoop.util.ImportException;
import com.couchbase.sqoop.mapreduce.db.CouchbaseInputFormat;
import com.couchbase.sqoop.mapreduce.db.CouchbaseOutputFormat;

/**
 * Database manager that connects to a tap stream; its 
 * constructor is parameterized on the http URL
 * class to load.
 */
public class CouchbaseManager extends ConnManager {
  public static final Log LOG = LogFactory.getLog(
      CouchbaseManager.class.getName());
  private static final String CLMN_1 = "Key";
  private static final String CLMN_2 = "Value";

  protected SqoopOptions options;

  public CouchbaseManager(final SqoopOptions opts) {
    this.options = opts;
  }

  @Override
  public String[] listDatabases() {
    return new String[] { "Not currently supported" };
  }

  @Override
  public String[] listTables() {
    return new String[] { "DUMP", "BACKFILL_5" };
  }

  @Override
  public String[] getColumnNames(String tableName) {
    return new String[] { CLMN_1, CLMN_2 };
  }

  @Override
  public String getPrimaryKey(String tableName) {
    return CLMN_1;
  }

  @Override
  public String toJavaType(int sqlType) {
    return "String";
  }

  @Override
  public String toHiveType(int sqlType) {
    return "STRING";
  }

  @Override
  public Map<String, Integer> getColumnTypes(String tableName) {
    Map<String, Integer> clmnTypes = new HashMap<String, Integer>();
    clmnTypes.put(CLMN_1, Integer.valueOf(Types.VARCHAR));
    clmnTypes.put(CLMN_2, Integer.valueOf(Types.VARCHAR));
    return clmnTypes;
  }

  @Override
  public ResultSet readTable(String tableName, String[] columns)
      throws SQLException {
    return null;
  }

  @Override
  public Connection getConnection() throws SQLException {
    return null;
  }

  @Override
  public String getDriverClass() {
    return "couchbase doesn't use a jdbc driver";
  }

  @Override
  public void execAndPrint(String s) {
    LOG.info("Couchbase server doesn't handle SQL");
  }

  @Override
  public void importTable(ImportJobContext context) throws IOException,
      ImportException {
    String tableName = context.getTableName();
    String jarFile = context.getJarFile();
    SqoopOptions opts = context.getOptions();

    context.setConnManager(this);
    context.setInputFormat(CouchbaseInputFormat.class);

    ImportJobBase importer;
    if (opts.getHBaseTable() != null) {
      // Import to HBase.
      importer = new HBaseImportJob(opts, context);
    } else {
      // Import to HDFS.
      importer = new DataDrivenImportJob(opts, context.getInputFormat(),
          context);
    }

    String splitCol = getSplitColumn(opts, tableName);
    importer.runImport(tableName, jarFile, splitCol, opts.getConf());
  }

  /**
   * Export data stored in HDFS into a table in a database.
   */
  public void exportTable(ExportJobContext context) throws IOException,
      ExportException {
    context.setConnManager(this);
    JdbcExportJob exportJob = new JdbcExportJob(context, null, null,
        CouchbaseOutputFormat.class);
    exportJob.runExport();
  }

  protected String getSplitColumn(SqoopOptions opts, String tableName) {
    String splitCol = opts.getSplitByCol();
    if (null == splitCol && null != tableName) {
      splitCol = getPrimaryKey(tableName);
    }
    return splitCol;
  }

  @Override
  public void close() throws SQLException {
  }

  @Override
  public void release() {
  }
}
