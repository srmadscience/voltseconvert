package org.voltdb.convert.oracle;

/* This file is part of VoltDB.
 * Copyright (C) 2008-2017 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.voltdb.convert.common.AbstractBaseConverter;
import org.voltdb.convert.common.ConverterDBInterface;
import org.voltdb.convert.common.OtherDBProcedures;
import org.voltdb.convert.common.OtherDBTable;
import org.voltdb.convert.common.OtherDBTableColumn;
import org.voltdb.convert.common.OtherDbProcParameter;
import org.voltdb.convert.common.OtherDbSQLStatement;
import org.voltdb.seutils.utils.CSException;
import org.voltdb.seutils.wranglers.oracle.OracleConnectionWrangler;
import org.voltdb.seutils.wranglers.oracle.SqlUtils;

public class OracleConverterImpl extends AbstractBaseConverter implements ConverterDBInterface {

	final long SQL_PASS_MS = 60000;

	static final String TABLENAME_QUERY = "select owner, table_name, 'TABLE' kind from all_tables "
			+ "where owner in (user,?) " + " union all " // Include views
			+ "Select owner, view_name, 'VIEW' from all_views where owner in (user,?) " + "order by owner, table_name";

	static final String TABLECOL_QUERY = "select column_name, data_type, data_length, "
			+ "nvl(data_scale,-42) data_scale, nvl(data_precision," + SqlUtils.NULL_VALUE
			+ ") data_precision, nullable " + "from all_tab_columns " + "where owner = ? and table_name = ? "
			+ "order by column_id";

	static final String TABLEDDL_QUERY = "select DBMS_METADATA.GET_DDL(?,?,?) from DUAL";

	static final String PROC_QUERY = "select owner,package_name, object_name, nvl(overload,-1) overload from all_arguments where data_level = 0  and owner in (user, ?) group by owner,package_name, object_name, overload order by owner,package_name, object_name, overload";

	static final String ARG_QUERY = "select owner,package_name, object_name, nvl(overload,-1) overload, "
			+ "nvl(argument_name , 'out') argument_name, data_type, in_out " + "from all_arguments "
			+ "where package_name = ? " + "and   object_name  = ? " + " and owner = ?" + "and   NVL(overload,-1) = ? "
			+ "and data_level = 0  " + "order by package_name, object_name, overload, sequence";

	static final String SOURCE_QUERY = "select * from all_source where owner in (user,?) and name = ? and type in ('PACKAGE BODY','PROCEDURE','FUNCTION')"
			+ "order by owner,name, type, line";

	static final String SQL_QUERY = "select sql_id, sql_text, parsing_schema_name " + " from v$sql "
			+ "where parsing_schema_name in (user,?) " + " AND sql_text not like '%FROM SYS.KU$%' "
			+ " AND sql_text NOT LIKE 'SELECT SYS_CONTEXT(_USERENV_,_CURRENT_USER_) FROM DUAL' "
			// + " and sql_id = '40fbcvbrtjn0s' "
			// TODO
			+ "group by sql_id, sql_text, parsing_schema_name order by sql_text ";

	static final String TABLEDDL_PKQUERY = "select c.constraint_name, f.column_name, position "
			+ "from all_constraints c, all_cons_columns f  " + "where c.owner = ?  " + "and  c.table_name = ? "
			+ "and c.constraint_type = 'P'  " + "and f.owner = c.owner  " + "and f.constraint_name = c.constraint_name "
			+ "order by position";

	OracleConnectionWrangler w = null;

	ArrayList<OtherDBTable> theTables = new ArrayList<OtherDBTable>();

	ArrayList<OtherDBProcedures> theProcedures = new ArrayList<OtherDBProcedures>();

	Hashtable<String, OtherDbSQLStatement> theSql = new Hashtable<String, OtherDbSQLStatement>();

	int sqlPasses = 1;
	
	public OracleConverterImpl(String pHostName, int pPort, String pSid, String pUser, String pPassword,
			String otherUser, String packageName, String rootDirName, int sqlPasses) throws Exception {

		super(pUser, otherUser, packageName, rootDirName);

		w = new OracleConnectionWrangler(pHostName, pPort, pSid, pUser, pPassword, l, OracleConnectionWrangler.ORA);

		this.sqlPasses = sqlPasses;

	}

	private void getSqlStatements() {
		try {
			int matches = 0;
			int rows = 0;

			w.confirmConnected();

			PreparedStatement s = w.mrConnection.prepareStatement(SQL_QUERY);

			s.setString(1, otherUser);
			ResultSet sqlRs = s.executeQuery();

			while (sqlRs.next()) {

				rows++;

				String hash = sqlRs.getString("SQL_ID");
				String text = sqlRs.getString("SQL_TEXT");

				OtherDbSQLStatement newSql = new OtherDbSQLStatement(text, hash);

				if (!theSql.containsKey(hash)) {

					matches++;

					theSql.put(hash, newSql);

					l.info("Found SQL " + newSql.getId() + "...");
					l.info(newSql.getSql());

					// find places where this is used
					findAndUpdateUsage(newSql);
				}

			}
			sqlRs.close();

			l.info("Found " + rows + " SQL Statements; added " + matches);
		} catch (Exception e) {
			e.printStackTrace();

		}

	}

	private void findAndUpdateUsage(OtherDbSQLStatement newSql) {

		if (theProcedures.size() > 0) {
			for (int i = 0; i < theProcedures.size(); i++) {
				theProcedures.get(i).addSqlStatementIfUsed(newSql, l);
			}
		}

	}

	@Override
	public OtherDBTable[] getTables() {

		theTables.clear();

		try {
			w.confirmConnected();

			PreparedStatement p = w.mrConnection.prepareStatement(TABLENAME_QUERY);
			PreparedStatement c = w.mrConnection.prepareStatement(TABLECOL_QUERY);
			PreparedStatement s = w.mrConnection.prepareStatement(TABLEDDL_QUERY);
			PreparedStatement pk = w.mrConnection.prepareStatement(TABLEDDL_PKQUERY);

			p.setString(1, otherUser);
			p.setString(2, otherUser);

			ResultSet tableNameRS = p.executeQuery();

			while (tableNameRS.next()) {

				String tableOwner = tableNameRS.getString("OWNER");
				String tableName = tableNameRS.getString("TABLE_NAME");
				String tableKind = tableNameRS.getString("KIND");

				c.setString(1, tableOwner);
				c.setString(2, tableName);

				ResultSet columnRS = c.executeQuery();

				ArrayList<OtherDBTableColumn> colList = new ArrayList<OtherDBTableColumn>();

				while (columnRS.next()) {

					String columnName = columnRS.getString("COLUMN_NAME");

					int datatype = SqlUtils.getUnderlyingOracleDatatype(columnRS.getString("DATA_TYPE"));
					int length = columnRS.getInt("DATA_LENGTH");
					int scale = columnRS.getInt("DATA_SCALE");
					int precision = columnRS.getInt("DATA_PRECISION");

					String columnVoltDataType = SqlUtils.getUnderlyingVoltDBDataType(datatype, length, scale,
							precision);

					columnRS.getString("DATA_TYPE");
					boolean notNull = false;
					if (columnRS.getString("NULLABLE").equals("N")) {
						notNull = true;
					}

					OtherDBTableColumn newCol = new OtherDBTableColumn(columnName, columnVoltDataType, notNull);
					colList.add(newCol);

				}

				OtherDBTableColumn[] columns = new OtherDBTableColumn[colList.size()];
				columns = colList.toArray(columns);

				pk.setString(2, tableName);
				pk.setString(1, tableOwner);

				ResultSet pkRS = pk.executeQuery();

				OtherDBTable newTable = new OtherDBTable(tableName, columns);

				while (pkRS.next()) {
					newTable.setPkName(pkRS.getString("CONSTRAINT_NAME"));
					String colName = pkRS.getString("COLUMN_NAME");
					int colPos = pkRS.getInt("POSITION");

					for (int i = 0; i < columns.length; i++) {

						if (columns[i].getColumnName().equals(colName)) {
							columns[i].setInPK(colPos);
						}

					}
				}

				pkRS.close();

				s.setString(2, tableName);
				s.setString(3, tableOwner);
				s.setString(1, tableKind);
				ResultSet ddlRS = s.executeQuery();
				while (ddlRS.next()) {
					newTable.setComment(ddlRS.getString(1));
				}
				ddlRS.close();

				theTables.add(newTable);
			}

			c.close();
			p.close();
			s.close();
			pk.close();

		} catch (CSException e) {
			l.error(e);
		} catch (SQLException e) {
			l.error(e);
		}

		OtherDBTable[] theArray = new OtherDBTable[theTables.size()];
		theArray = theTables.toArray(theArray);
		return theArray;
	}

	@Override
	public OtherDBProcedures[] getProcedures() {

		theProcedures.clear();

		// Add a fake procedure that gets every SQL statement we find...
		final String[] otherSource = { "" };
		OtherDBProcedures everythingProc = new OtherDBProcedures(javaPackageName, "",
				getSaneProcName(OtherDBProcedures.EVERYTHING, OtherDBProcedures.EVERYTHING, -42),
				new ArrayList<OtherDbProcParameter>(), new ArrayList<OtherDbProcParameter>(), otherSource, l);
		theProcedures.add(everythingProc);

		
		// Go and get the other procedures...
		try {
			w.confirmConnected();

			PreparedStatement p = w.mrConnection.prepareStatement(PROC_QUERY);
			PreparedStatement c = w.mrConnection.prepareStatement(ARG_QUERY);
			PreparedStatement src = w.mrConnection.prepareStatement(SOURCE_QUERY);

			p.setString(1, otherUser);
			ResultSet procNameRS = p.executeQuery();

			while (procNameRS.next()) {

				String packOwner = procNameRS.getString("OWNER");
				String packName = procNameRS.getString("PACKAGE_NAME");
				String objName = procNameRS.getString("OBJECT_NAME");
				int overload = procNameRS.getInt("OVERLOAD");

				c.setString(1, packName);
				c.setString(2, objName);
				c.setString(3, packOwner);
				c.setInt(4, overload);

				src.setString(1, packOwner);

				if (packName != null) {
					src.setString(2, packName);
				} else {
					src.setString(2, objName);
				}

				
				// Get the source code
				ResultSet sourceRs = src.executeQuery();

				ArrayList<String> srcArrayList = new ArrayList<String>();
				while (sourceRs.next()) {
					srcArrayList.add(sourceRs.getString("TEXT"));
				}

				String[] procSource = new String[srcArrayList.size()];
				procSource = srcArrayList.toArray(procSource);
				
				
				// get parameters
				ResultSet paramRS = c.executeQuery();

				ArrayList<OtherDbProcParameter> paramInList = new ArrayList<OtherDbProcParameter>();
				ArrayList<OtherDbProcParameter> paramOutList = new ArrayList<OtherDbProcParameter>();

				while (paramRS.next()) {

					String columnVoltJavaDataType = SqlUtils.getUnderlyingVoltJavaDataType(
							SqlUtils.getUnderlyingOracleDatatype(paramRS.getString("DATA_TYPE")), -1, -1, -1);
					String columnVoltDBDataType = SqlUtils.getUnderlyingVoltDBDataType(
							SqlUtils.getUnderlyingOracleDatatype(paramRS.getString("DATA_TYPE")), -1, -1, -1);
					OtherDbProcParameter newParam = new OtherDbProcParameter(paramRS.getString("ARGUMENT_NAME"),
							columnVoltJavaDataType, columnVoltDBDataType);

					String inOut = paramRS.getString("IN_OUT");

					if (!inOut.equals("OUT")) {
						paramInList.add(newParam);
					}

					if (!inOut.equals("IN")) {
						paramOutList.add(newParam);
					}

				}


				OtherDBProcedures newProc = new OtherDBProcedures(javaPackageName,packName,
						getSaneProcName(packName, objName, overload), paramInList, paramOutList, procSource, l);

				theProcedures.add(newProc);
			}

			c.close();
			p.close();
			src.close();

			// Now go off and look for parsed SQL statements. Add them to the 
			// right procedures if possible.
			getSqlStatements();

			if (sqlPasses > 1) {

				for (int j = 1; j < sqlPasses; j++) {

					// We may want to run for a while to pick up all the SQL
					l.info("Sleeping " + SQL_PASS_MS + " ms");
					try {
						Thread.sleep(SQL_PASS_MS);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					l.info("Pass " + (j + 1) + " of " + sqlPasses);

					getSqlStatements();
				}

			}

		} catch (CSException e) {
			l.error(e);
		} catch (SQLException e) {
			l.error(e);
		}

		OtherDBProcedures[] theArray = new OtherDBProcedures[theProcedures.size()];
		theArray = theProcedures.toArray(theArray);

		int procsWithSql = 0;

		for (int i = 0; i < theArray.length; i++) {
			if (theArray[i].getSqlStatementCount() > 0) {
				procsWithSql++;
			} else {
				l.info(theArray[i].getPackageName() + " " + theArray[i].getProcedureName()
						+ " doesnt have any SQL statements we can find in V$SQL");
			}

		}

		l.info(procsWithSql + " procedures out of " + theArray.length + " have SQL statements.");
		l.flush();

		return theArray;
	}

	private String getSaneProcName(String packName, String objName, int overload) {
		String name = objName;

		if (overload > 0) {
			name = name + overload;
		}

		return name;
	}

	public static String getUsageMethod() {

		return "ORACLE host m_port sid user pass otheruser package_name dir sqlpasses" + System.lineSeparator()
				+ "for example: " + System.lineSeparator()
				+ "ORACLE voltconv.cpolcsopv9zq.us-east-1.rds.amazonaws.com 1521 ORCL demouser demopass otherusername org.foo /Users/drolfe/foo2 1"
				+ System.lineSeparator();
	}

}
