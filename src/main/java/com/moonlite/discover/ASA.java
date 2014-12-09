package com.moonlite.discover;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.sun.xml.internal.ws.util.StringUtils;

import javafx.util.Pair;

/**
 * Data model of an ASA device.
 * 
 * @author Dahai Li
 *
 */
public class ASA extends Host {
    private String user;
    private String password;
    
    
    public ASA (String address, String user, String password) {
        super(address, "", "ASA");
        this.user = user;
        this.password = password;
    }

    /**
     * Gather the configuration information of the device.
     * @param network String, the filer used to identify the network of interest.
     * @return configuration for the given network.
     * @throws IOException 
     */
    public Configuration probe(String network) throws IOException {
        Configuration config = new Configuration();
        String path = "https://" +address + "/admin/config";
        String cfg  = new ApacheHttpClient().get(path, user, password);
        
        String[] lines = Arrays.asList(cfg.split("\n")).
                stream().
                filter(line -> ! line.startsWith("!")). //skip comments
                toArray(String[]::new);
        List<Object> commands = CompositeCommand.lines2Commands(lines, 0);

        Stream<Object> accessLists = commands.stream().filter(cmd -> cmd.toString().startsWith("interface "));
        config.accessLists = accessLists.toArray(Object[]::new);

        return config;
    }

    /**
     * 
     * CompoisteCommand models ASA CLI that has sub-commands.
     *
     */
    static class CompositeCommand {
        final String command;
        final boolean isNo;
        final List<Object> subCommands;
        
        CompositeCommand(String command) {
            this(command, false, null);
        }

        CompositeCommand(String command, List<Object> subCommands) {
            this(command, false, subCommands);
        }

        CompositeCommand(String command, boolean isNo) {
            this(command, isNo, null);
        }

        CompositeCommand(String command, boolean isNo, List<Object> subCommands) {
            this.command = command;
            this.isNo = isNo;
            this.subCommands = subCommands;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof CompositeCommand)) {
               return false; 
            }
            CompositeCommand other = (CompositeCommand)obj;
            return command.equals(other.command) &&
                    isNo == other.isNo &&
                    (subCommands != null && subCommands.equals(other.subCommands));
        }

        @Override
        public String toString() {
            if (isNo)
                return "no " + command;
            StringBuffer buf = new StringBuffer(command);
            for (Object c: subCommands) {
                for (String line: c.toString().split("\n"))
                    buf.append("\n ").append(line);
            }
            return buf.toString();
        }

        public static String[] toStringArray(List<Object> cmds) {
            StringBuffer buf = null;
            for (Object cmd: cmds) {
                if (buf == null)
                    buf = new StringBuffer(cmd.toString());
                else 
                    buf.append("\n").append(cmd.toString());
            }
            return buf.toString().split("\n");
        }

        public static String joinStringArray(String glue, String[] array) {
            StringBuffer buf = null;
            for (String s: array) {
                if (buf == null)
                    buf = new StringBuffer(s);
                else
                    buf.append(glue).append(s);
            }
            return buf.toString();
        }
        
        /**
         * Convert a list of lines in ASA configuration into a list of String's or CompositeCommand's.
         * @param lines String[], lines of running-config
         * @param int pos, the index of the line in lines to start
         * @return List of String or CompositeCommand
         */
        static public List<Object> lines2Commands(String[] lines,  int pos) {
            List<Object> result = new ArrayList<>();
            int n = lines.length;
            while (pos < n) {
                CommandPos cp = convertOneCommand(lines, pos, 0);
                result.add(cp.command);
                pos = cp.pos;
            }
            return result;
        }

        /**
         * Convert the CLI line to a simple command (string), or a CompositeCommand
         * @param lines String[], lines of running-config
         * @param int pos, the index of the line in lines to start
         * @param indent int, the number of spaces for sub-command
         * @return CommandPos a pair: (command, pos), command is the resulting command, and pos is
         * the pos of the next line to process.
         */
        static private CommandPos convertOneCommand(String[] lines, int pos, int indent) {
            //'''Convert a CLI in raw format from "running-config" to a command.
            String line = lines[pos];
            Object cmd;
            if (line.startsWith("no ")) {
                cmd = new  CompositeCommand(line.substring(3).trim(), true);
                pos += 1;
            } else {
                SubCommandsPos result = getSubCommands(lines, pos+1, indent+1);
                if (result.subCommands != null) {
                    cmd = new CompositeCommand(line.trim(), result.subCommands);
                } else
                    cmd = line.trim();
                pos = result.pos;
            }
            return new CommandPos(cmd, pos);
        }


        /***
         * Create a list of sub commands from the given position in the input CLI list.
         * 
         * @param lines String[]
         * @param pos int
         * @param indent int
         * @return tuple: (list of commands, int)
                the first value of the tuple is a list of commands,
                the second value of the tuple is position of the next un-processed line in lines
         */
        static private SubCommandsPos getSubCommands(String[] lines, int pos, int indent) {
            int n = lines.length;
            List<Object> result = new ArrayList<>();
            while (pos < n) {
                String line = lines[pos];
                if (line.length() < indent || line.substring(0, indent).trim().length() != 0)
                    break;
                CommandPos cmdPos = convertOneCommand(lines, pos, indent);
                result.add(cmdPos.command);
                pos = cmdPos.pos;
            }
            return new SubCommandsPos(result.size() == 0? null : result, pos);
        }
            
        static private class CommandPos {
            final Object command;
            final int pos;

            CommandPos (Object command, int pos) {
                this.command = command;
                this.pos = pos;
            }
        }
        
        static private class SubCommandsPos {
            final List<Object> subCommands;
            final int pos;

            SubCommandsPos (List<Object> subCommands, int pos) {
                this.subCommands = subCommands;
                this.pos = pos;
            }
        }
    }
    
    static public class Configuration {
        public Object[] accessLists;
        public Object[] qos;
        private Object[] vpn;
    }
}
