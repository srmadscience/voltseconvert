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

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.voltdb.seutils.log.LogInterface;
import org.voltdb.seutils.utils.JavaUtils;

public class OtherDBProcedures {

	String javaPackageName = null;
	String packageName = null;
	String procedureName = null;
	String procedureJavaName = null;

	ArrayList<OtherDbProcParameter> inputParams = new ArrayList<OtherDbProcParameter>();

	ArrayList<OtherDbProcParameter> outputParams = new ArrayList<OtherDbProcParameter>();

	ArrayList<OtherDbSQLStatement> sqlStatements = new ArrayList<OtherDbSQLStatement>();

	String[] procedureSourceCode = new String[0];
	int procedureSourceCodeStartLine = 0;
	int procedureSourceCodeEndLine = 0;

	private String searchAbleProcedureSourceCode = null;

	public final static String EVERYTHING = "EVERY THING";

	public OtherDBProcedures(String javaPackageName, String packageName, String procedureName,
			ArrayList<OtherDbProcParameter> inputParams, ArrayList<OtherDbProcParameter> outputParams,
			String[] procedureSourceCode, LogInterface l) {
		super();
		this.javaPackageName = javaPackageName;
		this.packageName = packageName;
		this.procedureName = procedureName;

		if (packageName == null || packageName.length() == 0) {
			procedureJavaName = JavaUtils.getJavaName(procedureName.toLowerCase(), "JavaStandard.java", l, true);
		} else {
			procedureJavaName = JavaUtils.getJavaName(packageName.toLowerCase() + "_" + procedureName.toLowerCase(),
					"JavaStandard.java", l, true);
		}

		this.inputParams = inputParams;
		this.outputParams = outputParams;
		setProcedureSourceCode(procedureSourceCode);
	}

	public String getProcedureName() {
		return procedureName;
	}

	public String getProcedureJavaName(LogInterface l) {
		return procedureJavaName;
	}

	public void setProcedureName(String procedureName) {
		this.procedureName = procedureName;
	}

	public ArrayList<OtherDbProcParameter> getInputParams() {
		return inputParams;
	}

	public void setInputParams(ArrayList<OtherDbProcParameter> inputParams) {
		this.inputParams = inputParams;
	}

	public ArrayList<OtherDbProcParameter> getOutputParams() {
		return outputParams;
	}

	public void setOutputParams(ArrayList<OtherDbProcParameter> outputParams) {
		this.outputParams = outputParams;
	}

	public ArrayList<OtherDbSQLStatement> getSqlStatements() {
		return sqlStatements;
	}

	public int getSqlStatementCount() {
		return sqlStatements.size();
	}

	public void setSqlStatements(ArrayList<OtherDbSQLStatement> sqlStatements) {
		this.sqlStatements = sqlStatements;
	}

	public String[] getProcedureSourceCode() {
		return procedureSourceCode;
	}

	public void setProcedureSourceCode(String[] procedureSourceCode) {
		this.procedureSourceCode = procedureSourceCode;

		StringBuffer allBuffer = new StringBuffer("");
		StringBuffer someBuffer = new StringBuffer("");

		if (procedureSourceCode != null && procedureSourceCode.length > 0) {

			procedureSourceCodeStartLine = -1;
			procedureSourceCodeEndLine = procedureSourceCode.length - 1;

			for (int i = 0; i < procedureSourceCode.length; i++) {

				// Make a fairly reckless but heuristically plausible assumption
				// -
				// PROCEDURE or FUNCTION and Procedure name will be on same
				// line.
				if ((procedureSourceCode[i].toUpperCase().trim().startsWith("PROCEDURE")
						|| procedureSourceCode[i].toUpperCase().trim().startsWith("FUNCTION"))
						&& procedureSourceCode[i].toUpperCase().indexOf(procedureName.toUpperCase()) > -1) {
					procedureSourceCodeStartLine = i;
				}

				if (procedureSourceCodeStartLine >= 0 && i > 1 && i > procedureSourceCodeStartLine // We've
																									// found
																									// start
																									// of
																									// proc
																									// on
				// a previous line
						&& (procedureSourceCode[i].toUpperCase().trim().startsWith("PROCEDURE")
								|| procedureSourceCode[i].toUpperCase().trim().startsWith("FUNCTION"))
						&& procedureSourceCodeEndLine == procedureSourceCode.length - 1) {
					procedureSourceCodeEndLine = i - 1;

				}

				allBuffer.append(procedureSourceCode[i].toUpperCase().replace("\n", ""));
				allBuffer.append(" ");

				if (i >= procedureSourceCodeStartLine && procedureSourceCodeStartLine > -1
						&& i <= procedureSourceCodeEndLine) {
					someBuffer.append(procedureSourceCode[i].toUpperCase().replace("\n", ""));
					someBuffer.append(" ");
				}

			}

			if (procedureSourceCodeStartLine == -1) {
				procedureSourceCodeStartLine = 0;
			}
		}

		searchAbleProcedureSourceCode = AbstractBaseConverter.removeRegexChars(someBuffer.toString());
	}

