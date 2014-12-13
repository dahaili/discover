package com.moonlite.discover;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import jdk.nashorn.internal.ir.annotations.Ignore;

import org.junit.Test;

import com.moonlite.discover.ASA.CompositeCommand;

public class ASATest {

    @Test
    public void testGetConfiguration() {
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
    public void testCompositeCommand() {
        String[] lines = {"main", " sub1", "  sub1.1", " sub2", "main2"};
        List<Object> commands = CompositeCommand.lines2Commands(lines, 0);
        assertEquals(commands.toArray()[0],
                new CompositeCommand("main",
                        Arrays.asList(
                                new CompositeCommand("sub1", Arrays.asList("sub1.1")),
                                "sub2")));
        assertArrayEquals(lines, CompositeCommand.toStringArray(commands));
    }
    
    @Test
    public void testFlatMap() {
        //Stream::flatMap: to map stream of streams to a single stream
        List<String> a = Arrays.asList("Helo", "world", "!");
        List<String> c = Arrays.asList("热烈", "欢迎");
        List<Object> s = Arrays.asList(a, c);
        Object[] ss = s.stream().flatMap(v -> ((List<String>)v).stream()).toArray();
        assert(ss.equals(new String[] {"Helo", "world", "!", "热烈", "欢迎"}));
        
    }
}
