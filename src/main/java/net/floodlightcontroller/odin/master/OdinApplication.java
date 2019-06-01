package net.floodlightcontroller.odin.master;

import net.floodlightcontroller.odin.master.OdinMaster.ChannelAssignmentParams;
import net.floodlightcontroller.odin.master.OdinMaster.MobilityParams;
import net.floodlightcontroller.odin.master.OdinMaster.ScannParams;
import net.floodlightcontroller.odin.master.OdinMaster.SmartApSelectionParams;
import net.floodlightcontroller.util.MACAddress;

import java.net.InetAddress;
import java.util.Map;
import java.util.Set;


/**
 * Base class for all Odin applications. They are expected to run as a thread provided by the
 * master. *
 *
 * @author Lalith Suresh <suresh.lalith@gmail.com>
 */
public abstract class OdinApplication implements Runnable {

    private IOdinMasterToApplicationInterface odinApplicationInterfaceToMaster;
    private String pool;

    private State state;
    private Object lock;


    /**
     * Set the OdinMaster to use
     */
    final void setOdinInterface(IOdinMasterToApplicationInterface om) {
        odinApplicationInterfaceToMaster = om;
    }


    /**
     * Sets the pool to use for the application
     */
    final void setPool(String pool) {
        this.pool = pool;
    }


    /**
     * Gets the application state (RUNNING, HALTING, HALTED)
     *
     * @return the application state
     * @author André Oliveira <andreduartecoliveira@gmail.com>
     */
    protected State getState() {
        return state;
    }


    /**
     * Sets the application state (RUNNING, HALTING, HALTED)
     *
     * @author André Oliveira <andreduartecoliveira@gmail.com>
     */
    protected void setState(State state) {
        this.state = state;
    }


    /**
     * Gets the application lock (for synchronization purposes)
     *
     * @return the application lock
     * @author André Oliveira <andreduartecoliveira@gmail.com>
     */
    protected Object getLock() {
        return lock;
    }


    /**
     * Sets the application lock (for synchronization purposes)
     *
     * @param lock the application lock
     * @author André Oliveira <andreduartecoliveira@gmail.com>
     */
    protected void setLock(Object lock) {
        this.lock = lock;
    }


    /**
     * Needed to wrap OdinApplications into a thread, and is implemented by the specific
     * application
     */
    public abstract void run();


    /**
     * VAP-Handoff a client to a new AP. This operation is idempotent.
     *
     * @param staHwAddr Ethernet address of STA to be handed off
     * @param newApIpAddr IPv4 address of new access point
     */
    protected final void handoffClientToAp(MACAddress staHwAddr, InetAddress newApIpAddr) {
        odinApplicationInterfaceToMaster.handoffClientToAp(pool, staHwAddr, newApIpAddr);
    }


    /**
     * Get the list of clients currently registered with Odin
     *
     * @return a map of OdinClient objects keyed by HW Addresses
     */
    protected final Set<OdinClient> getClients() {
        return odinApplicationInterfaceToMaster.getClients(pool);
    }


    /**
     * Get the OdinClient type from the client's MACAddress
     *
     * @return a OdinClient instance corresponding to clientHwAddress
     */
    protected final OdinClient getClientFromHwAddress(MACAddress clientHwAddress) {
        return odinApplicationInterfaceToMaster.getClientFromHwAddress(pool, clientHwAddress);
    }

    /**
     * Retreive LastHeard from the agent
     *
     * @param agentAddr InetAddress of the agent
     * @return timestamp of the last ping heard from the agent
     */
    protected final long getLastHeardFromAgent(InetAddress agentAddr) {
        return odinApplicationInterfaceToMaster.getLastHeardFromAgent(pool, agentAddr);
    }

    /**
     * Retreive TxStats from the agent
     *
     * @param agentAddr InetAddress of the agent
     * @return Key-Value entries of each recorded statistic for each client
     */
    protected final Map<MACAddress, Map<String, String>> getTxStatsFromAgent(
            InetAddress agentAddr) {
        return odinApplicationInterfaceToMaster.getTxStatsFromAgent(pool, agentAddr);
    }

    /**
     * Retreive RxStats from the agent
     *
     * @param agentAddr InetAddress of the agent
     * @return Key-Value entries of each recorded statistic for each client
     */
    protected final Map<MACAddress, Map<String, String>> getRxStatsFromAgent(
            InetAddress agentAddr) {
        return odinApplicationInterfaceToMaster.getRxStatsFromAgent(pool, agentAddr);
    }


