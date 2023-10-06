package org.voltdb.convert.common;

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

public class OtherDBTable {

	String tableName = null;

	OtherDBTableColumn[] columns = new OtherDBTableColumn[0];

	String comment = null;

	String pkName = null;

	public OtherDBTable(String tableName, OtherDBTableColumn[] columns) {
		super();
		this.tableName = tableName;
		this.columns = columns;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public OtherDBTableColumn[] getColumns() {
		return columns;
	}

	public void setColumns(OtherDBTableColumn[] columns) {
		this.columns = columns;
	}

	@Override
	public String toString() {

		StringBuffer tableDef = new StringBuffer();

		if (comment != null && comment.length() > 0) {
			tableDef.append("//");
			tableDef.append(System.lineSeparator());
			tableDef.append("//");
			tableDef.append(comment);
			tableDef.append(System.lineSeparator());
			tableDef.append("//");
			tableDef.append(System.lineSeparator());
			tableDef.append(System.lineSeparator());
		}

		tableDef.append("CREATE TABLE ");
		tableDef.append(tableName);
		tableDef.append(System.lineSeparator());

		for (int i = 0; i < columns.length; i++) {

			if (i == 0) {
				tableDef.append('(');
			} else {
				tableDef.append(',');
			}
			tableDef.append(columns[i].toString());

			tableDef.append(System.lineSeparator());
		}

		addPk(tableDef);

		tableDef.append(");");
		tableDef.append(System.lineSeparator());

		return tableDef.toString();
	}

	private void addPk(StringBuffer tableDef) {

		if (pkName != null && pkName.length() > 0) {
			tableDef.append(", CONSTRAINT ");
			tableDef.append(pkName);
			tableDef.append(" PRIMARY KEY ");
			tableDef.append(System.lineSeparator());

			int colId = 1;
			boolean notFinished = true;

			char sepChar = '(';

			while (notFinished) {

				boolean found = false;
				for (int i = 0; i < columns.length; i++) {

					if (columns[i].pkPosition == colId) {
						found = true;

						tableDef.append(sepChar);
						sepChar = ',';

						tableDef.append(columns[i].getColumnName());

						colId++;

						break;

					}

				}
				
				if (!found) {
					notFinished = false;
				}
			}

			tableDef.append(")");
			tableDef.append(System.lineSeparator());

		}

	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getPkName() {
		return pkName;
	}

	public void setPkName(String pkName) {
		this.pkName = pkName;
	}

}
