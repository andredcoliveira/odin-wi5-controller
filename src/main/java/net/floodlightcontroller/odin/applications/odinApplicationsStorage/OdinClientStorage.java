package net.floodlightcontroller.odin.applications.odinApplicationsStorage;

import java.net.InetAddress;
import java.util.Map;

import net.floodlightcontroller.odin.master.Lvap;
import net.floodlightcontroller.util.MACAddress;

public class OdinClientStorage {
	
	private final String hwAddress;
	private InetAddress ipAddress;
	private String agenIpAddress;
	private Map<InetAddress, Integer> agentsHeardClient;
	private Lvap lvap;
	private Double[] averageDBM;
	private long lastScanInfo;
	private Long lastHeardFromClient;
	
	
	public OdinClientStorage(String hwAddress, InetAddress ipAddress,
			Lvap lvap) {
		this.hwAddress = hwAddress;
		this.ipAddress = ipAddress;
		this.lvap = lvap;
	}



	public OdinClientStorage(String hwAddress, InetAddress ipAddress, Lvap lvap,
			Double[] averageDBM, long lastScanInfo) {
		this.hwAddress = hwAddress;
		this.ipAddress = ipAddress;
		this.lvap = lvap;
		this.agenIpAddress = lvap.getAgent().getIpAddress().toString();
		this.averageDBM = averageDBM;
		this.lastScanInfo = lastScanInfo;
	}


	public InetAddress getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(InetAddress ipAddress) {
		this.ipAddress = ipAddress;
	}

	public Lvap getLvap() {
		return lvap;
	}

	public void setLvap(Lvap lvap) {
		this.lvap = lvap;
	}

	public Double[] getAverageDBM() {
		return averageDBM;
	}

	public void setAverageDBM(Double[] averageDBM) {
		this.averageDBM = averageDBM;
	}


	public void setAgentsHeardClient(Map<InetAddress, Integer> agentsHeardClient) {
		this.agentsHeardClient = agentsHeardClient;
	}

	public Map<InetAddress, Integer> getAgentsHeardClient() {
		return agentsHeardClient;
	}


	public long getLastScanInfo() {
		return lastScanInfo;
	}

	public void setLastScanInfo(long lastScanInfo) {
		this.lastScanInfo = lastScanInfo;
	}

	public String getHwAddress() {
		return hwAddress;
	}	
}
