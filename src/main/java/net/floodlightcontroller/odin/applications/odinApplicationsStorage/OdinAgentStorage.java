package net.floodlightcontroller.odin.applications.odinApplicationsStorage;

import java.net.InetAddress;

public class OdinAgentStorage {
	
	private InetAddress ipAddress;
	private String Network;
	private long lastHeard;
	private int channel;
	private Long lastScan;
	private int txpower;
	private int numClients;
	
	public OdinAgentStorage(InetAddress ipAddress, long lastHeard, int channel, String Network,
			Long lastScan, int txpower, int numClients) {
		this.Network = Network;
		this.ipAddress = ipAddress;
		this.lastHeard = lastHeard;
		this.channel = channel;
		this.lastScan = lastScan;
		this.numClients = numClients;
		this.txpower = txpower;
	}

	public InetAddress getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(InetAddress ipAddress) {
		this.ipAddress = ipAddress;
	}

	public long getLastHeard() {
		return lastHeard;
	}

	public void setLastHeard(long lastHeard) {
		this.lastHeard = lastHeard;
	}

	public String getNetwork() {
		return Network;
	}

	public void setLastHeard(String Network) {
		this.Network = Network;
	}

	public int getChannel() {
		return channel;
	}

	public void setChannel(int channel) {
		this.channel = channel;
	}

	public int getNumberOfClients() {
		return numClients;
	}

	public void setNumberOfClients(int numClients) {
		this.numClients = numClients;
	}

	public long getLastScan() {
		return lastScan;
	}

	public void setLastScan(long lastScan) {
		this.lastScan = lastScan;
	}

	public int getTxpower() {
		return txpower;
	}

	public void setTxpower(int txpower) {
		this.txpower = txpower;
	}
	
	
}
