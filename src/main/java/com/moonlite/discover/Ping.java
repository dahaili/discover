package com.moonlite.discover;

import java.io.IOException;
import java.net.InetAddress;

/**
 * 
 * @author dli
 * Ping: //http://stackoverflow.com/questions/11506321/java-code-to-ping-an-ip-address
 * It suggests to use ping command.http://alvinalexander.com/java/java-ping-class
 * InetAddress.isReachable does not work at all. 
 * 
 * Or use this tool:
 * http://javapingtool.com/
 * 
 */
public class Ping {
	static boolean ping(String address) throws IOException {
		InetAddress inet = InetAddress.getByName(address);
		return inet.isReachable(5000);
	}
}
