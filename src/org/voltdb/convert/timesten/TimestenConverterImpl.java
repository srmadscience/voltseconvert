package org.voltdb.convert.timesten;

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

public class TimestenConverterImpl extends AbstractBaseConverter implements ConverterDBInterface {

	OracleConnectionWrangler w = null;

	ArrayList<OtherDBTable> theTables = new ArrayList<OtherDBTable>();

	ArrayList<OtherDBProcedures> theProcedures = new ArrayList<OtherDBProcedures>();

	Hashtable<String, OtherDbSQLStatement> theSql = new Hashtable<String, OtherDbSQLStatement>();

	int sqlPasses = 1;

	final long SQL_PASS_MS = 60000;

	static final String TABLENAME_QUERY = "select owner, table_name, 'TABLE' kind from all_tables "
			+ "where owner in (user,?) " + " union all " // Include views
			+ "Select owner, view_name, 'VIEW' from all_views where owner in (user,?) " + "order by owner, table_name";

	// See
	// https://docs.oracle.com/cd/E21901_01/timesten.1122/e21639/dtypesfunc.htm#TTPLS185
	// and
	// https://docs.oracle.com/cd/B19306_01/server.102/b14200/sql_elements001.htm
	// and
	// https://docs.oracle.com/cd/E11882_01/timesten.112/e21642/types.htm#TTSQL124
	// and
	// https://docs.oracle.com/database/121/SQLRF/sql_elements001.htm

	static final String TABLECOL_QUERY = "select c.colname column_name , " + "decode(c.coltype" + ", 1, 'NUMBER' " // 'TT_SMALLINT'
			+ ", 2, 'NUMBER' " // 'TT_INTEGER'
			+ ", 3, 'BINARY_FLOAT'" + ", 4, 'BINARY_DOUBLE'" + ", 5, 'CHAR' " // 'TT_CHAR'
			+ ", 6, 'VARCHAR' " // 'TT_VARCHAR'
			+ ", 7, 'RAW' " // 'BINARY'"
			+ ", 8, 'RAW' " // 'VARBINARY'"
			+ ",11, 'NUMBER' " // 'TT_DECIMAL'"
			+ ",12, 'NCHAR' " // 'TT_NCHAR'"
			+ ",13, 'NVARCHAR' " // 'TT_NVARCHAR'"
			+ ",14, 'DATE' " // 'TT_DATE'"
			+ ",15, 'VARCHAR' " // 'TIME'" - iffy, but time is "A time of day
								// between 00:00:00 (midnight) and 23:59:59
								// (11:59:59 pm), inclusive"
			+ ",16, 'TIMEASTAMP' " // TT_TIMESTAMP'"
			+ ",20, 'NUMBER' "// 'TT_TINYINT'"
			+ ",21, 'NUMBER' "// 'TT_BIGINT'"
			+ ",22, 'VARCHAR' " // 'TT_VARCHAR'"
			+ ",23, 'VARBINARY'" + ",24, 'NVARCHAR' " // TT_NVARCHAR'"
			+ ",25, 'NUMBER'" + ",26, 'CHAR'" + ",27, 'VARCHAR2'" + ",28, 'NCHAR'" + ",29, 'NVARCHAR2'" + ",30, 'DATE'"
			+ ",31, 'TIMESTAMP'" + ",32, 'VARCHAR2'" + ",33, 'NVARCHAR2'" + ",34, 'ROWID'" + ",36, 'CLOB'"
			+ ",37, 'NCLOB'" + ",38, 'BLOB'" + "   , 'VARCHAR') "
			+ " data_type, c.collen data_length , nvl(null,-42) data_scale, nvl(null,-42) data_precision from sys.tables t, sys.columns c  where c.id = t.tblid and tblowner = ? and tblname = ? order by colnum";

	static final String PROC_QUERY = "select owner,package_name, object_name, nvl(overload,-1) overload from all_arguments where data_level = 0  and owner in (user, ?) group by owner,package_name, object_name, overload order by owner,package_name, object_name, overload";

	static final String ARG_QUERY = "select owner,package_name, object_name, nvl(overload,-1) overload, "
			+ "nvl(argument_name , 'out') argument_name, data_type, in_out " + "from all_arguments "
			+ "where package_name = ? " + "and   object_name  = ? " + " and owner = ?" + "and   NVL(overload,-1) = ? "
			+ "and data_level = 0  " + "order by package_name, object_name, overload, sequence";

	static final String SOURCE_QUERY = "select * from all_source where owner in (user,?) and name = ? and type in ('PACKAGE BODY','PROCEDURE','FUNCTION')"
			+ "order by owner,name, type, line";

	static final String TABLEDDL_PKQUERY = "select rtrim(tblname)||'_PK' constraint_name, rtrim(colname) column_name, rownum position, COLOPTIONS  "
			+ " from sys.tables t, sys.columns c  where c.id = t.tblid and tblowner = ? and tblname = ? order by colnum";

	public TimestenConverterImpl(String pHostName, int pPort, String pSid, String pUser, String pPassword,
			String otherUser, String packageName, String rootDirName, int sqlPasses) throws Exception {

		super(pUser, otherUser, packageName, rootDirName);

		w = new OracleConnectionWrangler(pHostName, pPort, pSid, pUser, pPassword, l,OracleConnectionWrangler.TT);

		this.sqlPasses = sqlPasses;

	}

