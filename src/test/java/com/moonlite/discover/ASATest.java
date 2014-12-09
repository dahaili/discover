package com.moonlite.discover;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.moonlite.discover.ASA.CompositeCommand;

public class ASATest {

    @Test
    public void testGetConfiguration() {
        ASA asa = new ASA("my-f1", "cisco", "cisco");
        try {
            ASA.Configuration config = asa.getConfiguration("192.168.103.1/24");
            Arrays.stream(config.firewall).forEach(System.out::println);
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
}
