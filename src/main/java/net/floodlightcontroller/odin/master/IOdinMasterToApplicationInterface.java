package net.floodlightcontroller.odin.master;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.odin.master.OdinApplication.State;
import net.floodlightcontroller.odin.master.OdinClient;
import net.floodlightcontroller.odin.master.OdinMaster.MobilityParams;
import net.floodlightcontroller.odin.master.OdinMaster.ScannParams;
import net.floodlightcontroller.odin.master.OdinMaster.ChannelAssignmentParams;
import net.floodlightcontroller.odin.master.OdinMaster.SmartApSelectionParams;
import net.floodlightcontroller.util.MACAddress;

interface IOdinMasterToApplicationInterface {

    /**
     * VAP-Handoff a client to a new AP. This operation is idempotent.
     *
     * @param pool Pool that the invoking application corresponds to
     * @param staHwAddr MAC address of the client
     * @param newApIpAddr InetAddress of the future agent
     */
    void handoffClientToAp(String pool, MACAddress staHwAddr, InetAddress newApIpAddr);


    /**
     * @param pool Pool that the invoking application corresponds to
     * @return a map of OdinClient objects keyed by HW Addresses
     */
    Set<OdinClient> getClients(String pool);


    /**
     * Get the OdinClient type from the client's MACAddress
     *
     * @param pool Pool that the invoking application corresponds to
     * @param clientHwAddress MAC address of the client
     * @return an OdinClient instance corresponding to clientHwAddress
     */
    OdinClient getClientFromHwAddress(String pool, MACAddress clientHwAddress);


    /**
     * @param pool Pool that the invoking application corresponds to
     * @param agentAddr InetAddress of the agent
     */
    long getLastHeardFromAgent(String pool, InetAddress agentAddr);


    /**
     * Retrieve TxStats from the agent
     *
     * @param pool Pool that the invoking application corresponds to
     * @param agentAddr InetAddress of the agent
     * @return Key-Value entries of each recorded statistic for each client
     */
    Map<MACAddress, Map<String, String>> getTxStatsFromAgent(String pool, InetAddress agentAddr);


    /**
     * Retrieve RxStats from the agent
     *
     * @param pool Pool that the invoking application corresponds to
     * @param agentAddr InetAddress of the agent
     * @return Key-Value entries of each recorded statistic for each client
     */
    Map<MACAddress, Map<String, String>> getRxStatsFromAgent(String pool, InetAddress agentAddr);


    /**
     * Request scanned stations statistics from the agent
     *
     * @param pool Pool that the invoking application corresponds to
     * @param agentAddr InetAddress of the agent
     * @param channel Channel to scan
     * @param ssid Network to scan (always is *)
     * @return If request is accepted return 1, otherwise, return 0
     */
    int requestScannedStationsStatsFromAgent(String pool, InetAddress agentAddr, int channel,
            String ssid);


    /**
     * Retreive scanned stations statistics from the agent
     *
     * @param pool Pool that the invoking application corresponds to
     * @param agentAddr InetAddress of the agent
     * @param ssid Network name
     * @return Key-Value entries of each recorded statistic for each station
     */
    Map<MACAddress, Map<String, String>> getScannedStationsStatsFromAgent(String pool,
            InetAddress agentAddr, String ssid);


    /**
     * Request scanned stations statistics from the agent
     *
     * @param pool Pool that the invoking application corresponds to
     * @param agentAddr InetAddress of the agent
     * @param channel Channel to send measurement beacon to
     * @param ssid Network to scan (e.g odin_init)
     * @return If request is accepted return 1, otherwise, return 0
     */
    int requestSendMesurementBeaconFromAgent(String pool, InetAddress agentAddr, int channel,
            String ssid);


    /**
     * Stop sending mesurement beacon from the agent
     *
     * @param agentAddr InetAddress of the agent
     */
    int stopSendMesurementBeaconFromAgent(String pool, InetAddress agentAddr);


    /**
     * Get a list of Odin agents from the agent tracker
     *
     * @return a map of OdinAgent objects keyed by Ipv4 addresses
     */
    Set<InetAddress> getAgentAddrs(String pool);


    /**
     * Add a subscription for a particular event defined by oes. cb is defines the application
     * specified callback to be invoked during notification. If the application plans to delete the
     * subscription, later, the onus is upon it to keep track of the subscription id for removal
     * later.
     *
     * @param oes the susbcription
     * @param cb the callback
     */
    long registerSubscription(String pool, OdinEventSubscription oes, NotificationCallback cb);


    /**
     * Remove a subscription from the list
     *
     * @param pool Pool that the invoking application corresponds to
     * @param id subscription id to remove
     */
    void unregisterSubscription(String pool, long id);


    /**
     * Add a flow detection for a particular event defined by oefd. cb is defines the application
     * specified callback to be invoked during flow detection. If the application plans to delete
     * the flow detection, later, the onus is upon it to keep track of the flow detection id for
     * removal later.
     *
     * @param oefd the flow detection
     * @param cb the callback
     */
    long registerFlowDetection(String pool, OdinEventFlowDetection oefd, FlowDetectionCallback cb);


