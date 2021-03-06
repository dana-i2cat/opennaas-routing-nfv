package org.opennaas.extensions.opendaylight.vtn.capability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opennaas.core.resources.protocol.ProtocolException;
import org.opennaas.core.resources.protocol.ProtocolSessionContext;
import org.opennaas.extensions.opendaylight.vtn.model.Boundary;
import org.opennaas.extensions.opendaylight.vtn.model.BoundaryMap;
import org.opennaas.extensions.opendaylight.vtn.model.Link;
import org.opennaas.extensions.opendaylight.vtn.model.OpenDaylightController;
import org.opennaas.extensions.opendaylight.vtn.model.OpenDaylightvBridge;
import org.opennaas.extensions.opendaylight.vtn.model.PortMap;
import org.opennaas.extensions.opendaylight.vtn.model.Switch;
import org.opennaas.extensions.opendaylight.vtn.model.VTN;
import org.opennaas.extensions.opendaylight.vtn.model.vBridgeInterfaces;
import org.opennaas.extensions.opendaylight.vtn.model.vLink;
import org.opennaas.extensions.opendaylight.vtn.protocol.OpenDaylightProtocolSession;
import org.opennaas.extensions.opendaylight.vtn.protocol.OpenDaylightProtocolSessionFactory;
import org.opennaas.extensions.opendaylight.vtn.protocol.client.IOpenDaylightvtnAPIClient;
import org.opennaas.extensions.opendaylight.vtn.protocol.client.wrappers.BoundaryWrapper;
import org.opennaas.extensions.opendaylight.vtn.protocol.client.wrappers.LogicalPortsOFFlowsWrapper;
import org.opennaas.extensions.opendaylight.vtn.protocol.client.wrappers.SwitchesWrapper;
import org.opennaas.extensions.opendaylight.vtn.protocol.client.wrappers.vBridgeInterfacesWrapper;
import org.opennaas.extensions.opendaylight.vtn.protocol.client.wrappers.vBridgesWrapper;
import org.opennaas.extensions.opendaylight.vtn.protocol.client.wrappers.vLinksWrapper;
//import org.opennaas.extensions.openflowswitch.utils.Utils;

/**
 *
 * @author Josep Batallé (josep.batalle@i2cat.net)
 *
 */
public class VTNCapability implements IVTNCapability {

    Log log = LogFactory.getLog(VTNCapability.class);
    ProtocolSessionContext context;
    OpenDaylightProtocolSession session;
    IOpenDaylightvtnAPIClient client;
    private static final String SESSION_ID = "0001";
//    private String PROTOCOL_URI = "http://192.168.254.72:8083/";
    private String PROTOCOL_URI = "http://84.88.40.153:8083/";
    private VTN vtn;
    private List<OpenDaylightController> controllers = new ArrayList<OpenDaylightController>();
    private List<Boundary> boundaries = new ArrayList<Boundary>();
    private Map<vBridgeInterfaces, String> mapPorts;

    public VTNCapability() {
        context = generateContext();

        try {
            session = (OpenDaylightProtocolSession) new OpenDaylightProtocolSessionFactory().createProtocolSession(SESSION_ID, context);
            session.connect();
            client = session.getOpenDaylightClientForUse();
        } catch (ProtocolException ex) {
            Logger.getLogger(VTNCapability.class.getName()).log(Level.SEVERE, null, ex);
        }
        initConfig();
    }

    private ProtocolSessionContext generateContext() {
        context = new ProtocolSessionContext();
        context.addParameter(ProtocolSessionContext.PROTOCOL, OpenDaylightProtocolSession.OPENDAYLIGHT_PROTOCOL_TYPE);
        context.addParameter(ProtocolSessionContext.PROTOCOL_URI, PROTOCOL_URI);
        context.addParameter(ProtocolSessionContext.AUTH_TYPE, "noauth");
        return context;
    }

