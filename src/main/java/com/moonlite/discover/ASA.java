package com.moonlite.discover;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    
    
    public ASA (final String address, final String user, final String password) {
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
    public Configuration getConfiguration(final String network) throws IOException {
        final String path = "https://" +address + "/admin/config";
        final String cfg  = new ApacheHttpClient().get(path, user, password);
        return new Configuration(cfg, network);
    }


    /***
     * Send the configuration to this ASA.
     * @param cfg Configuration
     * @return delivery result
     */
    public Object setConfiguration(Configuration cfg) {
        //TODO implementation
        return null;
    }

    /**
     * 
     * InterfaceCommand represents the ASA CLI for interface.
     *
     */
    static public class InterfaceCommand extends CompositeCommand {
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
    static public class CompositeCommand {
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
            return command + "\n "
                + subCommands.stream()
                .flatMap(cmd -> Stream.of(cmd.toString().split("\n")))
                .collect(Collectors.joining("\n "));
        }

        public static String[] toStringArray(Stream<Object> cmds) {
            return cmds.map(Object::toString)
                    .collect(Collectors.joining("\n"))
                    .split("\n");
        }
        
        public static String[] toStringArray(List<Object> cmds) {
            return toStringArray(cmds.stream());
        }

        public static String[] toStringArray(Object[] cmds) {
            return toStringArray(Stream.of(cmds));
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
    
    /**
     * 
     * Configuration stores the running-configuration of an ASA device for or given network.
     *
     */
    static public class Configuration {
        // The interface that resides in the concerned network.
        public InterfaceCommand intf;
        public int vlan;

        // Firewall
        public Object[] accessGroups;
        // QoS
        public Object[] servicePolicies;
        public Object[] policyMaps;
        public Object[] classMaps;
        // VPN
        
        // Building blocks
        public Object[] accessLists;
        public Object[] ObjectNetworks;
        public Object[] ObjectNetworkGroups;
        public Object[] ObjectServcies;
        public Object[] ObjectServiceGroups;

        // internal use
        private Set<String> aclNames;

        public Configuration(final String runningConfig, final String network) {
            this(Arrays.asList(runningConfig.split("\n")).
                    stream().
                    filter(line -> ! line.startsWith("!")). //skip comments
                    toArray(String[]::new), 
                    network);
        }

        public Configuration(String[] lines, String network) {
            final List<Object> commands = CompositeCommand.lines2Commands(lines, 0);

            //locate the interface that is within the network.
            intf = getInterfaceCommand(commands, network);
            if (intf == null) {
                return;
            }
            String nameIf = intf.getNameIf();
            vlan = intf.getVLAN();
            getFirewall(commands, nameIf);
            getQoS(commands, nameIf);
            getACLs(commands);
        }


        @Override
        /***
         * @return the configuration in a text format acceptable by ASA
         */
        public String toString() {
            //TODO implementation
            return super.toString();
        }

        /***
         * 
         * @return the configuration in XML format that can send to ASA via HTTP
         */
        public String toXML() {
            //TODO 
            return null;
        }

        /**
         * Get the InterfaceCommand for the interface in a given network.
         * @param commands List<Object> of ASA commands
         * @param network String network
         * @return InterfaceCommand for the interface that is in the given network
         */
        private static InterfaceCommand getInterfaceCommand(List<Object> commands, String network) {
            Function <Object, Boolean> isTheInterfaceCmd = cmd -> {
                if (cmd instanceof String) {
                    return false;
                }
                CompositeCommand c = (CompositeCommand) cmd;
                if (!(c.command.startsWith("interface ")))
                        return false;
                SubnetInfo subnetInfo = new SubnetUtils(network).getInfo();
                InterfaceCommand intf = new InterfaceCommand(c.command, c.subCommands);
                String address = intf.getIpAddress();
                return address != null && subnetInfo.isInRange(address);
            };

            Optional<Object> hit = commands.stream()
                    .filter(cmd -> isTheInterfaceCmd.apply(cmd))
                    .findFirst();
            if (hit.isPresent()) {
                CompositeCommand c = (CompositeCommand) hit.get();
                return new InterfaceCommand(c.command, c.subCommands);
            }
            return null;
        }
        
        /**
         * Get the firewall related configurations for an interface, such as ACL, NAT
         * @param commands List<Object> String's and CompositeCommand's
         * @param nameIf Sting, nameif from interface command.
         * @return void
         */
        private void getFirewall(List<Object> commands, String nameIf) {
            // get the access-group for the interface: "access-group inside_access_out out interface inside"
            // gather the global access-group, " "access-group inside_access_out global", as well.
            accessGroups = commands.stream()
                    .filter(cmd -> { 
                        if (cmd instanceof CompositeCommand) {
                            return false;
                        }
                        String[] words = mainCommandWords(cmd);
                        return words[0].equals("access-group") &&
                                (words[2].equals("global") || words[4].equals(nameIf));
                    })
                    .toArray();
            //starts to accumulate the names of the access-lists used.
            aclNames = Stream.of(accessGroups)
                    .map(accessGroup -> mainCommandWords(accessGroup)[1]).collect(Collectors.toSet());
        }

        /**
         * Get QoS configuration for an interface: service-policy, policy-map, and class-map
         * @param commands List<Object>: running-config
         * @param nameIf String: the name of the interested interface
         * @return void
         */
        private void getQoS(List<Object> commands, String nameIf) {
            // TODO service-policy, policy-map, class-map, ACLs.

            /**
             * pick up the global service-policy: "service-policy <name> global",
             * and interface specific one: "service-policy <policy-map-name> interface <nameIf>"
             * 
             */
            servicePolicies = commands.stream()
                    .filter(cmd -> {
                        if (!(cmd instanceof String)) {
                            return false;
                        }
                        String[] words = mainCommandWords(cmd);
                        int len = words.length;
                        return words[0].equals("service-policy") &&
                               (words[len-1].equals("global") || words[len-1].equals(nameIf));})
                    .toArray();

            /**
             * Pickup the policy-map commands used by servicePolicies
             */
            Set<Object> policyMapNames = Stream.of(servicePolicies)
                    .map(cmd -> mainCommandWords(cmd)[1])
                    .collect(Collectors.toSet());
            // policy-map command format: "policy-map <name>"
            policyMaps = commands.stream()
                    .filter(cmd -> mainCommand(cmd).startsWith("policy-map "))
                    .filter(cmd -> policyMapNames.contains(mainCommandWords(cmd)[1]))
                    .toArray();

            /**
             * Pickup the class-map names used by policyMaps
             */
            // class-map names are discovered under the policy-map sub-command "class <classMapName>". 
            Set<String>classMapNames = Stream.of(policyMaps)
                    .filter(policyMap -> policyMap instanceof CompositeCommand)
                    .flatMap(policyMap -> ((CompositeCommand)policyMap).subCommands.stream())
                    .filter(subCmd -> mainCommand(subCmd).startsWith("class "))
                    .map(classSubCmd -> mainCommandWords(classSubCmd)[1])
                    .collect(Collectors.toSet());
            // class-map commands are of the format: "class-map <name>"
            classMaps = commands
                    .stream()
                    .filter(cmd -> mainCommand(cmd).startsWith("class-map "))
                    .filter(classMapCmd -> classMapNames.contains(mainCommandWords(classMapCmd)[1]))
                    .toArray();
 
            //TODO gather the names of the access-lists used by classMaps;
//            Set<String> aclName;
//            this.aclNames.addAll(aclNames);
        }

        /**
         * populate accessLists with all the access-list commands whose name is in aclNames.
         * @param commands List<Object> running-config
         */
        private void getACLs(List<Object> commands) {
            // gather all the acls applied to the nameIf interface as well as the global acl.
            accessLists = commands.stream()
                    .filter(cmd -> {
                        if (!(cmd instanceof String)) {
                            return false;
                        }
                        String[] words = mainCommandWords(cmd);
                        return words[0].equals("access-list") && 
                            aclNames.contains(words[1]);})
                    .toArray();
        }


        private static String mainCommand(Object cmd) {
            return cmd instanceof String? (String)cmd : ((CompositeCommand)cmd).command;
        }

        private static String[] mainCommandWords(Object cmd) {
            return mainCommand(cmd).split(" ");
        }
    }
}