	@Override
	public String toString() {

		StringBuffer proc = new StringBuffer();

		if (javaPackageName != null && javaPackageName.length() > 0) {
			proc.append("package " + javaPackageName + ";");
			proc.append(System.lineSeparator());
		}

		proc.append("import org.voltdb.*;");
		proc.append(System.lineSeparator());
		proc.append("public class ");
		proc.append(procedureJavaName);
		proc.append(" extends VoltProcedure {");
		proc.append(System.lineSeparator());
		proc.append(System.lineSeparator());

		for (int i = 0; i < sqlStatements.size(); i++) {
			proc.append(sqlStatements.get(i).toString());
			proc.append(System.lineSeparator());
			proc.append(System.lineSeparator());

		}

		proc.append("public ");

		proc.append(getReturnType());

		proc.append(" run (");

		getArguments(proc);

		proc.append(") throws VoltAbortException {");
		proc.append(System.lineSeparator());

		getSqlStatementParams(proc);
		proc.append(System.lineSeparator());

		proc.append("// output variables");
		proc.append(System.lineSeparator());

		getReturnTypeDefineStatements(proc);
		proc.append(System.lineSeparator());

		addInternalComment(proc);
		proc.append(System.lineSeparator());

		proc.append("// package results to send back to client");
		proc.append(System.lineSeparator());
		getReturnTypeLoadStatements(proc);

		proc.append(System.lineSeparator());

		proc.append("}");
		proc.append(System.lineSeparator());

		proc.append("}");
		proc.append(System.lineSeparator());

		return proc.toString();
	}

	private void addInternalComment(StringBuffer proc) {

		for (int i = procedureSourceCodeStartLine; i <= procedureSourceCodeEndLine; i++) {

			proc.append("// ");
			proc.append(procedureSourceCode[i]);
		}

	}

	private void getSqlStatementParams(StringBuffer proc) {

	}

	private void getReturnTypeDefineStatements(StringBuffer b) {

		if (outputParams.size() == 0) {
			return;
		}

		for (int i = 0; i < outputParams.size(); i++) {
			b.append(outputParams.get(i).toString());
			b.append(";");
			b.append(System.lineSeparator());

		}

	}

	private void getReturnTypeLoadStatements(StringBuffer b) {

		if (outputParams.size() == 0) {
			return;
		}

		b.append(System.lineSeparator());

		if (outputParams.size() == 0) {

			b.append("return null;");
			b.append(System.lineSeparator());

		} else if (outputParams.size() == 1) {

			b.append(outputParams.get(0).toStringAsSoleOutput());
			b.append(System.lineSeparator());

		} else {


			b.append("VoltTable[] results = new VoltTable[");
			b.append(outputParams.size());
			b.append("];");
			b.append(System.lineSeparator());

			for (int i = 0; i < outputParams.size(); i++) {

				b.append(outputParams.get(i).toStringAsVoltTableAssign(i));
				b.append(System.lineSeparator());
				b.append(System.lineSeparator());

			}

			b.append(System.lineSeparator());
			b.append("return results;");
			b.append(System.lineSeparator());

		}

	}

	private void getArguments(StringBuffer b) {

		for (int i = 0; i < inputParams.size(); i++) {
			if (i > 0) {
				b.append(',');
			}
			b.append(inputParams.get(i).toString());
			b.append(System.lineSeparator());

		}

	}

	private String getReturnType() {

		if (outputParams.size() == 0) {
			return "void";
		}

		if (outputParams.size() == 1) {
			return outputParams.get(0).getParamJavaType();
		}

		return "VoltTable[]";
	}

	public boolean addSqlStatementIfUsed(OtherDbSQLStatement theStatement, LogInterface l) {

		if ( theStatement.id.equals("gxkrdagaxn7up") ) {
			System.out.println("d");
		}

		try {

			String[] searchStrings = theStatement.getSearchPatterns();

			for (int i = 0; i < searchStrings.length; i++) {

				Pattern pattern = Pattern.compile(searchStrings[i]);

				Matcher matcher = pattern.matcher(searchAbleProcedureSourceCode);

				if (matcher.find() || procedureName.equals(EVERYTHING)) {

					sqlStatements.add(theStatement);
					l.info("Adding to " + getProcedureJavaName(l));
					return true;

				}

			}
		} catch (Exception e) {
			l.error(e);
		}

		return false;

	}

	public String getPackageName() {
		return javaPackageName;
	}

	public void setPackageName(String packageName) {
		this.javaPackageName = packageName;
	}

}
