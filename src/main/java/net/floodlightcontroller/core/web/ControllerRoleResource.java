package net.floodlightcontroller.core.web;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerRoleResource extends ServerResource {

    protected static Logger log = LoggerFactory
            .getLogger(ControllerRoleResource.class);

    @Get("json") public RoleInfo getRole() {
        IFloodlightProviderService floodlightProvider = (IFloodlightProviderService) getContext()
                .getAttributes().
                        get(IFloodlightProviderService.class
                                    .getCanonicalName());
        return new RoleInfo(floodlightProvider.getRole());
    }

    @Post("json") public void setRole(RoleInfo roleInfo) {
        //Role role = Role.lookupRole(roleInfo.getRole());
        Role role = null;
        try {
            role = Role.valueOf(roleInfo.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            // The role value in the REST call didn't match a valid
            // role name, so just leave the role as null and handle
            // the error below.
        }
        if (role == null) {
            log.warn(
                    "Invalid role value specified in REST API to set controller role");
            setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid role value");
            return;
        }

        IFloodlightProviderService floodlightProvider = (IFloodlightProviderService) getContext()
                .getAttributes().
                        get(IFloodlightProviderService.class
                                    .getCanonicalName());

        floodlightProvider.setRole(role);
    }
}
