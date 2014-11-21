package com.moonlite.discover;

import java.io.PrintStream;

/***
 * 
 * @author Dahai Li
 * JRun let you run shell command in Java.
 */
public class JRun {
	public static void main(String[] args) {
		Shell sh = new Shell();
		try {
			int rc = sh.run(args);
			PrintStream out = (rc == 0)? System.out : System.err;
			out.print(rc == 0? sh.getStdOut(): sh.getStdErr());
			out.close();
			System.exit(rc);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