    private void initConfig() {
        vtn = new VTN("vtn1");
        Response response = createVTN(vtn.getVtn_name());
        if (response.getEntity().equals("VTN is not accessible")) {
            log.error("VTN is not accesible, change the IP address");
            return;
        }

//        String ctrl1_IP = "192.168.254.156";
//        String ctrl2_IP = "192.168.254.221";
        String ctrl1_IP = "84.88.36.100";
        String ctrl2_IP = "84.88.41.171";
        createController("ctrl1", ctrl1_IP, "odc", "floodlight");
        createController("ctrl2", ctrl2_IP, "odc", "opendaylight");
        OpenDaylightController ctrl1 = new OpenDaylightController("ctrl1", ctrl1_IP, "odc", "1.0", "enable", "floodlight");
        controllers.add(ctrl1);
        ctrl1 = new OpenDaylightController("ctrl2", ctrl2_IP, "odc", "1.0", "enable", "");
        controllers.add(ctrl1);

        log.error("Register Bridges");
        vBridgesWrapper vbrs = getvBridges(vtn.getVtn_name());//get list
        if (vbrs.size() > 0) {
            for (OpenDaylightvBridge vbr : vbrs) {
                vtn.getvBridges().add(getvBridge(vtn.getVtn_name(), vbr.getVbr_name()));
            }
        } else {
            log.error("Creating vBridges");
            createvBridge(vtn.getVtn_name(), "vbr1", controllers.get(0).getController_id(), "DEFAULT");
            createvBridge(vtn.getVtn_name(), "vbr2", controllers.get(1).getController_id(), "DEFAULT");
        }
        log.error("Register interfaces...");
        updateInterfaces();

        log.error("Creating boundary");
        BoundaryWrapper bds = getBoundaries();//get list
        if (bds.size() > 0) {
            for (Boundary bound : bds) {
                boundaries.add(getBoundary(bound.getBoundary_id()));
            }
        } else {
            String borderPort1 = "PP-OF:00:00:64:87:88:58:f8:57-ge-1/0/3.0";
            String borderPort2 = "PP-OF:00:00:00:00:00:00:00:03-eth1";
            createBoundary("b1", controllers.get(0).getController_id(), "DEFAULT", borderPort1, controllers.get(1).getController_id(), "DEFAULT", borderPort2);
        }

        log.error("Creating vLinks");
        vLinksWrapper vLinks = getvLinks(vtn.getVtn_name());//get list
        if (vLinks.size() > 0) {
            for (vLink vlk : vLinks) {
                vtn.getVlink().add(getvLink(vtn.getVtn_name(), vlk.getVlk_name()));
            }
        } else {
            createvLink(vtn.getVtn_name(), "vlink1", "vbr1", "if1", "vbr2", "if1", "b1", "50");
        }

        log.error("------------- VTN summary -------------");
        log.error("List of vbr (" + vtn.getvBridges().size() + ")");
        for (OpenDaylightvBridge vbr : vtn.getvBridges()) {
            log.error("In vBridge " + vbr.getVbr_name() + ". List of ifaces (" + vbr.getIface().size() + ")");
        }

        log.error("Number of vLinks: " + vtn.getVlink().size());
        if (vtn.getVlink().size() > 0) {
            log.error("vLink: " + vtn.getVlink().get(0).getVlk_name() + " " + vtn.getVlink().get(0).getVnode1_name());
            log.error("vLinkCont.: " + vtn.getVlink().get(0).getVnode2_name() + "  " + vtn.getVlink().get(0).getBoundaryMap().getBoundary_id());
        }
        log.error("Assign each iface to a physical port...");
        checkPortMap();
    }

    @Override
    public Response createVTN(String name) {
        log.info("Create VTN " + name);
        vtn = new VTN(name);
        Response response;
        try {
            response = client.createVTN(vtn);
        } catch (Exception e) {
            response = Response.ok("VTN is not accessible").build();
        }
        return response;
    }

    @Override
    public Response removeVTN(String name) {
        return client.deleteVTN(name);
    }

    @Override
    public Response coordinatorAddress(String address, String port) {
        PROTOCOL_URI = "http://" + address + ":" + port;
        generateContext();
        return Response.ok().build();
    }

