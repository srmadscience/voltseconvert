package org.voltdb.convert.common;

import org.voltdb.seutils.wranglers.oracle.SqlUtils;

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


public class OtherDbProcParameter {

	String paramName = null;
	
	String paramJavaType = null;

	 String voltDbType = null;

	public OtherDbProcParameter(String paramName, String paramJavaType, String voltDbType) {
		super();
		this.paramName = paramName;
		this.paramJavaType = paramJavaType;
		this.voltDbType = voltDbType;
	}

	public String getParamName() {
		return paramName;
	}

	public void setParamName(String paramName) {
		this.paramName = paramName;
	}

	public String getParamJavaType() {
		return paramJavaType;
	}

	public void setParamJavaType(String paramJavaType) {
		this.paramJavaType = paramJavaType;
	}
	
	@Override
	public String toString() {
		return paramJavaType + " " + SqlUtils.toJavaName(paramName);
	}

	public String toStringAsOutputVariable() {
		return paramJavaType + " " + SqlUtils.toJavaName(paramName) + ";";
	}
	
	public String toStringAsVoltTableAssign(int pos) {
		
		if (paramJavaType.equals("VoltTable")) {
		return "results[" + pos + "] = " + SqlUtils.toJavaName(paramName) + ";";
		}
		
		StringBuffer b = new StringBuffer();
		
		b.append("VoltTable t");
		b.append(pos);
		b.append(" = new VoltTable(");
		b.append(System.lineSeparator());
		
	    b.append("  new VoltTable.ColumnInfo(\"");
	    
	    b.append(paramName);
	    b.append("\" , ");
	    b.append(SqlUtils.getVoltDBDataTypeEnumerationName(voltDbType));
	    b.append("));");
		b.append(System.lineSeparator());
	    
 	    b.append("t");
        b.append(pos);
        b.append(".addRow(");
        b.append(SqlUtils.toJavaName(paramName));
        b.append(");");
		b.append(System.lineSeparator());
		
		b.append("results[");
		b.append(pos);
		b.append("] = t");
        b.append(pos);
		b.append(";");
		b.append(System.lineSeparator());
		
		return b.toString();
	}
	
	public String toStringAsSoleOutput() {
		return "return " + SqlUtils.toJavaName(paramName) + ";";
	}


	
}
