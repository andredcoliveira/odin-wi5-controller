package net.floodlightcontroller.topology;

import java.util.Set;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface ITopologyService extends IFloodlightService  {
    /**
     * Query to determine if the specified switch id and port are
     * connected to another switch or not.  If so, this means the link
     * is passing LLDPs properly between two OpenFlow switches.
     * @param switchid
     * @param port
     * @return
     */
    public boolean isInternal(long switchid, short port);

    /**
     * Returns the cluster ID of a given switch.
     * @param switchId The DPID of the switch in long form
     * @return The DPID of the switch that is the key for the cluster
     */
    public long getSwitchClusterId(long switchId);

    /**
     * Retrieves a set of all the switches in the same cluster as sw.
     * A cluster is a set of switches that are directly or indirectly
     * connected via Openflow switches that use the same controller
     * (not necessarily the same controller instance but any controller
     * instance in a group sharing the same network database).
     * @param sw The switch whose cluster we're obtaining
     * @return Set of switches in the cluster
     */
    public Set<Long> getSwitchesInCluster(long switchId);

    /**
     * Queries whether two switches are in the same cluster.
     * @param switch1 the DPID of the first switch
     * @param switch2 the DPID of the second switch
     * @return true if the switches are in the same cluster
     */
    public boolean inSameCluster(long switch1, long switch2);

    /**
     * Queries whether two switches are in the same island.
     * Currently, island and cluster are the same. In future,
     * islands could be different than clusters.
     * @param switch1
     * @param switch2
     * @return True of they are in the same island, false otherwise
     */
    public boolean inSameIsland(long switch1, long switch2);

    /**
     * Adds a listener to get topology notifications
     * @param listener The module that wants to listen for events
     */
    public void addListener(ITopologyListener listener);

    /**
     * Gets the set of ports that belong to a broadcast domain.
     * @return The set of ports that belong to a broadcast domain.
     */
    public Set<NodePortTuple> getBroadcastDomainLinks();
    
    /**
     * Returns that set of links that are tunnel links.
     * @return The set of links that are tunnel links.
     */
    public Set<NodePortTuple> getTunnelLinks();
    
    public boolean isBroadcastDomainPort(long sw, short port);

    public boolean isAllowed(long sw, short portId);

    public boolean isConsistent(long oldSw, short oldPort, long newSw,
                                short newPort);
    /**
     * If the dst is not allowed by the higher-level topology, this method provides
     * the topologically equivalent broadcast port.  
     * @param src
     * @param dst
     * @return the allowed broadcast port
     */
    public NodePortTuple getAllowedOutgoingBroadcastPort(long src,
                                                         short srcPort,
                                                         long dst,
                                                         short dstPort);
    
    /**
     * If the src broadcast domain port is not allowed for incoming broadcast, 
     * this method provides the topologically equivalent incoming broadcast-allowed
     * src port.  
     * @param src
     * @param dst
     * @return the allowed broadcast port
     */
    public NodePortTuple getAllowedIncomingBroadcastPort(long src,
                                                         short srcPort);

    public boolean isIncomingBroadcastAllowed(long sw, short portId);

    public boolean isInSameBroadcastDomain(long s1, short p1, long s2, short p2);

    /**
     * Gets a list of ports on a given switch
     * @param sw The switch DPID in long
     * @return The set of ports on this switch
     */
    public Set<Short> getPorts(long sw);

    public Set<Short> getBroadcastPorts(long targetSw, long src, short srcPort);

    public NodePortTuple getOutgoingSwitchPort(long src, short srcPort,
                                               long dst, short dstPort);

    public NodePortTuple getIncomingSwitchPort(long src, short srcPort,
                                               long dst, short dstPort);
}
