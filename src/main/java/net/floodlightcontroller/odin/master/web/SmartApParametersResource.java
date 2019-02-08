package net.floodlightcontroller.odin.master.web;

import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.restlet.resource.ResourceException;

import net.floodlightcontroller.odin.master.OdinMaster.SmartApSelectionParams;
import net.floodlightcontroller.odin.master.*;


public class SmartApParametersResource extends ServerResource {

	@Get("json")
    public SmartApSelectionParams retreive() {

		OdinMaster oc = (OdinMaster) getContext().getAttributes().
				get(OdinMaster.class.getCanonicalName());

		return oc.getSmartApSelectionParams();
	}

}