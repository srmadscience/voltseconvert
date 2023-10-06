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

public class OtherDbSQLStatement {

	String sql = "";
	String id = "";
	String[] searchPatterns = new String[1];

	private int paramCount = 0;

	public OtherDbSQLStatement(String sql, String id) {
		super();
		formatAndStoreSQL(sql);
		this.id = id;
	}

	private void formatAndStoreSQL(String sql) {

		String text = sql.toUpperCase();

		int param = 0;

		if (text.indexOf(":B1") > -1) {

			param = 1;

			while (true) {

				if (text.indexOf(":B" + param) > -1) {
					text = text.replace(":B" + param, "?");
				} else {
					param--;
					break;
				}

				param++;
			}
		}

		if (text.indexOf(":1") > -1) {

			param = 1;

			while (true) {

				if (text.indexOf(":" + param) > -1) {
					text = text.replace(":" + param, "?");
				} else {
					param--;
					break;
				}

				param++;
			}
		}

		this.sql = text;
		this.paramCount = param;

		searchPatterns[0] = new String(AbstractBaseConverter.removeRegexChars(this.sql));
		searchPatterns[0] = searchPatterns[0].replace("?", "\\w+");

	}

	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "public final SQLStmt sql" + id + " = new SQLStmt(\"" + sql + ";\");";
	}

	public int getParamCount() {
		return paramCount;
	}

	public String[] getSearchPatterns() {

		return searchPatterns;

	}

}
