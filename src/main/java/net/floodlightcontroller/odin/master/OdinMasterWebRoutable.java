package net.floodlightcontroller.odin.master;

import net.floodlightcontroller.restserver.RestletRoutable;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

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
        return router;
    }
}