    /**
     * Request scanned stations statistics from the agent
     *
     * @param agentAddr InetAddress of the agent
     * @param channel to scan
     * @param ssid to scan (always is *)
     * @return If request is accepted return 1, otherwise, return 0
     */
    protected final int requestScannedStationsStatsFromAgent(InetAddress agentAddr, int channel,
            String ssid) {
        return odinApplicationInterfaceToMaster
                .requestScannedStationsStatsFromAgent(pool, agentAddr, channel, ssid);
    }


    /**
     * Retreive scanned stations statistics from the agent
     *
     * @param agentAddr InetAddress of the agent
     * @return Key-Value entries of each recorded statistic for each station
     */
    protected final Map<MACAddress, Map<String, String>> getScannedStationsStatsFromAgent(
            InetAddress agentAddr, String ssid) {
        return odinApplicationInterfaceToMaster
                .getScannedStationsStatsFromAgent(pool, agentAddr, ssid);
    }


    /**
     * Request scanned stations statistics from the agent
     *
     * @param agentAddr InetAddress of the agent
     * @param channel to send mesurement beacon
     * @param ssid to scan (e.g odin_init)
     * @return If request is accepted return 1, otherwise, return 0
     */
    protected final int requestSendMesurementBeaconFromAgent(InetAddress agentAddr, int channel,
            String ssid) {
        return odinApplicationInterfaceToMaster
                .requestSendMesurementBeaconFromAgent(pool, agentAddr, channel, ssid);
    }


    /**
     * Stop sending mesurement beacon from the agent
     *
     * @param agentAddr InetAddress of the agent
     * @return if the method reaches the end, returns 1
     */
    protected final int stopSendMesurementBeaconFromAgent(InetAddress agentAddr) {
        return odinApplicationInterfaceToMaster.stopSendMesurementBeaconFromAgent(pool, agentAddr);
    }


    /**
     * Get a list of Odin agents from the agent tracker
     *
     * @return a map of OdinAgent objects keyed by Ipv4 addresses
     */
    protected final Set<InetAddress> getAgents() {
        return odinApplicationInterfaceToMaster.getAgentAddrs(pool);
    }


    /**
     * Add a subscription for a particular event defined by oes. cb is defines the application
     * specified callback to be invoked during notification. If the application plans to delete the
     * subscription, later, the onus is upon it to keep track of the subscription id for removal
     * later.
     *
     * @param oes the susbcription
     * @param cb the callback
     */
    protected final long registerSubscription(OdinEventSubscription oes, NotificationCallback cb) {
        return odinApplicationInterfaceToMaster.registerSubscription(pool, oes, cb);
    }


    /**
     * Remove a subscription from the list
     *
     * @param id subscription id to remove
     */
    protected final void unregisterSubscription(long id) {
        odinApplicationInterfaceToMaster.unregisterSubscription(pool, id);
    }


    /**
     * Add a flow detection for a particular event defined by oefd. cb is defines the application
     * specified callback to be invoked during flow detection. If the application plans to delete
     * the flow detection, later, the onus is upon it to keep track of the flow detection id for
     * removal later.
     *
     * @param oefd the flow detection
     * @param cb the callback
     */
    protected final long registerFlowDetection(OdinEventFlowDetection oefd,
            FlowDetectionCallback cb) {
        return odinApplicationInterfaceToMaster.registerFlowDetection(pool, oefd, cb);
    }


    /**
     * Remove a flow detection from the list
     *
     * @param id flow detection id to remove
     */
    protected final void unregisterFlowDetectionn(long id) {
        odinApplicationInterfaceToMaster.unregisterFlowDetection(pool, id);
    }


    /**
     * Add an SSID to the Odin network.
     *
     * @param ssid Name of the network
     * @return true if the network could be added, false otherwise
     */
    protected final boolean addNetwork(String ssid) {
        return odinApplicationInterfaceToMaster.addNetwork(pool, ssid);
    }


    /**
     * Remove an SSID from the Odin network.
     *
     * @param ssid Name of the network
     * @return true if the network could be removed, false otherwise
     */
    protected final boolean removeNetwork(String ssid) {
        return odinApplicationInterfaceToMaster.removeNetwork(pool, ssid);
    }


    /**
     * Change the Wi-Fi channel of an specific agent (AP)
     *
     * @param agentAddr InetAddress of the agent
     * @param channel Channel to set
     */
    protected final void setChannelToAgent(InetAddress agentAddr, int channel) {
        odinApplicationInterfaceToMaster.setChannelToAgent(pool, agentAddr, channel);
    }


