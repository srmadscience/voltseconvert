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

public class OtherDBTableColumn {

	String columnName = null;

	String columnVoltDataType = null;

	boolean notNull = false;

	int pkPosition = Integer.MIN_VALUE;

	int precision = Integer.MIN_VALUE;

	public OtherDBTableColumn(String columnName, String columnVoltDataType, boolean notNull) {
		super();
		this.columnName = columnName;
		this.columnVoltDataType = columnVoltDataType;
		this.notNull = notNull;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public String getColumnVoltDataType() {
		return columnVoltDataType;
	}

	public void setColumnVoltDataType(String columnVoltDataType) {
		this.columnVoltDataType = columnVoltDataType;
	}

	public boolean isNotNull() {
		return notNull;
	}

	public void setNotNull(boolean notNull) {
		this.notNull = notNull;
	}

	public boolean isInPK() {
		if (pkPosition > Integer.MIN_VALUE) {
			return true;
		}

		return false;
	}

	public void setInPK(int isInPK) {
		this.pkPosition = isInPK;
	}

	@Override
	public String toString() {

		StringBuffer b = new StringBuffer(columnName);
		
		b.append(' ');
		b.append(columnVoltDataType);

		if (precision > Integer.MIN_VALUE) {
			b.append("(");
			b.append(precision);
			b.append(")");
		}

		if (notNull) {
			b.append(" NOT NULL");
		}

		return b.toString();
	}

	public int getPrecision() {
		return precision;
	}

	public void setPrecision(int precision) {
		this.precision = precision;
	}

}
