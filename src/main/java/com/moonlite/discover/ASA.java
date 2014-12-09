package com.moonlite.discover;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;

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
     *        it must be in CDIR notation, e.g) 1.1.1.0/24.
     * @return configuration for the given network.
     * @throws IOException 
     */
    public Configuration getConfiguration(String network) throws IOException {
        Configuration config = new Configuration();
        String path = "https://" +address + "/admin/config";
        String cfg  = new ApacheHttpClient().get(path, user, password);
        
        String[] lines = Arrays.asList(cfg.split("\n")).
                stream().
                filter(line -> ! line.startsWith("!")). //skip comments
                toArray(String[]::new);
        List<Object> commands = CompositeCommand.lines2Commands(lines, 0);

        //locate the interface that is within the network.
        InterfaceCommand intf = getInterfaceCommand(commands, network);
        if (intf == null) {
            return null;
        }
        String nameIf = intf.getNameIf();
        config.vlan = intf.getVLAN();
        config.firewall = getFirewall(commands, nameIf);//NAT, and ServicePolicy?
//        config.qos = getQoS(commands, nameIf);

        return config;
    }

    /**
     * Get the firewall related configurations, such as ACL, NAT, and ServicePolicy
     * @param commands List<Object> String's and CompositeCommand's
     * @param nameIf Sting, nameif from interface command.
     * @return Object[] commands
     */
    private Object[] getFirewall(List<Object> commands, String nameIf) {
        // get the access-group for the interface: "access-group inside_access_out out interface inside"
        Set<String> aclNames = new HashSet<>();
        commands.stream().
                filter(cmd -> cmd.toString().startsWith("access-group ") &&
                        (cmd.toString().endsWith(" global") ||  cmd.toString().split(" ")[4].equals(nameIf))).
                forEach(cmd -> aclNames.add(cmd.toString().split(" ")[1]));
        Object[] acls = commands.stream().
                filter(cmd -> cmd.toString().startsWith("access-list ") && 
                        aclNames.contains(cmd.toString().split(" ")[1])).
                toArray();
        //TODO take care of NAT, ServicePolicy?
        return acls;
    }

    /**
     * Get the InterfaceCommand for the interface in a given netowrk.
     * @param commands List<Object> of ASA commands
     * @param network String network
     * @return InterfaceCommand for the interface that is in the given network
     */
    private static InterfaceCommand getInterfaceCommand(List<Object> commands, String network) {
        Function <Object, Boolean> isInSubnet = obj -> {
            if (obj instanceof String) {
                return false;
            }
            CompositeCommand c = (CompositeCommand) obj;
            if (!(c.command.startsWith("interface ")))
                    return false;
            SubnetInfo subnetInfo = new SubnetUtils(network).getInfo();
            InterfaceCommand intf = new InterfaceCommand(c.command, c.subCommands);
            String address = intf.getIpAddress();
            return address != null && subnetInfo.isInRange(address);
        };

        Optional<Object> hit = commands.stream().
                filter(c -> isInSubnet.apply(c)).
                findFirst();
        if (hit.isPresent()) {
            CompositeCommand c = (CompositeCommand) hit.get();
            return new InterfaceCommand(c.command, c.subCommands);
        }
        return null;
    }

    static class InterfaceCommand extends CompositeCommand {
        InterfaceCommand(String command, List<Object> subCommands) {
            super(command, subCommands);
        }
     
        String getNameIf () {
            String[] words = getSubCommandWords("nameif ");
            if (words == null) {
                return null;
            } else {
                return words[1];
            }
        }

        String getIpAddress() {
            String[] words = getSubCommandWords("ip address ");
            if (words == null) {
                return null;
            }
            return words[2];
        }
        
        int getVLAN() {
            String[] subCommandWords = getSubCommandWords("vlan ");
            if (subCommandWords == null) {
                return 0;
            } else {
                return Integer.parseInt(subCommandWords[1]);
            }
        }

        /**
         * Get the sub-command with the given prefix
         * @param prefix String
         * @return String[] words in the sub-command if found, or null
         */
        String[] getSubCommandWords(String prefix) {
            Optional<Object> hit = this.subCommands.stream().filter(line -> ((String)line).startsWith(prefix)).findFirst();
            if (hit.isPresent()) {
                return ((String) hit.get()).split(" ");
            } else {
                return null;
            }
        }
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
        public int vlan;
        public Object[] buildingBlocks;
        public Object[] firewall;
        public Object[] qos;
        private Object[] vpn;
    }
}
