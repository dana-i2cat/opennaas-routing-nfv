package org.opennaas.extensions.vrf.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.opennaas.extensions.openflowswitch.model.FloodlightOFAction;
import org.opennaas.extensions.openflowswitch.model.FloodlightOFFlow;
import org.opennaas.extensions.openflowswitch.model.FloodlightOFMatch;
import org.opennaas.extensions.openflowswitch.model.OFFlow;
import org.opennaas.extensions.sdnnetwork.model.NetworkConnection;
import org.opennaas.extensions.sdnnetwork.model.Port;
import org.opennaas.extensions.vrf.model.L2Forward;
import org.opennaas.extensions.vrf.model.VRFRoute;

/**
 *
 * @author Josep Batallé (josep.batalle@i2cat.net)
 */
public class Utils {

    static Log log = LogFactory.getLog(Utils.class);
    
    public static int detectIPVersion(String ip){
        if (Utils.isIPv4Address(ip)){
            return 4;
        } else if (Utils.isIpAddress(ip) == 6 ) {
            return 6;
        } else {
            return 0;
        }
    }

    /**
     * Accepts an IPv4 address and returns of string of the form xxx.xxx.xxx.xxx
     * ie 192.168.0.1
     *
     * @param ipAddress
     * @return
     */
    public static String intIPv4toString(int ipAddress) {
        StringBuilder sb = new StringBuilder();
        int result;
        for (int i = 0; i < 4; ++i) {
            result = (ipAddress >> ((3 - i) * 8)) & 0xff;
            sb.append(Integer.valueOf(result).toString());
            if (i != 3) {
                sb.append(".");
            }
        }
        return sb.toString();
    }

    /**
     * Accepts an IPv4 address of the form xxx.xxx.xxx.xxx, ie 192.168.0.1 and
     * returns the corresponding 32 bit integer.
     *
     * @param ipAddress
     * @return
     */
    public static int StringIPv4toInt(String ipAddress) {
        if (ipAddress == null) {
            throw new IllegalArgumentException("Specified IPv4 address must"
                    + "contain 4 sets of numerical digits separated by periods");
        }
        String[] octets = ipAddress.split("\\.");
        if (octets.length != 4) {
            throw new IllegalArgumentException("Specified IPv4 address must"
                    + "contain 4 sets of numerical digits separated by periods");
        }

        int result = 0;
        for (int i = 0; i < 4; ++i) {
            int oct = Integer.valueOf(octets[i]);
            if (oct > 255 || oct < 0) {
                throw new IllegalArgumentException("Octet values in specified"
                        + " IPv4 address must be 0 <= value <= 255");
            }
            result |= oct << ((3 - i) * 8);
        }
        return result;
    }

    /**
     * Is an IPv4 address received from the controller. In integer format
     *
     * @param ipAddress
     * @return
     */
    public static boolean isIPv4Address(String ipAddress) {
        return !ipAddress.contains(":");
    }

    public static String tryToCompressIPv6(String ipv6) {
        if (ipv6.contains("::")) {
            return ipv6;
        } else {
            return ipv6.replaceFirst(":0:", "::");
        }
    }

