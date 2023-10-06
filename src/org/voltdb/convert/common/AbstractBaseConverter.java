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


import java.io.File;

import org.voltdb.seutils.log.ConsoleLog;
import org.voltdb.seutils.log.LogInterface;
import org.voltdb.seutils.utils.CSException;
import org.voltdb.seutils.utils.IOUtils;


public abstract class AbstractBaseConverter implements ConverterDBInterface {

	
	
	private final static  String[] ESCAPE_CHARS = { "<", "(", "*", "[", "{", "^", "-", "=",  "$", "!", "|", "]", "}", ")", /*"?",*/ "*",
			 "+", ".", ">" ,";"};
	
	public String user;
	public String otherUser;
	public String javaPackageName;
	File rootDir;
	File javaDir;
	File sqlDir;
	public LogInterface l;

	public AbstractBaseConverter(String user, String otherUser, String javaPackageName, String rootDirName) {
		super();
		l = new ConsoleLog();

		this.user = user;
		this.otherUser = otherUser;
		this.javaPackageName = javaPackageName;
		this.rootDir = new File(rootDirName);
		sqlDir = new File(rootDir, "sql");
		javaDir = new File(rootDir, "src");

		File[] neededDirs = { rootDir, javaDir, sqlDir };

		for (int i = 0; i < neededDirs.length; i++) {
			if (!neededDirs[i].exists()) {
				l.info("Trying to create code directory " + neededDirs[i]);
				if (!neededDirs[i].mkdirs()) {
					l.error("Unable to create directory, exiting");
					System.exit(1);
				}
			}
		}

	}

	public void createJavaFile(String fileName, String fileContents) {

		File newFile = new File(javaDir, fileName);

		try {
			l.info("Creating file " + newFile.getName());
			IOUtils.loadStringIntoFile(fileContents, newFile, l);
		} catch (CSException e) {
			l.error(e);
		}

	}

	public void createSqlFile(String fileName, String fileContents) {

		File newFile = new File(sqlDir, fileName);

		try {
			l.info("Creating file " + newFile.getName());
			IOUtils.loadStringIntoFile(fileContents, newFile, l);
		} catch (CSException e) {
			l.error(e);
		}

	}



	public abstract OtherDBTable[] getTables();

	public abstract OtherDBProcedures[] getProcedures();

	public void generate() {

		OtherDBTable[] tables = getTables();

		if (tables == null || tables.length == 0) {
			l.info("No tables Found");
		} else {
			for (int i = 0; i < tables.length; i++) {
				createSqlFile(tables[i].getTableName() + ".sql", tables[i].toString());
			}
		}
		
		OtherDBProcedures[] procs = getProcedures();

		if (procs == null || procs.length == 0) {
			l.info("No Procedures Found");
		} else {
			for (int i = 0; i < procs.length; i++) {
				createJavaFile(procs[i].getProcedureJavaName(l) + ".java", procs[i].toString());
			}
		}

	}
	

	
	
	public static String removeRegexChars(String oldString) {
		
		
		String newString = new String(oldString.replaceAll("\\s+", " ").replaceAll("\\s+\\)", ")").replace("( ", "(").replace(" )", ")").replace(" ,", ",").replace(", ",","));
		
		for (int i = 0; i < ESCAPE_CHARS.length; i++) {
			newString =  newString .replace(ESCAPE_CHARS[i], "");
		}
		
		return newString.replaceAll("  ", " ");

	}
	
}