	@Override
	public OtherDBTable[] getTables() {

		theTables.clear();

		try {
			w.confirmConnected();

			PreparedStatement p = w.mrConnection.prepareStatement(TABLENAME_QUERY);
			PreparedStatement c = w.mrConnection.prepareStatement(TABLECOL_QUERY);
			PreparedStatement pk = w.mrConnection.prepareStatement(TABLEDDL_PKQUERY);

			p.setString(1, otherUser);
			p.setString(2, otherUser);

			ResultSet tableNameRS = p.executeQuery();

			while (tableNameRS.next()) {

				String tableOwner = tableNameRS.getString("OWNER");
				String tableName = tableNameRS.getString("TABLE_NAME");

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

					OtherDBTableColumn newCol = new OtherDBTableColumn(columnName.trim(), columnVoltDataType.trim(),
							true);
					colList.add(newCol);

				}

				// Find PK and nullable info
				pk.setString(1, tableOwner);
				pk.setString(2, tableName);

				ResultSet pkRS = pk.executeQuery();

				OtherDBTableColumn[] columns = new OtherDBTableColumn[colList.size()];
				columns = colList.toArray(columns);

				OtherDBTable newTable = new OtherDBTable(tableName, columns);

				int colPos = 1;

				while (pkRS.next()) {
					newTable.setPkName(pkRS.getString("CONSTRAINT_NAME"));
					String colName = pkRS.getString("COLUMN_NAME");

					// last bit is used to indicate PK
					byte[] coloptions = pkRS.getBytes("COLOPTIONS");
					int isPK = coloptions[0] % 2;

					if (isPK == 1) {
						for (int i = 0; i < columns.length; i++) {

							if (columns[i].getColumnName().equals(colName)) {
								columns[i].setInPK(colPos++);
								break;
							}

						}
					}

					// Third bit is used to indicate nullable - default in TT is
					// NOT NULL
					int couldBeNull = coloptions[0];
					couldBeNull = couldBeNull >> 2;
					couldBeNull = couldBeNull % 2;

					if (couldBeNull == 1) {
						for (int i = 0; i < columns.length; i++) {

							if (columns[i].getColumnName().equals(colName)) {
								columns[i].setNotNull(false);
								break;
							}

						}
					}

				}
				pkRS.close();
				theTables.add(newTable);
			}

			c.close();
			p.close();
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

		try {
			w.confirmConnected();

			PreparedStatement p = w.mrConnection.prepareStatement(PROC_QUERY);
			PreparedStatement c = w.mrConnection.prepareStatement(ARG_QUERY);
			PreparedStatement src = w.mrConnection.prepareStatement(SOURCE_QUERY);

			// p.setString(1, w.mrUser);

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

				ResultSet columnRS = c.executeQuery();

				ResultSet sourceRs = src.executeQuery();

				ArrayList<String> srcArrayList = new ArrayList<String>();
				while (sourceRs.next()) {
					srcArrayList.add(sourceRs.getString("TEXT"));
				}

				ArrayList<OtherDbProcParameter> paramInList = new ArrayList<OtherDbProcParameter>();
				ArrayList<OtherDbProcParameter> paramOutList = new ArrayList<OtherDbProcParameter>();

				while (columnRS.next()) {

					String columnVoltJavaDataType = SqlUtils.getUnderlyingVoltJavaDataType(
							SqlUtils.getUnderlyingOracleDatatype(columnRS.getString("DATA_TYPE")), -1, -1, -1);
					String columnVoltDBDataType = SqlUtils.getUnderlyingVoltDBDataType(
							SqlUtils.getUnderlyingOracleDatatype(columnRS.getString("DATA_TYPE")), -1, -1, -1);
					OtherDbProcParameter newParam = new OtherDbProcParameter(columnRS.getString("ARGUMENT_NAME"),
							columnVoltJavaDataType, columnVoltDBDataType);

					String inOut = columnRS.getString("IN_OUT");

					if (!inOut.equals("OUT")) {
						paramInList.add(newParam);
					}

					if (!inOut.equals("IN")) {
						paramOutList.add(newParam);
					}

				}

				String[] procSource = new String[srcArrayList.size()];
				procSource = srcArrayList.toArray(procSource);

				OtherDBProcedures newProc = new OtherDBProcedures(javaPackageName, packName,
						getSaneProcName(packName, objName, overload), paramInList, paramOutList, procSource, l);

				theProcedures.add(newProc);
			}

			c.close();
			p.close();

		} catch (CSException e) {
			l.error(e);
		} catch (SQLException e) {
			l.error(e);
		}

		OtherDBProcedures[] theArray = new OtherDBProcedures[theProcedures.size()];
		theArray = theProcedures.toArray(theArray);

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

		return "TIMESTEN host m_port sid user pass otheruser package_name dir sqlpasses" + System.lineSeparator()
				+ "for example: " + System.lineSeparator()
				+ "TIMESTEN voltconv.cpolcsopv9zq.us-east-1.rds.amazonaws.com 53397 tt_1122 demouser demopass otherusername org.foo /Users/drolfe/foo2 1"
				+ System.lineSeparator();
	}

}
