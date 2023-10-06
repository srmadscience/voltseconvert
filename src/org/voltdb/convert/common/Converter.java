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

import org.voltdb.convert.oracle.OracleConverterImpl;
import org.voltdb.convert.timesten.TimestenConverterImpl;

public class Converter {

	private static final String ORACLE = "ORACLE";
	private static final String TIMESTEN = "TIMESTEN";

	public static void main(String[] args) {

		if (args.length < 1) {
			System.err.println(printUsage());
			System.exit(1);
		}

		ConverterDBInterface i = null;

		if (args[0].equalsIgnoreCase(ORACLE)) {

			if (args.length != 10) {
				System.err.println(OracleConverterImpl.getUsageMethod());
				System.exit(3);
			}

			String host = args[1];

			int port = 1521;
			
			try {
				port = Integer.parseInt(args[2]);
			} catch (NumberFormatException e1) {
				System.err.println("'m_port' must be an integer");
				System.exit(4);
			}
			
			String sid = args[3];
			String user = args[4];
			String pass = args[5];
			String otheruser = args[6];
			String pack = args[7];
			String dirname = args[8];

			int sqlPasses = 1;
			try {
				sqlPasses = Integer.parseInt(args[9]);
			} catch (NumberFormatException e1) {
				System.err.println("'sqlpasses' must be an integer");
				System.exit(4);
			}

			try {
				i = new OracleConverterImpl(host, port, sid, user, pass, otheruser, pack, dirname, sqlPasses);
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else if (args[0].equalsIgnoreCase(TIMESTEN)) {

			if (args.length != 10) {
				System.err.println(TimestenConverterImpl.getUsageMethod());
				System.exit(3);
			}

			String host = args[1];

			int port = 53397;
			
			try {
				port = Integer.parseInt(args[2]);
			} catch (NumberFormatException e1) {
				System.err.println("'m_port' must be an integer");
				System.exit(4);
			}
			
			String sid = args[3];
			String user = args[4];
			String pass = args[5];
			String otheruser = args[6];
			String pack = args[7];
			String dirname = args[8];

			int sqlPasses = 1;
			try {
				sqlPasses = Integer.parseInt(args[9]);
			} catch (NumberFormatException e1) {
				System.err.println("'sqlpasses' must be an integer");
				System.exit(4);
			}

			try {
				i = new TimestenConverterImpl(host, port, sid, user, pass, otheruser, pack, dirname, sqlPasses);
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else {
			printUsage();
			System.exit(0);
		}

		i.generate();

		System.exit(0);

	}

	private static String printUsage() {
		return "Usage : " + TimestenConverterImpl.getUsageMethod() + "or \n Usage: " + OracleConverterImpl.getUsageMethod();
	}

}
