package com.moonlite.discover;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import jdk.nashorn.internal.ir.annotations.Ignore;

import org.junit.Test;

import com.moonlite.discover.ASA.CompositeCommand;

public class ASATest {

    @Test
    public void test() {
        ASA asa = new ASA("asav", "asadp", "dat@package");
        try {
            ASA.Configuration config = asa.probe(null);
            Arrays.stream(config.accessLists).forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
            fail("ASA.probe");
        }
    }

    @Ignore
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
}