    @Override
    public Response createController(String name, String ipaddr, String type, String description) {
        OpenDaylightController ctrl = new OpenDaylightController(name, ipaddr, type, "1.0", "enable", description);
        Response response = client.createController(ctrl);
        log.error("Response "+response.getStatus());
        if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
            controllers.add(ctrl);
        }
        return response;
    }

    @Override
    public Response createvBridge(String vtn_name, String name, String ctrl, String domain) {
        OpenDaylightvBridge vBridge = new OpenDaylightvBridge(name, ctrl, domain);
        Response response = client.createvBridge(vtn_name, vBridge);
        log.error("Response "+response.getStatus());
        if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
            vtn.getvBridges().add(vBridge);
        }
        return response;
    }

    @Override
    public Response createInterfaces(String vtn_name, String vBridge, String iface) {
        vBridgeInterfaces inf = new vBridgeInterfaces(iface);
        Response response = client.createInterfaces(vtn_name, vBridge, inf);
        log.error("Create iface?: " + response.getStatus());
        if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
            for (OpenDaylightvBridge vbr : vtn.getvBridges()) {
                if (vbr.getVbr_name().equals(vBridge)) {
                    vbr.getIface().add(inf);
                }
            }
        }
        return response;
    }

    @Override
    public Response createBoundary(String id, String ctrl1, String domain1, String port1, String ctrl2, String domain2, String port2) {
        Boundary bound = new Boundary(id, new Link(ctrl1, domain1, port1, ctrl2, domain2, port2));
        Response response = client.createBoundary(bound);
        if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
            boundaries.add(bound);
        }
        return response;
    }

    @Override
    public Response createvLink(String vtnName, String vlinkName, String vnode1, String if1, String vnode2, String if2, String boundId, String vlanId) {
        vLink vlink = new vLink(vlinkName, vnode1, if1, vnode2, if2, new BoundaryMap(boundId, vlanId));
        Response response = client.createvLink(vtnName, vlink);
        if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
            if (!vtn.getVlink().contains(vlink)) {
                vtn.getVlink().add(vlink);
            }
        }
        return response;
    }

    @Override
    public Response mapPort(String vtnName, String vBridge, String iface, String port) {
        PortMap portMap = new PortMap();
        portMap.setLogical_port_id(port);
        return client.configPortMap(vtnName, vBridge, iface, portMap);
    }

    @Override
    public Response ipreq(String DPID, String Port) {
        log.error("Requested route in VRF. Trying to map the ODL ports.");
        log.error("Req. information: " + DPID + " mapPort: " + Port);
        Response response;

        if (vtn == null) {
            return Response.status(500).entity("VTN no exists. Please create a VTN").build();
        }
        if (Port == null) {
            return Response.accepted("Error. Destination port is null").build();
        }
        String dstSw_num = DPID.substring(DPID.length() - 1);
        String outPort = "PP-OF:" + DPID + "-s" + dstSw_num + "-eth" + Port;
//        port = "PP-OF:00:00:00:00:00:00:00:05-s5-eth2";
        String iface = "if2";
        String vbrName = getvBridgeOfSwitch(DPID);
        for (vBridgeInterfaces inf : vtn.getvBridge(vbrName).getIface()) {
//            client.configPortMap(vtn.getVtn_name(), vbrName, inf.getIf_name());
            for (PortMap pm : inf.getPortMaps()) {
                if (pm.getLogical_port_id() != null) {
                    if (pm.getLogical_port_id().equals(outPort)) {
                        iface = inf.getIf_name();
                        log.error("Set Iface: " + iface);
                        break;
                    }
                }
            }
        }
        if (vtn.getvBridge(vbrName).getIface().size() == 1) {
            createInterfaces(vtn.getVtn_name(), vbrName, iface);
        }
        log.error("iface: " + iface + " Port: " + outPort);
        response = mapPort(vtn.getVtn_name(), vbrName, iface, outPort);
        log.error("HTTP Code of ODL VTN - portMap: " + response.getStatus());

        return response;
    }

    @Override
    public PortMap mapPort(String vtnName, String vBridge, String iface) {
        return client.configPortMap(vtnName, vBridge, iface);
    }

    @Override
    public LogicalPortsOFFlowsWrapper getLogicalPorts(String ctrl, String domain) {
        log.error("Get Logical Ports of ctrl " + ctrl + "and domain " + domain);
        LogicalPortsOFFlowsWrapper lp = client.getLogicalPorts(ctrl, "(" + domain + ")");
        return lp;
    }

    /**
     * Analyze the vbr of OpenNaaS and assign the mapping(port-interface) of
     * each interface that contains each vbridge
     */
    @Override
    public void checkPortMap() {
        log.error("Assing Port-Map Interface of each vBridge");
        //for each vBridge and each interface check the mapping port
        PortMap pMap;
        for (OpenDaylightvBridge vbr : vtn.getvBridges()) {
            for (vBridgeInterfaces inf : vbr.getIface()) {
                pMap = client.configPortMap(vtn.getVtn_name(), vbr.getVbr_name(), inf.getIf_name());
                inf.getPortMaps().add(pMap);
                log.error("Iface " + inf.getIf_name() + " from " + vbr.getVbr_name() + ". AddingMap " + pMap.getLogical_port_id());
            }
        }
    }

    @Override
    public OpenDaylightvBridge getvBridge(String vtnName, String vBridge) {
        OpenDaylightvBridge vbr = client.getvBridge(vtnName, vBridge);
        return vbr;
    }

    @Override
    public vBridgesWrapper getvBridges(String vtnName) {
        vBridgesWrapper vbrs = client.getvBridges("vtn1");
        return vbrs;
    }

    @Override
    public vBridgeInterfacesWrapper getInterfaces(String vtnName, String vBridge) {
        vBridgeInterfacesWrapper ifaces = client.getInterfaces(vtnName, vBridge);
        return ifaces;
    }

    @Override
    public Boundary getBoundary(String bound) {
        return client.getBoundary(bound);
    }

    @Override
    public BoundaryWrapper getBoundaries() {
        return client.getBoundaries();
    }

    @Override
    public vLink getvLink(String vtnName, String bound) {
        return client.getvLink(vtnName, bound);
    }

    @Override
    public vLinksWrapper getvLinks(String vtnName) {
        return client.getvLinks(vtnName);
    }

    private int getNumInts(String controller_id) {
        LogicalPortsOFFlowsWrapper resp = getLogicalPorts(controller_id, "DEFAULT");
        log.error(resp.size());
        return resp.size();
    }

    @Override
    public void updateInterfaces() {
        vBridgeInterfacesWrapper ifaces;
        for (OpenDaylightvBridge vbr : vtn.getvBridges()) {
            ifaces = getInterfaces(vtn.getVtn_name(), vbr.getVbr_name());
            log.error("Get Interfaces. Ifaces bridge "+vbr.getVbr_name()+": " + ifaces.size());
            if (ifaces.size() > 0) {
                for (vBridgeInterfaces iface : ifaces) {
                    vbr.getIface().add(iface);
                }
            } else {
                log.error("Creating interfaces of vBridge " + vbr.getVbr_name());
//                List<vBridgeInterfaces> ints = getPossibleInts(vbr.getVbr_name(), vbr.getController_id());
                if (getODLController(vbr.getController_id()).getDescription().equals("floodlight")) {
                    createInterfaces(vtn.getVtn_name(), vbr.getVbr_name(), "if1");
                } else {
                    int numInt = getNumInts(vbr.getController_id());
                    createInterfaces(vtn.getVtn_name(), vbr.getVbr_name(), "if1");
                    log.error("Number of possible interfaces of vBridge " + vbr.getVbr_name());
                    for (int i = 1; i < numInt; i++) {
                        String ifName = "if" + (i + 1);
                        createInterfaces(vtn.getVtn_name(), vbr.getVbr_name(), ifName);
                    }
                }
            }
        }
    }

    @Override
    public void update() {
        vtn = new VTN("vtn1");
        controllers = new ArrayList<OpenDaylightController>();
        boundaries = new ArrayList<Boundary>();
        initConfig();
    }

    @Override
    public void cleanVTN() {
        client.deleteVTN("vtn1");
        client.deleteBoundary("b1");
    }

    private OpenDaylightController getODLController(String controller_id) {
        for (OpenDaylightController ctrl : controllers) {
            if (ctrl.getController_id().equals(controller_id)) {
                return ctrl;
            }
        }
        return null;
    }

    private String getvBridgeOfSwitch(String DPID) {
        String vbrName = "vbr2";
        for (OpenDaylightvBridge vB : vtn.getvBridges()) {
            SwitchesWrapper listSw = client.getListSwitchs(vB.getController_id());
            for (Switch sw : listSw) {
                if (sw.getSwitch_id().equals(DPID)) {
                    return vB.getVbr_name();
                }
            }
        }
        return vbrName;
    }
}
