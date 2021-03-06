package net.floodlightcontroller.core.module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * Finds all Floodlight modules in the class path and loads/starts them.
 *
 * @author alexreimers
 */
public class FloodlightModuleLoader {
    protected static Logger logger = LoggerFactory
            .getLogger(FloodlightModuleLoader.class);

    protected static Map<Class<? extends IFloodlightService>, Collection<IFloodlightModule>> serviceMap;
    protected static Map<IFloodlightModule, Collection<Class<? extends IFloodlightService>>> moduleServiceMap;
    protected static Map<String, IFloodlightModule> moduleNameMap;
    protected static Object lock = new Object();

    protected FloodlightModuleContext floodlightModuleContext;

    public static final String COMPILED_CONF_FILE = "floodlightdefault.properties";
    public static final String FLOODLIGHT_MODULES_KEY = "floodlight.modules";

    public FloodlightModuleLoader() {
        floodlightModuleContext = new FloodlightModuleContext();
    }

    /**
     * Finds all IFloodlightModule(s) in the classpath. It creates 3 Maps.
     * serviceMap -> Maps a service to a module
     * moduleServiceMap -> Maps a module to all the services it provides
     * moduleNameMap -> Maps the string name to the module
     *
     * @throws FloodlightModuleException If two modules are specified in the configuration
     *                                   that provide the same service.
     */
    protected static void findAllModules(Collection<String> mList)
            throws FloodlightModuleException {
        synchronized (lock) {
            if (serviceMap != null)
                return;
            serviceMap = new HashMap<Class<? extends IFloodlightService>, Collection<IFloodlightModule>>();
            moduleServiceMap = new HashMap<IFloodlightModule, Collection<Class<? extends IFloodlightService>>>();
            moduleNameMap = new HashMap<String, IFloodlightModule>();

            // Get all the current modules in the classpath
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            ServiceLoader<IFloodlightModule> moduleLoader = ServiceLoader
                    .load(IFloodlightModule.class, cl);
            // Iterate for each module, iterate through and add it's services
            for (IFloodlightModule m : moduleLoader) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Found module " + m.getClass().getName());
                }

                // Set up moduleNameMap
                moduleNameMap.put(m.getClass().getCanonicalName(), m);

                // Set up serviceMap
                Collection<Class<? extends IFloodlightService>> servs = m
                        .getModuleServices();
                if (servs != null) {
                    moduleServiceMap.put(m, servs);
                    for (Class<? extends IFloodlightService> s : servs) {
                        Collection<IFloodlightModule> mods = serviceMap
                                .get(s);
                        if (mods == null) {
                            mods = new ArrayList<IFloodlightModule>();
                            serviceMap.put(s, mods);
                        }
                        mods.add(m);
                        // Make sure they haven't specified duplicate modules in the config
                        int dupInConf = 0;
                        for (IFloodlightModule cMod : mods) {
                            if (mList.contains(
                                    cMod.getClass().getCanonicalName()))
                                dupInConf++;
                        }

                        if (dupInConf > 1) {
                            String duplicateMods = "";
                            for (IFloodlightModule mod : mods) {
                                duplicateMods +=
                                        mod.getClass().getCanonicalName()
                                        + ", ";
                            }
                            throw new FloodlightModuleException(
                                    "ERROR! The configuraiton "
                                    + " file specifies more than one module that provides the service "
                                    + s.getCanonicalName()
                                    + ". Please specify only ONE of the "
                                    + "following modules in the config file: "
                                    + duplicateMods);
                        }
                    }
                }
            }
        }
    }

    /**
     * Loads the modules from a specified configuration file.
     *
     * @param fName The configuration file path
     * @return An IFloodlightModuleContext with all the modules to be started
     * @throws FloodlightModuleException
     */
    public IFloodlightModuleContext loadModulesFromConfig(String fName)
            throws FloodlightModuleException {
        Properties prop = new Properties();

        File f = new File(fName);
        if (f.isFile()) {
            logger.info("Loading modules from file " + fName);
            try {
                prop.load(new FileInputStream(fName));
            } catch (FileNotFoundException e) {
                // should not happen
                e.printStackTrace();
            } catch (IOException e) {
                // should not happen
                e.printStackTrace();
            }
        } else {
            logger.info("Loading default modules");
            InputStream is = this.getClass().getClassLoader().
                    getResourceAsStream(COMPILED_CONF_FILE);
            try {
                prop.load(is);
            } catch (IOException e) {
                logger.error("Error, could not load default modules");
                logger.error(e.getMessage());
                System.exit(1);
            }
        }

        String moduleList = prop.getProperty(FLOODLIGHT_MODULES_KEY)
                                .replaceAll("\\s", "");
        Collection<String> configMods = new ArrayList<String>();
        configMods.addAll(Arrays.asList(moduleList.split(",")));
        return loadModulesFromList(configMods, prop);
    }

    /**
     * Loads modules (and their dependencies) specified in the list
     *
     * @param mList The array of fully qualified module names
     * @return The ModuleContext containing all the loaded modules
     * @throws FloodlightModuleException
     */
    public IFloodlightModuleContext loadModulesFromList(
            Collection<String> configMods, Properties prop)
            throws FloodlightModuleException {
        logger.debug("Starting module loader");
        findAllModules(configMods);

        Collection<IFloodlightModule> moduleSet = new ArrayList<IFloodlightModule>();
        Map<Class<? extends IFloodlightService>, IFloodlightModule> moduleMap = new HashMap<Class<? extends IFloodlightService>, IFloodlightModule>();

        Queue<String> moduleQ = new LinkedList<String>();
        // Add the explicitly configured modules to the q
        moduleQ.addAll(configMods);
        Set<String> modsVisited = new HashSet<String>();

        while (!moduleQ.isEmpty()) {
            String moduleName = moduleQ.remove();
            if (modsVisited.contains(moduleName))
                continue;
            modsVisited.add(moduleName);
            IFloodlightModule module = moduleNameMap.get(moduleName);
            if (module == null) {
                throw new FloodlightModuleException(
                        "Module " + moduleName + " not found");
            }
            // Add the module to be loaded
            addModule(moduleMap, moduleSet, module);
            // Add it's dep's to the queue
            Collection<Class<? extends IFloodlightService>> deps = module
                    .getModuleDependencies();
            if (deps != null) {
                for (Class<? extends IFloodlightService> c : deps) {
                    IFloodlightModule m = moduleMap.get(c);
                    if (m == null) {
                        Collection<IFloodlightModule> mods = serviceMap
                                .get(c);
                        // Make sure only one module is loaded
                        if ((mods == null) || (mods.size() == 0)) {
                            throw new FloodlightModuleException(
                                    "ERROR! Could not "
                                    + "find an IFloodlightModule that provides service "
                                    + c.toString());
                        } else if (mods.size() == 1) {
                            IFloodlightModule mod = mods.iterator().next();
                            if (!modsVisited.contains(
                                    mod.getClass().getCanonicalName()))
                                moduleQ.add(
                                        mod.getClass().getCanonicalName());
                        } else {
                            boolean found = false;
                            for (IFloodlightModule moduleDep : mods) {
                                if (configMods.contains(moduleDep.getClass()
                                                                 .getCanonicalName())) {
                                    // Module will be loaded, we can continue
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                String duplicateMods = "";
                                for (IFloodlightModule mod : mods) {
                                    duplicateMods +=
                                            mod.getClass().getCanonicalName()
                                            + ", ";
                                }
                                throw new FloodlightModuleException(
                                        "ERROR! Found more " + "than one ("
                                        + mods.size()
                                        + ") IFloodlightModules that provides "
                                        + "service " + c.toString()
                                        + ". Please specify one of the following modules in the config: "
                                        + duplicateMods);
                            }
                        }
                    }
                }
            }
        }

        floodlightModuleContext.setModuleSet(moduleSet);
        parseConfigParameters(prop);
        initModules(moduleSet);
        startupModules(moduleSet);

        return floodlightModuleContext;
    }

    /**
     * Add a module to the set of modules to load and register its services
     *
     * @param moduleMap the module map
     * @param moduleSet the module set
     * @param module    the module to add
     */
    protected void addModule(
            Map<Class<? extends IFloodlightService>, IFloodlightModule> moduleMap,
            Collection<IFloodlightModule> moduleSet,
            IFloodlightModule module) {
        if (!moduleSet.contains(module)) {
            Collection<Class<? extends IFloodlightService>> servs = moduleServiceMap
                    .get(module);
            if (servs != null) {
                for (Class<? extends IFloodlightService> c : servs)
                    moduleMap.put(c, module);
            }
            moduleSet.add(module);
        }
    }

    /**
     * Allocate  service implementations and then init all the modules
     *
     * @param moduleSet The set of modules to call their init function on
     * @throws FloodlightModuleException If a module can not properly be loaded
     */
    protected void initModules(Collection<IFloodlightModule> moduleSet)
            throws FloodlightModuleException {
        for (IFloodlightModule module : moduleSet) {
            // Get the module's service instance(s)
            Map<Class<? extends IFloodlightService>, IFloodlightService> simpls = module
                    .getServiceImpls();

            // add its services to the context
            if (simpls != null) {
                for (Entry<Class<? extends IFloodlightService>, IFloodlightService> s : simpls
                        .entrySet()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Setting " + s.getValue()
                                     + "  as provider for " + s.getKey()
                                                               .getCanonicalName());
                    }
                    if (floodlightModuleContext.getServiceImpl(s.getKey())
                        == null) {
                        floodlightModuleContext
                                .addService(s.getKey(), s.getValue());
                    } else {
                        throw new FloodlightModuleException(
                                "Cannot set " + s.getValue()
                                + " as the provider for " + s.getKey()
                                                             .getCanonicalName()
                                + " because " + floodlightModuleContext
                                        .getServiceImpl(s.getKey())
                                + " already provides it");
                    }
                }
            }
        }

        for (IFloodlightModule module : moduleSet) {
            // init the module
            if (logger.isDebugEnabled()) {
                logger.debug("Initializing " + module.getClass()
                                                     .getCanonicalName());
            }
            module.init(floodlightModuleContext);
        }
    }

    /**
     * Call each loaded module's startup method
     *
     * @param moduleSet the module set to start up
     */
    protected void startupModules(Collection<IFloodlightModule> moduleSet) {
        for (IFloodlightModule m : moduleSet) {
            if (logger.isDebugEnabled()) {
                logger.debug("Starting " + m.getClass().getCanonicalName());
            }
            m.startUp(floodlightModuleContext);
        }
    }

    /**
     * Parses configuration parameters for each module
     *
     * @param prop The properties file to use
     */
    protected void parseConfigParameters(Properties prop) {
        Enumeration<?> e = prop.propertyNames();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            // Ignore module list key
            if (key.equals(FLOODLIGHT_MODULES_KEY)) {
                continue;
            }

            String configValue = null;
            int lastPeriod = key.lastIndexOf(".");
            String moduleName = key.substring(0, lastPeriod);
            String configKey = key.substring(lastPeriod + 1);
            // Check to see if it's overridden on the command line
            String systemKey = System.getProperty(key);
            if (systemKey != null) {
                configValue = systemKey;
            } else {
                configValue = prop.getProperty(key);
            }

            IFloodlightModule mod = moduleNameMap.get(moduleName);
            if (mod == null) {
                logger.warn("Module {} not found or loaded. "
                            + "Not adding configuration option {} = {}",
                            new Object[] { moduleName, configKey,
                                           configValue });
            } else {
                floodlightModuleContext
                        .addConfigParam(mod, configKey, configValue);
            }
        }
    }
}
