package net.floodlightcontroller.odin.master.web;

import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.restlet.resource.ResourceException;
import java.net.InetAddress;
import java.util.Map;


import net.floodlightcontroller.odin.applications.odinApplicationsStorage.OdinAgentStorage;
import net.floodlightcontroller.odin.applications.odinApplicationsStorage.SmartApSelectionStorage;
import net.floodlightcontroller.odin.master.*;
import net.floodlightcontroller.util.MACAddress;


import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import com.google.gson.*;


public class AgentScanManagerResource extends ServerResource {

	@Post("json")    
    public String retreive(String mapApIpAddress) {

    	OdinMaster oc = (OdinMaster) getContext().getAttributes().
				get(OdinMaster.class.getCanonicalName());

		IPAddress fmdata;
		if(mapApIpAddress == null) {
			throw new ResourceException(400);
		}

		try {
            Gson gson = new GsonBuilder().create();
            fmdata = gson.fromJson(mapApIpAddress, IPAddress.class);
			
			String apIpAddress = fmdata.getApIpAddress();
			String network = fmdata.getNetwork();
			int channel = fmdata.getChannel();

			int isBusy = oc.requestScannedStationsStatsFromAgent(PoolManager.GLOBAL_POOL,InetAddress.getByName(apIpAddress),channel,network);

            try {
                int sleep =fmdata.geTimeToScan();
                if(sleep == 0)
                    sleep = 200;
                Thread.sleep(sleep);
            } 
            catch (InterruptedException e) {
                e.printStackTrace();
            } 
			if(isBusy == 0){
				System.out.println("-------------------------------------------------------------------------------Agent BUSY");
                return "{ \"res\": \"AGENT BUSY ON ACTION\" }";
			}
			else{
				System.out.println("-------------------------------------------------------------------------------DONE");
				String res = oc.getScannedStaRssiFromAgent(PoolManager.GLOBAL_POOL, InetAddress.getByName(apIpAddress));
                return "{  \"res\":\"" + res + "\"}";
			}
		} catch (Exception ex) {
			throw new ResourceException(400);
		}

    }


    static class IPAddress{
    	private String apIpAddress;
    	private String network;
    	private int channel;
        private int timeToScan;

    	public IPAddress(String apIpAddress, String network, int channel, int timeToScan){
    		this.apIpAddress = apIpAddress;
    		this.network = network;
    		this.channel = channel;
    	}

    	public String getApIpAddress(){
    		return this.apIpAddress;
    	}

    	public String getNetwork(){
    		return this.network;
    	}

    	public int getChannel(){
    		return this.channel;
    	}

        public int geTimeToScan(){
            return this.timeToScan;
        }


    }
}
