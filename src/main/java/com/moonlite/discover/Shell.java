package com.moonlite.discover;

import java.io.*;

/***
 * Shell to execute external command
 * @author Dahai Li
 *
 *@see Runtime, ProcessBuilder(JDK7)
 *http://www.google.com/search?q=java+execute+shell+command
 *
 *
 */
public class Shell {
	public Process process = null;
	/**
	 * 
	 * @param args String[] arguments
	 * @return exit code of running the command.
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public int run (String... args) throws IOException, InterruptedException {
		process = Runtime.getRuntime().exec(args);
		process.waitFor();
		return process.exitValue();
	}

	/**
	 * 
	 * @return the exit value from the last invokation of run
	 */
	public int getExitValue () {
		return process.exitValue();
	}

	public String getStdOut () throws IOException {
		return readStream(process.getInputStream());
	}

	public String getStdErr () throws IOException {
		return readStream(process.getErrorStream());
	}

	private String readStream (InputStream is) throws IOException {
		StringBuffer output = new StringBuffer();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		 
		String line = "";			
		while ((line = reader.readLine())!= null) {
			output.append(line + "\n");
		}
		return output.toString();
	}
}
