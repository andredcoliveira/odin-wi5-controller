package net.floodlightcontroller.odin.master.web;

import java.io.File;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.Map;

import net.floodlightcontroller.odin.master.OdinMaster;
import net.floodlightcontroller.odin.master.PoolManager;
import net.floodlightcontroller.util.MACAddress;

import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.restlet.resource.ResourceException;
import com.google.gson.*;


public class ScannedStationsStats extends ServerResource {

	
	@Post("json")
	public Map<MACAddress, Map<String, String>> GetAgentScannedStationStats(String IpAddress){

		OdinMaster oc = (OdinMaster) getContext().getAttributes().
				get(OdinMaster.class.getCanonicalName());

		IPAddress fmdata;
		if(IpAddress == null) {
			throw new ResourceException(400);
		}

		try {
            Gson gson = new GsonBuilder().create();
            fmdata = gson.fromJson(IpAddress, IPAddress.class);
			
			String apIpAddress = fmdata.getApIpAddress();
		
			Map<MACAddress, Map<String, String>> res = oc.getScannedStationsStatsFromAgent(PoolManager.GLOBAL_POOL, InetAddress.getByName(apIpAddress), "*");
			
			return res;
			
		}catch(Exception ex){
			File f = new File("SmartApSelectionLog.txt");
			try {
				PrintStream ps = new PrintStream(f);
				ex.printStackTrace(ps);
				ps.flush();
				return null;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
	}


    static class IPAddress{
    	private String apIpAddress;

    	public IPAddress(String apIpAddress){
    		this.apIpAddress = apIpAddress;
    	}

    	public String getApIpAddress(){
    		return apIpAddress;
    	}
    }
}
