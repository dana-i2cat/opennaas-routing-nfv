package org.opennaas.extensions.opendaylight.vtn.protocol.client.mockup;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.opennaas.extensions.opendaylight.vtn.model.Boundary;
import org.opennaas.extensions.opendaylight.vtn.model.OpenDaylightController;
import org.opennaas.extensions.opendaylight.vtn.model.OpenDaylightvBridge;
import org.opennaas.extensions.opendaylight.vtn.model.PortMap;
import org.opennaas.extensions.opendaylight.vtn.model.VTN;
import org.opennaas.extensions.opendaylight.vtn.model.vBridgeInterfaces;
import org.opennaas.extensions.opendaylight.vtn.model.vLink;
import org.opennaas.extensions.opendaylight.vtn.protocol.client.IOpenDaylightvtnAPIClient;
import org.opennaas.extensions.opendaylight.vtn.protocol.client.wrappers.BoundaryWrapper;
import org.opennaas.extensions.opendaylight.vtn.protocol.client.wrappers.LogicalPortsOFFlowsWrapper;
import org.opennaas.extensions.opendaylight.vtn.protocol.client.wrappers.SwitchesWrapper;
import org.opennaas.extensions.opendaylight.vtn.protocol.client.wrappers.vBridgeInterfacesWrapper;
import org.opennaas.extensions.opendaylight.vtn.protocol.client.wrappers.vBridgesWrapper;
import org.opennaas.extensions.opendaylight.vtn.protocol.client.wrappers.vLinksWrapper;

/**
 *
 * @author Josep Batallé (i2CAT)
 *
 */
public class OpenDaylightMockClient implements IOpenDaylightvtnAPIClient {

    private List<VTN> vtns;
    private Set<OpenDaylightController> controllers;
    private Set<OpenDaylightvBridge> vBridges;
    private Set<vBridgeInterfaces> vInterfaces;
    private Set<Boundary> boundaries;
    private Set<vLink> vLinks;

    public OpenDaylightMockClient() {
        this.controllers = new HashSet<OpenDaylightController>();
        this.vBridges = new HashSet<OpenDaylightvBridge>();
        this.vInterfaces = new HashSet<vBridgeInterfaces>();
        this.boundaries = new HashSet<Boundary>();
        this.vLinks = new HashSet<vLink>();
    }

    @Override
    public Response createVTN(VTN vtn) {
        vtns.add(vtn);
        return Response.ok().build();
    }

    @Override
    public Response createController(OpenDaylightController controller) {
        controllers.add(controller);
        return Response.ok().build();
    }

    @Override
    public Response createvBridge(String vtn, OpenDaylightvBridge vBridge) {
        vBridges.add(vBridge);
        return Response.ok().build();
    }

    @Override
    public Response deleteVTN(String name) {
        for (int i = 0; i<vtns.size(); i++){
            if(vtns.get(i).getVtn_name().equals(name))
                vtns.remove(i);
        }
        return Response.ok().build();
    }

    @Override
    public Response createInterfaces(String vtn_name, String vbr_name, vBridgeInterfaces iface) {
        vInterfaces.add(iface);
        return Response.ok().build();
    }

    @Override
    public Response createBoundary(Boundary bound) {
        boundaries.add(bound);
        return Response.ok().build();
    }

    @Override
    public Response createvLink(String vtn_name, vLink vlink) {
        vLinks.add(vlink);
        return Response.ok().build();
    }

    @Override
    public Response configPortMap(String vtn_name, String vbr_name, String if_name, PortMap portMap) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PortMap configPortMap(String vtnName, String vtnName0, String iface) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LogicalPortsOFFlowsWrapper getLogicalPorts(String ctrl, String domain) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public OpenDaylightvBridge getvBridge(String vtn, String vbr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public vBridgesWrapper getvBridges(String vtn) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public vBridgeInterfacesWrapper getInterfaces(String vtn, String vbr_name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Boundary getBoundary(String bound) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public BoundaryWrapper getBoundaries() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public vLink getvLink(String vtn, String vlink) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public vLinksWrapper getvLinks(String vtnName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Response deletevBridge(String vtn, String vbr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Response deleteInterfaces(String vtn, String vbr_name, String iface) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Response deleteBoundary(String bound) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Response deletevLink(String vtn, String vlink) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SwitchesWrapper getListSwitchs(String ctrl_id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
