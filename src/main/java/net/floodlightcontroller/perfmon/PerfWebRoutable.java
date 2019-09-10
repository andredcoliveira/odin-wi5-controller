package net.floodlightcontroller.perfmon;

import net.floodlightcontroller.restserver.RestletRoutable;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

public class PerfWebRoutable implements RestletRoutable {

    @Override public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        router.attach("/data/json", PerfMonDataResource.class);
        router.attach("/{perfmonstate}/json",
                      PerfMonToggleResource.class); // enable, disable, or reset
        return router;
    }

    @Override public String basePath() {
        return "/wm/performance";
    }
}
