package net.floodlightcontroller.topology.web;

import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.NodePortTuple;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import java.util.Set;

public class BlockedPortsResource extends ServerResource {
    @Get("json") public Set<NodePortTuple> retrieve() {
        ITopologyService topology = (ITopologyService) getContext()
                .getAttributes().
                        get(ITopologyService.class.getCanonicalName());

        return topology.getBlockedPorts();
    }
}
