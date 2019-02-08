package net.floodlightcontroller.odin.master.web;

import net.floodlightcontroller.odin.master.*;
import java.util.Set;
import java.util.HashSet;

import net.floodlightcontroller.util.MACAddress;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;


import net.floodlightcontroller.odin.master.*;

import net.floodlightcontroller.odin.applications.odinApplicationsStorage.SmartApSelectionStorage;

import java.util.Set;
import java.util.List;

import net.floodlightcontroller.odin.applications.odinApplicationsStorage.OdinClientStorage;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import net.floodlightcontroller.storage.IStorageSourceService;

public class ConnectedClientsResource extends ServerResource {

	@Get("json")
    public List<OdinClientStorage> retreive() {
    	OdinMaster oc = (OdinMaster) getContext().getAttributes().
        					get(OdinMaster.class.getCanonicalName());
    	
    	//Set<OdinClient> connectedClients = new HashSet<OdinClient> ();

    	
        IStorageSourceService storageService = oc.getStorageService();

        SmartApSelectionStorage smartApSelectionStorage = new SmartApSelectionStorage(storageService);

        List<OdinClientStorage> clients = smartApSelectionStorage.getClients();
    	
    	return clients;
    }


    
}