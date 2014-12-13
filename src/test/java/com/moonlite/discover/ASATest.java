package com.moonlite.discover;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Ignore;
import org.junit.Test;

import com.moonlite.discover.ASA.CompositeCommand;

public class ASATest {

    @Ignore
    /***
     * Test getting configuration information of a given network from a given device
     */
    public void testGetConfigurationFromDevice() {
        ASA asa = new ASA("my-f1", "cisco", "cisco");
        try {
            ASA.Configuration config = asa.getConfiguration("192.168.103.1/24");
            if (config.accessGroups != null)
                Stream.of(config.accessGroups).forEach(System.out::println);
            if (config.servicePolicies != null)
                Stream.of(config.servicePolicies).forEach(System.out::println);
            if (config.policyMaps != null)
                Stream.of(config.policyMaps).forEach(System.out::println);
            if (config.classMaps != null)
                Stream.of(config.classMaps).forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
            fail("ASA.getConfiguration");
        }
    }

    @Test
    /***
     * Test getting configuration information of a given network from a file containing
     * configuration of a device.
     */
    public void testGetConfigurationFromLocalFile() {
        try {
            String filePath = getFilePath("asa-running-config.txt");
            String[] lines = Files.lines(Paths.get(filePath)).toArray(String[]::new);
            ASA.Configuration config = new ASA.Configuration(lines, "192.168.103.1/24");
            if (config.accessGroups != null)
                Stream.of(config.accessGroups).forEach(System.out::println);
            if (config.servicePolicies != null)
                Stream.of(config.servicePolicies).forEach(System.out::println);
            if (config.policyMaps != null)
                Stream.of(config.policyMaps).forEach(System.out::println);
            if (config.classMaps != null)
                Stream.of(config.classMaps).forEach(System.out::println);
        } catch (Exception e) {
            e.printStackTrace();
            fail("ASA.getConfiguration");
        }
    }

    @Test
    /**
     * Test the getting the access-group's for the interface residing in a given network.
     */
    public void testGetConfigurationAccessGroups () {
        String[] runningConfig = {
                "interface GigabitEthernet0/0",
                " nameif eigrp_test",
                " security-level 100",
                " ip address 192.168.155.2 255.255.255.0", 
                "interface GigabitEthernet0/0.24",
                " vlan 24",
                " nameif testAgata",
                " security-level 24",
                " ip address 14.23.3.3 255.0.0.0",
                "interface GigabitEthernet0/1",
                " description Description Gig1",
                " shutdown",
                " nameif testNEwipv6",
                " security-level 80",
                " no ip address",
                "interface GigabitEthernet0/2",
                " channel-group 5 mode active",
                " no nameif",
                " no security-level",
                " no ip address",
                "interface GigabitEthernet0/3",
                " nameif inside",
                " security-level 100",
                " ip address 192.168.103.1 255.255.255.0", 
                "interface Management0/0",
                " management-only",
                " nameif mgmt",
                " security-level 100",
                " ip address 172.23.204.155 255.255.255.0", 
                " dhcprelay server 172.23.204.2",
                "access-group eigrp_test_access_in in interface eigrp_test",
                "access-group inside_access_in in interface inside",
                "access-group inside_access_out out interface inside",
                "access-group eigrp_test_access_in global",

        };
        String[] expected = {
                "access-group inside_access_in in interface inside",
                "access-group inside_access_out out interface inside",
                "access-group eigrp_test_access_in global",
        };
        ASA.Configuration config = new ASA.Configuration(runningConfig, "192.168.103.1/24");
        assertArrayEquals(expected, config.accessGroups);
        
        expected = new String[] {"access-group eigrp_test_access_in global"};
        config = new ASA.Configuration(runningConfig, "14.23.3.3/8");
        assertArrayEquals(expected, config.accessGroups);
    }

