package net.floodlightcontroller.odin.applications;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.floodlightcontroller.odin.master.OdinApplication;
import net.floodlightcontroller.odin.master.OdinClient;
import net.floodlightcontroller.util.MACAddress;

public class ShowStatistics extends OdinApplication {

  private final int INTERVAL = 10000;
  //private final int SIGNAL_THRESHOLD = 160;

  HashSet<OdinClient> clients;
  //Map<MACAddress, Set<InetAddress>> hearingMap = new HashMap<MACAddress, Set<InetAddress>> ();
  //Map<InetAddress, Integer> newMapping = new HashMap<InetAddress, Integer> ();
	
	
  @Override
  public void run() {
    while (true) {
      try {
        Thread.sleep(INTERVAL);
          clients = new HashSet<OdinClient>(getClients());
          /*
	  * Probe each AP to get the list of MAC addresses that it can "hear".
	  * We define "able to hear" as "signal strength > SIGNAL_THRESHOLD".
	  * 
	  *  We then build the hearing table.
	  */
				 
	  // for each Agent
	  for (InetAddress agentAddr: getAgents()) {
	    Map<MACAddress, Map<String, String>> vals = getRxStatsFromAgent(agentAddr);
	    System.out.println("\nAgent: " + agentAddr);

            // for each STA associated to the Agent
	    for (Entry<MACAddress, Map<String, String>> vals_entry: vals.entrySet()) {
	      MACAddress staHwAddr = vals_entry.getKey();
	      System.out.println("\tStation: " + staHwAddr );
	      System.out.println("\t\tnum packets: " + vals_entry.getValue().get("packets"));
	      System.out.println("\t\tavg rate: " + vals_entry.getValue().get("avg_rate") + "kbps");
	      System.out.println("\t\tavg signal: " + vals_entry.getValue().get("avg_signal") + "dBm");
	      System.out.println("\t\tavg length: " + vals_entry.getValue().get("avg_len_pkt") + "bytes");
	      System.out.println("\t\tair time: " + vals_entry.getValue().get("air_time") + "ms");						
	      System.out.println("\t\tinit time: " + vals_entry.getValue().get("first_received") + "sec");
	      System.out.println("\t\tend time: " + vals_entry.getValue().get("last_received") + "sec");
	      System.out.println("");
	    }
	  }
	} catch (InterruptedException e) {
	  e.printStackTrace();
	}
      }
    }
}