    /**
     * Check the IP version of the address
     *
     * @param ipAddress
     * @return IP version of the address
     */
    public static int isIpAddress(String ipAddress) {
        Pattern VALID_IPV4_PATTERN = null;
        Pattern VALID_IPV6_PATTERN = null;
        String ipv4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
        final String ipv6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";
        final String IPV6_HEX4DECCOMPRESSED_REGEX = "\\A((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?) ::((?:[0-9A-Fa-f]{1,4}:)*)(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}\\z";
        final String IPV6_6HEX4DEC_REGEX = "\\A((?:[0-9A-Fa-f]{1,4}:){6,6})(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}\\z";
        final String IPV6_HEXCOMPRESSED_REGEX = "\\A((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)\\z";
        final String IPV6_REGEX = "\\A(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}\\z";
        try {
            VALID_IPV4_PATTERN = Pattern.compile(ipv4Pattern, Pattern.CASE_INSENSITIVE);
            VALID_IPV6_PATTERN = Pattern.compile(IPV6_HEXCOMPRESSED_REGEX, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            //logger.severe("Unable to compile pattern", e);
        }
        Matcher m1 = VALID_IPV4_PATTERN.matcher(ipAddress);
        if (m1.matches()) {
            return 4;
        }
        Matcher m2 = VALID_IPV6_PATTERN.matcher(ipAddress);
        if (m2.matches()) {//Compressed
            return 6;
        }
        //NO match. Try to find with other patterns
        VALID_IPV6_PATTERN = Pattern.compile(IPV6_REGEX, Pattern.CASE_INSENSITIVE);
        m2 = VALID_IPV6_PATTERN.matcher(ipAddress);
        if (m2.matches()) {
            return 6;
        }
        VALID_IPV6_PATTERN = Pattern.compile(ipv6Pattern, Pattern.CASE_INSENSITIVE);
        m2 = VALID_IPV6_PATTERN.matcher(ipAddress);
        if (m2.matches()) {
            return 6;
        }
        VALID_IPV6_PATTERN = Pattern.compile(IPV6_HEX4DECCOMPRESSED_REGEX, Pattern.CASE_INSENSITIVE);
        m2 = VALID_IPV6_PATTERN.matcher(ipAddress);
        if (m2.matches()) {
            return 6;
        }
        VALID_IPV6_PATTERN = Pattern.compile(IPV6_6HEX4DEC_REGEX, Pattern.CASE_INSENSITIVE);
        m2 = VALID_IPV6_PATTERN.matcher(ipAddress);
        if (m2.matches()) {
            return 6;
        }

        return 0;
    }

    /**
     * Usually addr1 is subnet address and addr2 is ip address. Function will
     * return true, if addr2 is within addr1(subnet), or if addr1 and addr2 are
     * equals
     *
     * @param addr1 Subnet address
     * @param addr2 Subnet address or destination address
     * @return
     */
    public static boolean netMatch(String addr1, String addr2) {
        TreeMap<String, Integer> subnet1 = extractIPandMask(addr1);
        TreeMap<String, Integer> subnet2 = extractIPandMask(addr2);

        if (subnet1.containsValue(0) && subnet2.containsValue(0)) {
            if (subnet1.containsKey(subnet2.lastKey())) {
                return true;
            }
        }

        Inet4Address a1 = null;
        Inet4Address a2 = null;
        try {
            a1 = (Inet4Address) InetAddress.getByName(subnet1.lastKey());
            a2 = (Inet4Address) InetAddress.getByName(subnet2.lastKey());
        } catch (UnknownHostException e) {
        }

        byte[] b = a1.getAddress();
        int ipInt = ((b[0] & 0xFF) << 24)
                | ((b[1] & 0xFF) << 16)
                | ((b[2] & 0xFF) << 8)
                | ((b[3] & 0xFF) << 0);

        byte[] b1 = a2.getAddress();
        int ipInt1 = ((b1[0] & 0xFF) << 24)
                | ((b1[1] & 0xFF) << 16)
                | ((b1[2] & 0xFF) << 8)
                | ((b1[3] & 0xFF) << 0);

        int mask = ~((1 << (32 - subnet1.get(subnet1.lastKey()))) - 1);

        if ((ipInt & mask) == (ipInt1 & mask)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Extract the IP and the mask given an IP address
     *
     * @param addr in String format (e.g. 192.168.1.0/24 or 192.168.1.50)
     * @return
     */
    public static TreeMap<String, Integer> extractIPandMask(String addr) {
        TreeMap<String, Integer> subnet = new TreeMap<String, Integer>();
        String[] parts = addr.split("/");
        String ip = parts[0];
        int mask;
        if (parts.length < 2) {
            mask = 0;
        } else {
            mask = Integer.parseInt(parts[1]);
        }
        subnet.put(ip, mask);
        return subnet;
    }

    /**
     * The string is a subnet address, contains the mask
     *
     * @param addr
     * @return The mask of the given ip subnet
     */
    public static int isSubnetAddress(String addr) {
        String[] parts = addr.split("/");

        if (parts.length < 2) {
            return 0;
        } else {
            return Integer.parseInt(parts[1]);
        }
    }

    /**
     * Mapping VRFRoute to Route used by SDN-OF module of OpenNaaS Layer3 to
     * layer2
     *
     * @param route
     * @return
     */
    public static NetworkConnection VRFRouteToNetCon(VRFRoute route) {
        NetworkConnection netCon = new NetworkConnection();
//        netCon.setId();//internal-id, nom del flow que guarda el floodlight
        netCon.setName(String.valueOf(route.getId()));//user friendly name
        Port port = new Port();
        port.setDeviceId(route.getSwitchInfo().getDPID());
        port.setPortNumber(String.valueOf(route.getSwitchInfo().getInputPort()));
        netCon.setSource(port);
        port.setDeviceId(route.getSwitchInfo().getDPID());
        port.setPortNumber(String.valueOf(route.getSwitchInfo().getOutputPort()));
        netCon.setDestination(port);
        return netCon;
    }

    /**
     * Mapping VRFRoute to Floodlight Flow used by SDN-OF module of OpenNaaS
     * Layer3 to layer2
     *
     * @param route
     * @param etherType
     * @return
     */
    public static FloodlightOFFlow VRFRouteToFloodlightFlow(VRFRoute route, String etherType) {
        FloodlightOFFlow flow = new FloodlightOFFlow();
        FloodlightOFMatch match = new FloodlightOFMatch();
        List<FloodlightOFAction> listActions = new ArrayList<FloodlightOFAction>();
        FloodlightOFAction action = new FloodlightOFAction();
        match.setSrcIp(route.getSourceAddress());
        match.setDstIp(route.getDestinationAddress());
        match.setEtherType(etherType);
//        match.setIngressPort(String.valueOf(route.getSwitchInfo().getInputPort()));

        action.setType(FloodlightOFAction.TYPE_OUTPUT);
        action.setValue(String.valueOf(route.getSwitchInfo().getOutputPort()));
        listActions.add(action);
        flow.setActions(listActions);
        flow.setActive(true);
        flow.setMatch(match);
//        flow.setName(String.valueOf(route.getId())+"-"+etherType+"-"+route.getSourceAddress()+"-"+route.getDestinationAddress());
        flow.setName(createFlowName(String.valueOf(route.getId()), etherType, route.getSourceAddress(), route.getDestinationAddress(), route.getSwitchInfo().getDPID()));
        flow.setSwitchId(route.getSwitchInfo().getDPID());

        return flow;
    }

    public static String createFlowName(String id, String ethType, String source, String target, String dpid) {
//        log.error("SETNAME FLOW: 0-" + ethType + "-" + source + "-" + target + "-" + dpid.substring(dpid.length() - 2));
//        return id+"-"+ethType+"-"+source+"-"+target+"-" + dpid.substring(dpid.length() - 2);
        source = source.replace("/", "-");
        target = target.replace("/", "-");
        dpid = dpid.substring(dpid.length() - 4).replace(":", "-");
        return "0-" + ethType + "-" + source + "-" + target + "-" + dpid;
    }

    /**
     * Copy InputStream to OutputStream (file).
     *
     * @param is
     * @param os
     * @throws IOException
     */
    public static void copyStream(InputStream is, OutputStream os) throws IOException {
        int BUFFER_SIZE = 16384;
        byte[] buf = new byte[BUFFER_SIZE];
        while (true) {
            int len = is.read(buf);
            if (len == -1) {
                return;
            }
            os.write(buf, 0, len);
        }
    }

    /**
     * To convert the InputStream to String we use the Reader.read(char[]
     * buffer) method. We iterate until the Reader return -1 which means there's
     * no more data to read. We use the StringWriter class to produce the
     * string.
     *
     * @param is
     * @return The string that contains the value of the inputstream
     * @throws IOException
     */
    public static String convertStreamToString(InputStream is) throws IOException {
        int BUFFER_SIZE = 1024;
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[BUFFER_SIZE];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }

    /**
     * Mapping VRFRoute to Floodlight Flow used by SDN-OF module of OpenNaaS
     * Layer3 to layer2
     *
     * @param route
     * @param etherType
     * @return
     */
    public static OFFlow VRFRouteToOFFlow(VRFRoute route, String etherType) {
        OFFlow flow = new OFFlow();

        FloodlightOFMatch match = new FloodlightOFMatch();
        List<FloodlightOFAction> listActions = new ArrayList<FloodlightOFAction>();
        FloodlightOFAction action = new FloodlightOFAction();
        match.setSrcIp(route.getSourceAddress());
        match.setDstIp(route.getDestinationAddress());
        match.setEtherType(etherType);
        match.setIngressPort(String.valueOf(route.getSwitchInfo().getInputPort()));

        action.setType(FloodlightOFAction.TYPE_OUTPUT);
        action.setValue(String.valueOf(route.getSwitchInfo().getOutputPort()));
        listActions.add(action);
        flow.setActions(listActions);
        flow.setActive(true);
        flow.setMatch(match);
//        flow.setName(String.valueOf(route.getId())+"-"+etherType+"-"+route.getSourceAddress()+"-"+route.getDestinationAddress());
        flow.setName(createFlowName(String.valueOf(route.getId()), etherType, route.getSourceAddress(), route.getDestinationAddress(), route.getSwitchInfo().getDPID()));
        flow.setDPID(route.getSwitchInfo().getDPID());
        return flow;
    }

    public static String intIPToString(String ip, int detectIPVersion) {
        if(detectIPVersion == 4)
            return Utils.intIPv4toString(Integer.parseInt(ip));
        else if(detectIPVersion == 6){
            return Utils.tryToCompressIPv6(ip);
        }
        return null;
    }
    
    public static String base64Encode(String stringToEncode) {
        return DatatypeConverter.printBase64Binary(stringToEncode.getBytes());
    }

    /**
     * Create a JSON string that contains the path.
     * Format: [{ip:''},{dpid:'00:00'},{ip:''}]
     * @param source
     * @param switchDPID
     * @param listOF
     * @param target 
     * @return  
     */
    public static StringBuilder createJSONPath(String source, String switchDPID, List<OFFlow> listOF, String target) {
        StringBuilder listFlows = new StringBuilder();
        listFlows.append("[");
        listFlows.append("{ip:'").append(source).append("'},");//source IP
        if(switchDPID != null){
            listFlows.append("{dpid:'").append(switchDPID).append("'},");//first switch id
        }
        for (int i = 0; i < listOF.size(); i++) {
            if (i == 0) {
                listFlows.append("{dpid:'");
                listFlows.append(listOF.get(i).getDPID());
                listFlows.append("'},");//others switch ids
            }
            for (int j = 0; j < i; j++) {
                if (!listFlows.toString().contains(listOF.get(i).getDPID())) {
                    listFlows.append("{dpid:'");
                    listFlows.append(listOF.get(i).getDPID());
                    listFlows.append("'},");//others switch ids
                }
            }

        }
        listFlows.append("{ip:'").append(target).append("'}]");//final destination
        return listFlows;
    }

    public static Response insertRoutesFromJSONFile(String content) {
        String response = "Inserted";
        List<VRFRoute> routes = new ArrayList<VRFRoute>();
        try {
            JsonFactory f = new MappingJsonFactory();
            JsonParser jp = f.createJsonParser(content);
            JsonToken current = jp.nextToken();
            if (current != JsonToken.START_OBJECT) {
                return Response.status(404).entity("Error: root should be object: quiting.").build();
            }
            while (jp.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = jp.getCurrentName();
                current = jp.nextToken();// move from field name to field value
                if (fieldName.equals("routeIPv4") || fieldName.equals("routeIPv6")) {
                    if (current == JsonToken.START_ARRAY) {
                        // For each of the records in the array
                        while (jp.nextToken() != JsonToken.END_ARRAY) {
                            // read the record into a tree model,
                            // this moves the parsing position to the end of it
                            JsonNode node = jp.readValueAsTree();
                            String field = jp.getCurrentName();
                            // And now we have random access to everything in the object
                            VRFRoute newRoute = new VRFRoute();
                            L2Forward newSwitch = new L2Forward();
                            newRoute.setSourceAddress(node.get("srcAddr").getValueAsText());
                            newRoute.setDestinationAddress(node.get("dstAddr").getValueAsText());
                            newSwitch.setInputPort(Integer.parseInt(node.get("swInfo").getPath("inPort").getValueAsText()));
                            newSwitch.setOutputPort(Integer.parseInt(node.get("swInfo").getPath("outPort").getValueAsText()));
                            newSwitch.setDPID(node.get("swInfo").getPath("dpid").getValueAsText());
                            newRoute.setSwitchInfo(newSwitch);
                            routes.add(newRoute);
                        }
                    } else {
                        response = "Error: records should be an array: skipping.";
                        jp.skipChildren();
                    }
                } else {
                    response = "Unprocessed property: " + fieldName;
                    jp.skipChildren();
                }
            }
            return Response.ok(routes).build();

        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Response.status(404).entity("Some error. Check the file. Possible error: " + response).build();
    }
}
