package net.floodlightcontroller.core.web;

import net.floodlightcontroller.core.module.ModuleLoaderResource;
import org.restlet.resource.Get;

import java.util.Map;

public class LoadedModuleLoaderResource extends ModuleLoaderResource {
    /**
     * Retrieves information about all modules available
     * to Floodlight.
     *
     * @return Information about all modules available.
     */
    @Get("json") public Map<String, Object> retrieve() {
        return retrieveInternal(true);
    }
}
