package com.moonlite.discover;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.*;
import org.jdom2.input.SAXBuilder;

/***
 * 
 * @author Dahai Li
 * 
 * Use JDOM to implement NMap
 *
 */
public class NMapJDOM implements Discover {

	
	/**
	 * 
	 * @param fileName string, the filename containing the output from running "nmap -oX <filename> <network>"
	 * @return list of Host's
	 * @throws JDOMException 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public Host[] readXMLFile (String fileName) throws JDOMException, IOException {
		ArrayList<Host> result = new ArrayList<>();
		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File(fileName);

		/*** A sample host element in the XML file:
		<host starttime="1415838190" endtime="1415838197">
			<status state="up" reason="syn-ack" reason_ttl="249" />
			<address addr="172.23.204.252" addrtype="ipv4" />
			<hostnames>
				<hostname name="asav" type="user" />
				<hostname name="asav" type="PTR" />
			</hostnames>
			<ports>
				<extraports state="filtered" count="98">
					<extrareasons reason="no-responses" count="98" />
				</extraports>
				<port protocol="tcp" portid="23">
					<state state="open" reason="syn-ack" reason_ttl="249" />
					<service name="telnet" method="table" conf="3" />
				</port>
				<port protocol="tcp" portid="443">
					<state state="open" reason="syn-ack" reason_ttl="249" />
					<service name="https" method="table" conf="3" />
				</port>
			</ports>
			<os>
				<portused state="open" proto="tcp" portid="23" />
				<osmatch name="Cisco Adaptive Security Appliance (PIX OS 8.4)"
					accuracy="100" line="16369">
					<osclass type="firewall" vendor="Cisco" osfamily="PIX OS"
						osgen="8.X" accuracy="100">
						<cpe>cpe:/o:cisco:pix_os:8</cpe>
					</osclass>
				</osmatch>
			</os>
			<tcpsequence index="258" difficulty="Good luck!"
				values="CD463246,6C71F087,4194D3F9,7184CECD,45F00C,B1A694D2" />
			<ipidsequence class="Randomized" values="D952,E7DC,9400,8664,B2AA,D92A" />
			<tcptssequence class="none returned (unsupported)" />
			<times srtt="3381" rttvar="1971" to="100000" />
		</host>
		***/
		Document document = builder.build(xmlFile);
		Element rootNode = document.getRootElement();
		for (Element host:  rootNode.getChildren("host")) {
			result.add(new Host(getHostAddress(host), getHostName(host),
					getHostOS(host)));
		}
		Host[] r = new Host[result.size()];
		result.toArray(r);
		return r;
	}

	/***
	 * Discover the network by running the nmap command
	 * @param network
	 * @return Host[]
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws JDOMException
	 */
	public Host[] runCommand (String network) throws IOException, InterruptedException, JDOMException {
		Host[] result = null;
		String fileName = getTempFileName();
		/***
		 * -O option is to detect OS. It can only be run under privileged mode.
		 */
		if (new Shell().run("nmap", "-F", "-O", "-oX", fileName, network) == 0) {
			result = readXMLFile(fileName);
		}
		return result;
	}

	static private String getTempFileName () throws IOException {
		File f = File.createTempFile("nmap", null);
		String result = f.getAbsolutePath();
		f.deleteOnExit();
		return result;
	}
	
	/**
	 * Format of the XML: 	
	 *    <address addr="172.23.204.252" addrtype="ipv4" />
	 * @param host Element
	 * @return String, the host IP address
	 */
	private String getHostAddress (Element host) {
    	return host.getChild("address").getAttributeValue("addr");
	}

	/**
	 * Format of the XML:
		<os>
			<portused state="open" proto="tcp" portid="23" />
			<osmatch name="Cisco Adaptive Security Appliance (PIX OS 8.4)"
				accuracy="100" line="16369">
				<osclass type="firewall" vendor="Cisco" osfamily="PIX OS"
					osgen="8.X" accuracy="100">
					<cpe>cpe:/o:cisco:pix_os:8</cpe>
				</osclass>
			</osmatch>
		</os>
	 * @param host Element
	 * @return String the OS of the host
	 */
	private String getHostOS (Element host) {
		Element os = host.getChild("os");
		return os == null ? null : os.getChild("osmatch").getAttributeValue(
				"name");
	}
	
    /*** Format of the XML:
		<hostnames>
			<hostname name="asav" type="user" />
			<hostname name="asav" type="PTR" />
		</hostnames>
    ***/
	private String getHostName (Element host) {
		Element hostname = host.getChild("hostnames").getChild("hostname");
		return hostname != null ? hostname.getAttributeValue("name") : null;
	}
	
	@Override
	public Host[] discover (Object target) {
		try {
			return runCommand((String)target);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static void printHelp () {
		System.out.print("Usage: \n  java nmap -f <filename>, or\n  java nmap -n <network>\n" +
				"Example:\n  java nmap nmap-output.xml\n  java nmap -n www.cisco.com/24\n");
	}

	/***
	 * For test only
	 * @param args
	 */
	public static void main (String[] args) {
		try {
			if (args.length != 2) {
				printHelp();
				System.exit(-1);
			}
			
			Host[] hosts = null;
			NMap nmap = new NMap();
			switch((args[0])) {
			case "-f":	
				hosts = nmap.readXMLFile(args[1]);
				break;
			case "-n":
				hosts = nmap.discover(args[1]);
				break;
			default:
				printHelp();
				System.exit(-1);
			}
			if (hosts == null || hosts.length == 0) {
				System.out.println("No host detected.");
				System.exit(1);
			}
			for (Host host: hosts) {
				System.out.println("Address: " + host.address + ", Name: " + host.name + ", OS: " + host.os);
			}
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}