    @Test
    /**
     * Test the getting the QoS configuration for the interface residing in a given network.
     */
    public void testGetConfigurationQoS() {
        String[] runningConfig = {
                "interface GigabitEthernet0/0",
                " nameif eigrp_test",
                " security-level 100",
                " ip address 192.168.155.2 255.255.255.0", 
                "interface GigabitEthernet0/0.24",
                " vlan 24",
                " nameif testAgata",
                " security-level 24",
                " ip address 14.23.3.3 255.0.0.0",
                "interface GigabitEthernet0/1",
                " description Description Gig1",
                " shutdown",
                " nameif testNEwipv6",
                " security-level 80",
                " no ip address",
                "interface GigabitEthernet0/2",
                " channel-group 5 mode active",
                " no nameif",
                " no security-level",
                " no ip address",
                "interface GigabitEthernet0/3",
                " nameif inside",
                " security-level 100",
                " ip address 192.168.103.1 255.255.255.0", 
                "policy-map type inspect dns preset_dns_map",
                " parameters",
                " message-length maximum client auto",
                " message-length maximum 512",
                "policy-map global_policy",
                " class inspection_default",
                "  inspect ftp",
                "policy-map policy-map-inside",
                "service-policy global_policy global",
                "service-policy policy-map-inside interface inside",
        };
        String[] expectedServicePolicies = {
                "service-policy global_policy global",
                "service-policy policy-map-inside interface inside",
        };
        String[] expectedPolicyMaps = {
                "policy-map global_policy",
                " class inspection_default",
                "  inspect ftp",
                "policy-map policy-map-inside",
        };
        ASA.Configuration config = new ASA.Configuration(runningConfig, "192.168.103.1/24");
        assertArrayEquals(expectedServicePolicies, config.servicePolicies);
        assertArrayEquals(expectedPolicyMaps, CompositeCommand.toStringArray(config.policyMaps));
        
        expectedServicePolicies = new String[] {
                "service-policy global_policy global",
        };
        expectedPolicyMaps = new String[] {
                "policy-map global_policy",
                " class inspection_default",
                "  inspect ftp",
        };
        config = new ASA.Configuration(runningConfig, "14.23.3.3/8");
        assertArrayEquals(expectedServicePolicies, config.servicePolicies);
        assertArrayEquals(expectedPolicyMaps, CompositeCommand.toStringArray(config.policyMaps));
    }
    
    @Test
    /**
     * Test locating the interface resides in a given network
     */
    public void testGetConfigurationInterfaceAndVLAN () {
        String[] runningConfig = {
                "interface GigabitEthernet0/0",
                " nameif eigrp_test",
                " security-level 100",
                " ip address 192.168.155.2 255.255.255.0", 
                "interface GigabitEthernet0/0.24",
                " vlan 24",
                " nameif testAgata",
                " security-level 24",
                " ip address 14.23.3.3 255.0.0.0",
                "interface GigabitEthernet0/1",
                " description Description Gig1",
                " shutdown",
                " nameif testNEwipv6",
                " security-level 80",
                " no ip address",
                "interface GigabitEthernet0/2",
                " channel-group 5 mode active",
                " no nameif",
                " no security-level",
                " no ip address",
                "interface GigabitEthernet0/3",
                " nameif inside",
                " security-level 100",
                " ip address 192.168.103.1 255.255.255.0", 
                "interface Management0/0",
                " management-only",
                " nameif mgmt",
                " security-level 100",
                " ip address 172.23.204.155 255.255.255.0", 
                " dhcprelay server 172.23.204.2",
                "service-policy global_policy global",
                "service-policy policy-map-inside interface inside",
        };
        ASA.Configuration config = new ASA.Configuration(runningConfig, "192.168.103.1/24");
        assertEquals(config.intf.getNameIf(), "inside");
        assertEquals(config.vlan,  0);
        config =  new ASA.Configuration(runningConfig, "14.23.3.3/8");
        assertEquals(config.vlan,  24);
    }

    @Test
    public void testCompositeCommand() {
        String[] lines = {"main", " sub1", "  sub1.1", " sub2", "main2"};
        List<Object> commands = CompositeCommand.lines2Commands(lines, 0);
        CompositeCommand cmd = new CompositeCommand("main",
                Arrays.asList(
                        new CompositeCommand("sub1", Arrays.asList("sub1.1")),
                        "sub2"));

        assertEquals(commands.toArray()[0], cmd);
        assertArrayEquals(lines, CompositeCommand.toStringArray(commands));
        assertEquals(cmd.toString(), "main\n sub1\n  sub1.1\n sub2");
    }
    
    @Ignore
    public void testFlatMap() {
        //Stream::flatMap: to map stream of streams to a single stream
        List<String> a = Arrays.asList("Helo", "world", "!");
        List<String> c = Arrays.asList("热烈", "欢迎");
        List<Object> s = Arrays.asList(a, c);
        Object[] ss = s.stream().flatMap(v -> ((List<String>)v).stream()).toArray();
        assert(ss.equals(new String[] {"Helo", "world", "!", "热烈", "欢迎"}));
        
    }

    private String getFilePath(String fileName) {
        ClassLoader classLoader = getClass().getClassLoader();
        String dirPath = getClass().getPackage().getName().replace('.', '/');
        return classLoader.getResource(dirPath + "/" + fileName).getFile();
    }

}
