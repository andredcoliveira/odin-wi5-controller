package net.floodlightcontroller.core.web;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import org.openflow.util.HexString;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class SwitchRoleResource extends ServerResource {

    protected static Logger log = LoggerFactory
            .getLogger(SwitchRoleResource.class);

    @Get("json") public Object getRole() {
        IFloodlightProviderService floodlightProvider = (IFloodlightProviderService) getContext()
                .getAttributes().
                        get(IFloodlightProviderService.class
                                    .getCanonicalName());

        String switchId = (String) getRequestAttributes().get("switchId");

        RoleInfo roleInfo;

        if (switchId.equalsIgnoreCase("all")) {
            HashMap<String, RoleInfo> model = new HashMap<String, RoleInfo>();
            for (IOFSwitch sw : floodlightProvider.getSwitches().values()) {
                switchId = sw.getStringId();
                roleInfo = new RoleInfo(sw.getRole());
                model.put(switchId, roleInfo);
            }
            return model;
        }

        Long dpid = HexString.toLong(switchId);
        IOFSwitch sw = floodlightProvider.getSwitches().get(dpid);
        if (sw == null)
            return null;
        roleInfo = new RoleInfo(sw.getRole());
        return roleInfo;
    }
}
