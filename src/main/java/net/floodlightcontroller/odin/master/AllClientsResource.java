package net.floodlightcontroller.odin.master;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import java.util.Set;

public class AllClientsResource extends ServerResource {

    @Get("json") public Set<OdinClient> retreive() {
        OdinMaster oc = (OdinMaster) getContext().getAttributes().
                get(OdinMaster.class.getCanonicalName());

        return oc.getClients(PoolManager.GLOBAL_POOL);
    }
}