    /**
     * Remove a flow detection from the list
     *
     * @param pool Pool that the invoking application corresponds to
     * @param id flow detection id to remove
     */
    void unregisterFlowDetection(String pool, long id);


    /**
     * Add an SSID to the Odin network.
     *
     * @param pool Pool that the invoking application corresponds to
     * @param ssid Network name
     * @return true if the network could be added, false otherwise
     */
    boolean addNetwork(String pool, String ssid);


    /**
     * Remove an SSID from the Odin network.
     *
     * @param pool Pool that the invoking application corresponds to
     * @param ssid Network name
     * @return true if the network could be removed, false otherwise
     */
    boolean removeNetwork(String pool, String ssid);

    /**
     * Change the Wi-Fi channel of a specific agent (AP)
     *
     * @param pool Pool that the invoking application corresponds to
     * @param agentAddr InetAddress of the agent
     * @param channel Wi-Fi Channel
     * @author Luis Sequeira <sequeira@unizar.es>
     */
    void setChannelToAgent(String pool, InetAddress agentAddr, int channel);


    /**
     * Get channel from a specific agent (AP)
     *
     * @param pool Pool that the invoking application corresponds to
     * @param agentAddr InetAddress of the agent
     * @return Channel number
     * @author Luis Sequeira <sequeira@unizar.es>
     */
    int getChannelFromAgent(String pool, InetAddress agentAddr);


    /**
     * Channel Switch Announcement, to the clients of a specific agent (AP)
     *
     * @param pool Pool that the invoking application corresponds to
     * @param agentAddr InetAddress of the agent
     * @param clientHwAddr MAC address of the client
     * @author Luis Sequeira <sequeira@unizar.es>
     */
    void sendChannelSwitchToClient(String pool, InetAddress agentAddr, MACAddress clientHwAddr,
            List<String> lvapSsids, int channel);


    /**
     * Scanning for a client in a specific agent (AP)
     *
     * @param pool Pool that the invoking application corresponds to
     * @param agentAddr InetAddress
     * @param clientHwAddr MAC
     * @param channel Channel
     * @param time Scanning time
     * @return Signal power
     * @author Luis Sequeira <sequeira@unizar.es>
     */
    int scanClientFromAgent(String pool, InetAddress agentAddr, MACAddress clientHwAddr,
            int channel, int time);


    /**
     * Return MobilityParams for Mobility Manager App
     *
     * @return MobilityParams
     */
    MobilityParams getMobilityParams();


    /**
     * Return ScannParams for ShowMatrixOfDistancedBs App
     *
     * @return ScannParams
     */
    ScannParams getMatrixParams();


    /**
     * Return ScannParams for ShowScannedStationsStatistics App
     *
     * @return ScannParams
     */
    ScannParams getInterferenceParams();


    /**
     * Return ChannelAssignmentParams for ChannelAssignment App
     *
     * @return ChannelAssignmentParams
     */
    ChannelAssignmentParams getChannelAssignmentParams();


    /**
     * Return SmartApSelectionParams for SmartApSelection App
     *
     * @return SmartApSelectionParams
     */
    SmartApSelectionParams getSmartApSelectionParams();


    /**
     * Get TxPower from a specific agent (AP)
     *
     * @param pool Pool that the invoking application corresponds to
     * @param agentAddr InetAddress of the agent
     * @return TxPower in dBm
     */
    int getTxPowerFromAgent(String pool, InetAddress agentAddr);


    /**
     * Retreive scanned wi5 stations rssi from the agent
     *
     * @param agentAddr InetAddress of the agent
     * @return Key-Value entries of each recorded rssi for each wi5 station
     */
    String getScannedStaRssiFromAgent(String pool, InetAddress agentAddr);


    /**
     * Retreive associated wi5 stations in the agent
     *
     * @param agentAddr InetAddress of the agent
     * @return Set of OdinClient associated in the agent
     */
    Set<OdinClient> getClientsFromAgent(String pool, InetAddress agentAddr);


    /**
     * Return Vip AP IP address
     *
     * @return vipAPIpaddress
     */
    String getVipAPIpAddress();


    /**
     * Retrieve historical RSSI value stored at the agent, for a certain station
     *
     * @param staHwAddr Ethernet address of the client (Sta)
     * @param agentAddr InetAddress of the agent
     * @return historical RSSI value
     *
     * @author André Oliveira <andreduartecoliveira@gmail.com>
     */
    Double getStaWeightedRssiFromAgent(MACAddress staHwAddr, InetAddress agentAddr);


    /**
     * Gets the target application's state (RUNNING, HALTING, HALTED)
     *
     * @param applicationName Name of the target application
     * @return the application state
     *
     * @author André Oliveira <andreduartecoliveira@gmail.com>
     */
    State getApplicationState(String applicationName);


    /**
     * Sets a target application's state (RUNNING, HALTING, HALTED).
     * It assumes a lock has been set.
     *
     * @param applicationName Name of the target application
     * @param state State to set the application to (RUNNING, HALTING, HALTED)
     * @return true if application exists, false otherwise
     * @author André Oliveira <andreduartecoliveira@gmail.com>
     */
    boolean setApplicationState(String applicationName, State state);
}
