package net.floodlightcontroller.odin.master.web;

import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import java.io.File;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.odin.applications.odinApplicationsStorage.OdinClientStorage;
import net.floodlightcontroller.odin.applications.odinApplicationsStorage.SmartApSelectionStorage;
import net.floodlightcontroller.odin.master.OdinMaster;
import net.floodlightcontroller.odin.master.PoolManager;
import net.floodlightcontroller.storage.IStorageSourceService;
import net.floodlightcontroller.util.MACAddress;
import org.restlet.resource.Get;



public class PassiveMatrixManagerResurce extends ServerResource  {
	
	@Get("json")
	public PassiveMatrix GetPassiveMatrix(){

		OdinMaster oc = (OdinMaster) getContext().getAttributes().
				get(OdinMaster.class.getCanonicalName());
		
    	IStorageSourceService storageService = oc.getStorageService();
    	
    	SmartApSelectionStorage smartApSelectionStorage = new SmartApSelectionStorage(storageService);

		try {
			
			PassiveMatrix passiveMatrix = new PassiveMatrix();

			Set<InetAddress> agentsAddress = oc.getAgentAddrs(PoolManager.GLOBAL_POOL);
			
			
	    	List<OdinClientStorage> storage_clients = smartApSelectionStorage.getClients();
	    	
	    	
	    	String[] agents = new String[agentsAddress.size()];
	    	
	    	String[] clients = new String[storage_clients.size()];
	    	
	    	double[][] dbmPerClient = new double[agents.length][clients.length];
	    	
	    	int j = 0;
    		for (OdinClientStorage client : storage_clients) {
				clients[j]=client.getHwAddress();
				j++;
			}

	    	int i = 0;
	    	for (InetAddress agent : agentsAddress) {
				agents[i]=agent.getHostAddress();
				for (int k = 0; k < clients.length; k++) {
						OdinClientStorage client = storage_clients.get(k);
						Map<InetAddress, Integer> agentsHeardClient =client.getAgentsHeardClient();

						Double[] averageDBM = client.getAverageDBM();
						int id = agentsHeardClient.get(agent.getHostAddress());	
						dbmPerClient[i][k] = averageDBM[id];
				}
				i++;
				
			}

			passiveMatrix.setAgents(agents);
			passiveMatrix.setClients(clients);
			passiveMatrix.setHeardDbms(dbmPerClient);
	        
	        return passiveMatrix;
						
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


	class PassiveMatrix{
		
		private String[] agents;
		private String[] clients;
		private double[][] heardDbms;
		
		public PassiveMatrix() {};
		
		public PassiveMatrix(String[] Agents, double[][] HeardDbms)
		{
			this.agents =Agents;
			this.heardDbms = HeardDbms;
			this.clients = clients;
		}
	    public String[] getAgents() {
	        return agents;
	    }

	    public void setAgents(String[] agents) {
	        this.agents = agents;
	    }

	    public String[] getClients() {
	        return clients;
	    }

	    public void setClients(String[] clients) {
	        this.clients = clients;
	    }

	    public double[][] getHeardDbms() {
	        return heardDbms;
	    }

	    public void setHeardDbms(double[][] heardDbms) {
	        this.heardDbms = heardDbms;
	    }	    

	}
}
