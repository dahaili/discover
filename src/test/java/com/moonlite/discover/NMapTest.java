package com.moonlite.discover;

import static org.junit.Assert.*;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.jdom2.JDOMException;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

public class NMapTest {

	private String getFilePath(String fileName) {
		ClassLoader classLoader = getClass().getClassLoader();
		String dirPath = getClass().getPackage().getName().replace('.', '/');
		return classLoader.getResource(dirPath + "/" + fileName).getFile();
	}

	@Test
	public void testReadXMLFile() {
		NMap nmap = new NMap();
		String fileName = getFilePath("sample-nmap.xml");
	    try {
			Host[] hosts = nmap.readXMLFile(fileName);
			Host[] expecteds = {
					new Host("172.23.204.252", "asav", "Cisco Adaptive Security Appliance (PIX OS 8.4)"),
					new Host("172.23.204.155", "my-f1", "Cisco Adaptive Security Appliance (PIX OS 8.4)")
				};
			for (Host host: hosts) {
				System.out.println("Address: " + host.address + ", Name: " + host.name + ", OS: " + host.os);
			}
			assertArrayEquals(expecteds, hosts);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			fail("Fails to read the file");
		}
	}


	@Test
	public void testReadXMLFileWithJDOM() {
		NMapJDOM nmap = new NMapJDOM();
		String fileName = getFilePath("sample-nmap.xml");
	    try {
			Host[] hosts = nmap.readXMLFile(fileName);
			Host[] expecteds = {
					new Host("172.23.204.252", "asav", "Cisco Adaptive Security Appliance (PIX OS 8.4)"),
					new Host("172.23.204.155", "my-f1", "Cisco Adaptive Security Appliance (PIX OS 8.4)")
				};
			assertArrayEquals(expecteds, hosts);
			for (Host host: hosts) {
				System.out.println("Address: " + host.address + ", Name: " + host.name + ", OS: " + host.os);
			}
		} catch (IOException | JDOMException e) {
			fail("Fails to read the file");
		}
	}
}
