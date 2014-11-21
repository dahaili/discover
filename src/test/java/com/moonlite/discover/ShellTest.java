package com.moonlite.discover;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

/**
 * 
 * @author Dahai Li
 *
 * Test Shell class.
 */
public class ShellTest {

	@Test
	public void testRun() {
		Shell sh = new Shell();
		try {
			assertEquals("Exit code of pwd is 0", sh.run("pwd"), 0);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			fail("testRun throws exception");
		}
	}

	@Test
	public void testGetStdOut() {
		Shell sh = new Shell();
		try {
			sh.run("echo", "hello");
			assertEquals("Stdout of 'echo hello' is 'hello'", sh.getStdOut(), "hello\n");
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			fail("testGetStdOut throws exception");
		}
	}

	@Test
	public void testGetStdErr() {
		Shell sh = new Shell();
		try {
			assertFalse(sh.run("ls", "/junk") == 0);
			assertEquals(sh.getStdErr(), "ls: /junk: No such file or directory\n");
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			fail("testGetStdErr throws exception");
		}
	}
}
