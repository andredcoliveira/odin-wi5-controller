package net.floodlightcontroller.odin.master;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;


import net.floodlightcontroller.odin.master.web.*;
import net.floodlightcontroller.restserver.RestletRoutable;

class OdinMasterWebRoutable implements RestletRoutable {

	@Override
	public String basePath() {
		return "/odin";
	}

	@Override
	public Restlet getRestlet(Context context) {
		Router router = new Router(context);
		router.attach("/clients/all/json", AllClientsResource.class);
		router.attach("/clients/connected/json", ConnectedClientsResource.class);
		router.attach("/agents/json", AgentManagerResource.class);
		router.attach("/handoff/json", LvapHandoffResource.class);
		router.attach("/agents/scan/json", AgentScanManagerResource.class);
		router.attach("/params/json", SmartApParametersResource.class);
		router.attach("/agents/scaninfo/external/json", ScannedStationsStats.class);
		router.attach("/agents/scaninfo/internal/txStats/json",AgentTxStats.class);
		router.attach("/agents/scaninfo/internal/rxStats/json",AgentRxStats.class);
		router.attach("/agents/pasiveMatrix", PassiveMatrixManagerResurce.class);

		return router;
	}
}