    /**
     * Get channel from a specific agent (AP)
     *
     * @param agentAddr InetAddress of the agent
     * @return Channel number
     */
    protected final int getChannelFromAgent(InetAddress agentAddr) {
        return odinApplicationInterfaceToMaster.getChannelFromAgent(pool, agentAddr);
    }

//	/**
//	 * Channel Switch Announcement, to the clients of an specific agent (AP)
//	 *
//	 * @param Agent InetAddress
//	 * @param Client MAC
//	 * @param SSID
//	 * @param Channel
//	 */
//	protected final void sendChannelSwitchToClient (InetAddress agentAddr, MACAddress clientHwAddr, String ssid, int channel){
//		odinApplicationInterfaceToMaster.sendChannelSwitchToClient(pool, agentAddr, clientHwAddr, ssid, channel);
//	}


    /**
     * Scanning for a client in a specific agent (AP)
     *
     * @param agentAddr InetAddress of the agent
     * @param clientHwAddr MACAddress of the client (Sta)
     * @param time scanning time
     * @return Signal power
     */
    protected final int scanClientFromAgent(InetAddress agentAddr, MACAddress clientHwAddr,
            int channel, int time) {
        return odinApplicationInterfaceToMaster
                .scanClientFromAgent(pool, agentAddr, clientHwAddr, channel, time);
    }


    protected final MobilityParams getMobilityParams() {
        return odinApplicationInterfaceToMaster.getMobilityParams();
    }


    protected final ScannParams getMatrixParams() {
        return odinApplicationInterfaceToMaster.getMatrixParams();
    }


    protected final ScannParams getInterferenceParams() {
        return odinApplicationInterfaceToMaster.getInterferenceParams();
    }


    protected final ChannelAssignmentParams getChannelAssignmentParams() {
        return odinApplicationInterfaceToMaster.getChannelAssignmentParams();
    }


    protected final SmartApSelectionParams getSmartApSelectionParams() {
        return odinApplicationInterfaceToMaster.getSmartApSelectionParams();
    }

    /**
     * Get TxPower from and specific agent (AP)
     *
     * @param agentAddr InetAddress of the agent
     * @return TxPower in dBm
     */
    protected final int getTxPowerFromAgent(InetAddress agentAddr) {
        return odinApplicationInterfaceToMaster.getTxPowerFromAgent(pool, agentAddr);
    }


    /**
     * Retrieve scanned wi5 stations rssi from the agent
     *
     * @param agentAddr InetAddress of the agent
     * @return Key-Value entries of each recorded RSSI for each wi5 station
     */
    protected final String getScannedStaRssiFromAgent(InetAddress agentAddr) {
        return odinApplicationInterfaceToMaster.getScannedStaRssiFromAgent(pool, agentAddr);
    }


    /**
     * Retrieve associated wi5 stations in the agent
     *
     * @param agentAddr InetAddress of the agent
     * @return Set of OdinClient associated in the agent
     */
    protected final Set<OdinClient> getClientsFromAgent(InetAddress agentAddr) {
        return odinApplicationInterfaceToMaster.getClientsFromAgent(pool, agentAddr);
    }


    /**
     * Return Vip AP IP address
     *
     * @return vipAPIpaddress
     */
    protected final String getVipAPIpAddress() {
        return odinApplicationInterfaceToMaster.getVipAPIpAddress();
    }


    /**
     * Retrieve the historical RSSI value stored at the agent, for a certain station
     *
     * @param staHwAddr Ethernet address of the client (Sta)
     * @param agentAddr InetAddress of the agent
     * @return historical RSSI value
     * @author André Oliveira <andreduartecoliveira@gmail.com>
     */
    protected final Double getStaWeightedRssiFromAgent(MACAddress staHwAddr,
            InetAddress agentAddr) {
        return odinApplicationInterfaceToMaster.getStaWeightedRssiFromAgent(staHwAddr, agentAddr);
    }


    /**
     * Gets the target application's state (RUNNING, HALTING, HALTED)
     *
     * @param applicationName Name of the target application
     * @return the application state
     * @author André Oliveira <andreduartecoliveira@gmail.com>
     */
    protected State getApplicationState(String applicationName) {
        return odinApplicationInterfaceToMaster.getApplicationState(applicationName);
    }


    /**
     * Sets a target application's state (RUNNING, HALTING, HALTED)
     *
     * @param applicationName Name of the target application
     * @param state State to set the application to (RUNNING, HALTING, HALTED)
     * @return true if application exists, false otherwise
     * @author André Oliveira <andreduartecoliveira@gmail.com>
     */
    protected final boolean setApplicationState(String applicationName, State state) {
        return odinApplicationInterfaceToMaster.setApplicationState(applicationName, state);
    }


    /**
     * Describes the state of an application as RUNNING, HALTING or HALTED
     *
     * @author André Oliveira <andreduartecoliveira@gmail.com>
     */
    protected enum State {
        RUNNING, HALTING, HALTED
    }
}
