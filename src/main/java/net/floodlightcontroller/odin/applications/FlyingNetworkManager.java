package net.floodlightcontroller.odin.applications;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.floodlightcontroller.odin.master.OdinApplication;
import net.floodlightcontroller.odin.master.OdinClient;
import net.floodlightcontroller.util.MACAddress;

/**
 * Application that handles predictive mobility management in flying networks
 *
 * @author André Oliveira <andreduartecoliveira@gmail.com>
 */
public class FlyingNetworkManager extends OdinApplication {

    // TODO: Implementar versão de teste (JSON com data exata + tempo para handoff)
    String VERSION = "TEST"; // "TEST" || "PRODUCTION"

    @Override
    public void run() {

        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        try {
            serverSocket = new ServerSocket(6666);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // infinite loop if server socket is valid (i.e., listening on port 6666)
        while (serverSocket != null) {
            String message;
            try {
                clientSocket = serverSocket.accept(); // blocks until someone connects
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                message = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            out.println(Instant.now().toString());
            long receiveTime = System.nanoTime();

            if (!possibleJson(message)) {
                continue;
            }

            // Wait until it's safe
            tryHaltApplication("SmartApSelection");
            synchronized (getLock()) {
                while (!getApplicationState("SmartApSelection").equals(State.HALTED)) {
                    try {
                        getLock().wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Handle handoffs
                long resumeDelay = 0L;
                if (VERSION.equals("PRODUCTION")) {
                    resumeDelay = handleHandoffs(message, receiveTime);
                } else if (VERSION.equals("TEST")) {
                    resumeDelay = handleHandoffsTest(message, receiveTime);
                }

                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

                // Resume SmartApSelection() after the UAVs/APs are done moving around
                scheduler.schedule(
                        new RunResumeApplication("SmartApSelection", getLock()),
                        resumeDelay - (System.nanoTime() - receiveTime),
                        TimeUnit.NANOSECONDS);
                scheduler.shutdown();

                getLock().notifyAll();
            }

            out.println("DONE");
        }

        try {
            in.close();
            out.close();
            clientSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks very basic JSON rules. Eliminates easy targets.
     *
     * @param message the JSON string
     * @return true if string might be a JSON object, false otherwise
     */
    private boolean possibleJson(String message) {
        return message != null
                && !message.trim().isEmpty()
                && ((message.startsWith("[") && message.endsWith("]"))
                || (message.startsWith("{") && message.endsWith("}")));
    }

    /**
     * Handles a set of handoffs given a JSON string with IPv4 Addresses keying ApRelocation
     * objects. It requires: Origin coordinates (GPS), Destination coordinates (GPS), Reference
     * coordinates (GPS) and Velocity (NED), for every UAV acting as a possible AP.
     *
     * @param message JSON string with information regarding UAV relocations
     * @param receiveTime time [ns] used as reference to begin operations
     * @return the duration [ns] of the longest UAV flight (operation time)
     */
    private long handleHandoffs(String message, long receiveTime) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, ApRelocation> apRelocations;
        try {
            apRelocations = objectMapper.readValue(message, new TypeReference<Map<String, ApRelocation>>() {});
        } catch (IOException e) {
            apRelocations = Collections.emptyMap();
            e.printStackTrace();
        }

        HashSet<InetAddress> agents = new HashSet<>(getAgents());
        HashSet<OdinClient> clients = new HashSet<>(getClients());

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(clients.size());

        // For each UAV: GPS -> ECEF -> NED (Origin & Destination)
        Map<InetAddress, Cartesian> agentCoordinatesNedOrigin = new HashMap<>();
        Map<InetAddress, Cartesian> agentCoordinatesNedDestination = new HashMap<>();

        AtomicReference<Double> longestFlight = new AtomicReference<>(0.0);

        apRelocations.forEach(
                (agent, apRelocation) -> {
                    InetAddress agentAddress = null;
                    try {
                        agentAddress = InetAddress.getByName(agent);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }

                    if (agentAddress != null) {
                        Cartesian originNed =
                                apRelocation
                                        .origin
                                        .toEcef()
                                        .toNed(
                                                apRelocation.reference.lat,
                                                apRelocation.reference.lon,
                                                apRelocation.reference.alt);
                        agentCoordinatesNedOrigin.put(agentAddress, originNed);

                        Cartesian destinationNed =
                                apRelocation
                                        .destination
                                        .toEcef()
                                        .toNed(
                                                apRelocation.reference.lat,
                                                apRelocation.reference.lon,
                                                apRelocation.reference.alt);
                        agentCoordinatesNedDestination.put(agentAddress, destinationNed);

                        double flightX = (destinationNed.x - originNed.x) / apRelocation.velocity.x;
                        double flightY = (destinationNed.y - originNed.y) / apRelocation.velocity.y;
                        double flightZ = (destinationNed.z - originNed.z) / apRelocation.velocity.z;
                        double flightTime;

                        if (flightX >= flightY && flightX >= flightZ) {
                            flightTime = flightX;
                        } else if (flightY >= flightX && flightY >= flightZ) {
                            flightTime = flightY;
                        } else {
                            flightTime = flightZ;
                        }

                        if (flightTime > longestFlight.get()) {
                            longestFlight.set(flightTime);
                        }
                    }
                });

        // For each client: (NED, wRSSI)_uavs -> NED
        Map<MACAddress, Cartesian> clientCoordinatesNed = new HashMap<>();

        for (OdinClient client : clients) {
            double x = 0.0, y = 0.0, z = 0.0;
            int countValid = 0;

            for (InetAddress agent : agents) {
                Double weightedRssi = getStaWeightedRssiFromAgent(client.getMacAddress(), agent);

                if (weightedRssi != null && weightedRssi != -99.9) {
                    x += agentCoordinatesNedOrigin.get(agent).x * weightedRssi;
                    y += agentCoordinatesNedOrigin.get(agent).y * weightedRssi;
                    z += agentCoordinatesNedOrigin.get(agent).z * weightedRssi;

                    countValid++;
                }
            }

            x /= countValid;
            y /= countValid;
            z /= countValid;

            clientCoordinatesNed.put(client.getMacAddress(), new Cartesian(x, y, z));
        }

        // Foreach client in client_set
        for (OdinClient client : clients) {
            // Calculate distance squared to each AP (in its final position)
            InetAddress currentAgent = client.getLvap().getAgent().getIpAddress();
            InetAddress futureAgent = null;
            double min = Double.MAX_VALUE; // meters

            for (InetAddress agent : agents) {
                Double distanceSqr =
                        distanceSquared(
                                clientCoordinatesNed.get(client.getMacAddress()),
                                agentCoordinatesNedDestination.get(agent));

                if (distanceSqr < min) {
                    min = distanceSqr;
                    futureAgent = agent;
                }
            }

            // Check if client needs a handoff
            if (futureAgent == null || futureAgent.equals(currentAgent)) {
                continue;
            }

            // Calculate time for handoff
            Cartesian v1 = apRelocations.get(currentAgent.toString().substring(1)).velocity;
            Cartesian v2 = apRelocations.get(futureAgent.toString().substring(1)).velocity;
            Cartesian p1 = agentCoordinatesNedOrigin.get(currentAgent);
            Cartesian p2 = agentCoordinatesNedOrigin.get(futureAgent);
            Cartesian p = clientCoordinatesNed.get(client.getMacAddress());

            Double delay = lowestPositiveQuadraticSolution(
                    v1.x * v1.x + v1.y * v1.y + v1.z * v1.z - v2.x * v2.x - v2.y * v2.y
                            - v2.z * v2.z,
                    2 * (v1.x * (p1.x - p.x)
                            + v1.y * (p1.y - p.y)
                            + v1.z * (p1.z - p.z)
                            - v2.x * (p2.x - p.x)
                            - v2.y * (p2.y - p.y)
                            - v2.z * (p2.z - p.z)),
                    p1.x * p1.x
                            + p1.y * p1.y
                            + p1.z * p1.z
                            - p2.x * p2.x
                            - p2.y * p2.y
                            - p2.z * p2.z
                            - 2 * (p.x * (p1.x - p2.x)
                            + p.y * (p1.y - p2.y)
                            + p.z * (p1.z - p2.z))); // seconds

            if (delay != null) {
                // Schedule handoff
                scheduler.schedule(
                        new RunHandoffClientToAp(client.getMacAddress(), futureAgent),
                        Math.round(delay * 1000) - (System.nanoTime() - receiveTime) / 1000000,
                        TimeUnit.MILLISECONDS);
            }
        }

        // Gracefully shutdown the scheduler (i.e., still finishes old tasks)
        scheduler.shutdown();

        return (long) (longestFlight.get().doubleValue());
    }

    public double distanceSquared(Cartesian p1, Cartesian p2) {
        return ((p1.x - p2.x) * (p1.x - p2.x)
                + (p1.y - p2.y) * (p1.y - p2.y)
                + (p1.z - p2.z) * (p1.z - p2.z));
    }

    public double distance(Cartesian p1, Cartesian p2) {
        return Math.sqrt(distanceSquared(p1, p2));
    }

    private static Double lowestPositiveQuadraticSolution(double a, double b, double c) {

        double discriminant = b * b - 4 * a * c;

        // no real solution
        // avoids complex solutions
        if (discriminant < 0) {
            return null;
        }

        double answerPlus = (-b + Math.sqrt(discriminant)) / (2 * a);
        double answerMinus = (-b - Math.sqrt(discriminant)) / (2 * a);

        // discard negative numbers
        if ((answerPlus < 0 && answerMinus < 0)
                || (Double.isNaN(answerPlus) && Double.isNaN(answerMinus))) {
            return null;
        } else if (answerPlus > 0 && answerMinus > 0) {
            return Math.min(answerPlus, answerMinus);
        }

        // return that which is > 0
        return (answerPlus > answerMinus) ? answerPlus : answerMinus;
    }

    public class RunHandoffClientToAp implements Runnable {

        private final MACAddress staHwAddr;
        private final InetAddress futureAgent;

        public RunHandoffClientToAp(MACAddress staHwAddr, InetAddress futureAgent) {
            this.staHwAddr = staHwAddr;
            this.futureAgent = futureAgent;
        }

        @Override
        public void run() {
            handoffClientToAp(staHwAddr, futureAgent);
        }
    }

    protected class RunResumeApplication implements Runnable {

        final String appName;
        final Object lock;

        protected RunResumeApplication(String appName, Object lock) {
            this.appName = appName;
            this.lock = lock;
        }

        @Override
        public void run() {
            synchronized (lock) {
                if (resumeApplication(appName)) {
                    lock.notifyAll();
                }
            }
        }
    }

    public static class ApRelocation {

        public GpsCoordinates origin;
        public GpsCoordinates destination;
        public GpsCoordinates reference;
        public Cartesian velocity;

        @JsonCreator
        public ApRelocation(
                @JsonProperty("origin") GpsCoordinates origin,
                @JsonProperty("destination") GpsCoordinates destination,
                @JsonProperty("reference") GpsCoordinates reference,
                @JsonProperty("velocity") Cartesian velocity) {
            this.origin = origin;
            this.destination = destination;
            this.reference = reference;
            this.velocity = velocity;
        }

        @Override
        public String toString() {
            return "{\n"
                    + "\n  origin="
                    + origin
                    + "\n  destination="
                    + destination
                    + "\n  reference="
                    + reference
                    + "\n  velocity="
                    + velocity
                    + "\n}";
        }
    }

    @SuppressWarnings("Duplicates")
    public static class GpsCoordinates {

        double lat; // degrees
        double lon; // degrees
        double alt;

        @JsonCreator
        public GpsCoordinates(
                @JsonProperty("lat") double lat,
                @JsonProperty("lon") double lon,
                @JsonProperty("alt") double alt) {
            this.lat = lat;
            this.lon = lon;
            this.alt = alt;
        }

        public Cartesian toEcef() {
            // TODO: test

            // WGS-84 geodetic constants
            final double a = 6378137.0; // WGS-84 Earth semimajor axis (m)
            final double b = 6356752.314245; // Derived Earth semiminor axis (m)
            final double f = (a - b) / a; // Ellipsoid Flatness

            double e_sq = f * (2 - f); // Square of Eccentricity

            double lambda = Math.toRadians(lat);
            double phi = Math.toRadians(lon);
            double s = Math.sin(lambda);
            double N = a / Math.sqrt(1 - e_sq * s * s);

            double sin_lambda = Math.sin(lambda);
            double cos_lambda = Math.cos(lambda);
            double cos_phi = Math.cos(phi);
            double sin_phi = Math.sin(phi);

            return new Cartesian(
                    (alt + N) * cos_lambda * cos_phi,
                    (alt + N) * cos_lambda * sin_phi,
                    (alt + (1 - e_sq) * N) * sin_lambda);
        }

        @Override
        public String toString() {
            return "{lat=" + lat + ", lon=" + lon + ", alt=" + alt + "}";
        }
    }

    @SuppressWarnings("Duplicates")
    public static class Cartesian {

        public double x;
        public double y;
        public double z;

        @JsonCreator
        public Cartesian(
                @JsonProperty("x") double x, @JsonProperty("y") double y,
                @JsonProperty("z") double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Cartesian toNed(double lat0, double lon0, double alt0) {
            // TODO: test

            // WGS-84 geodetic constants
            final double a = 6378137.0; // WGS-84 Earth semimajor axis (m)
            final double b = 6356752.314245; // Derived Earth semiminor axis (m)
            final double f = (a - b) / a; // Ellipsoid Flatness

            double e_sq = f * (2 - f); // Square of Eccentricity

            double lambda = Math.toRadians(lat0);
            double phi = Math.toRadians(lon0);
            double s = Math.sin(lambda);
            double N = a / Math.sqrt(1 - e_sq * s * s);

            double sin_lambda = Math.sin(lambda);
            double cos_lambda = Math.cos(lambda);
            double cos_phi = Math.cos(phi);
            double sin_phi = Math.sin(phi);

            double x0 = (alt0 + N) * cos_lambda * cos_phi;
            double y0 = (alt0 + N) * cos_lambda * sin_phi;
            double z0 = (alt0 + (1 - e_sq) * N) * sin_lambda;

            double xd, yd, zd;
            xd = x - x0;
            yd = y - y0;
            zd = z - z0;

            // This is the matrix multiplication
            x = -cos_phi * sin_lambda * xd - sin_lambda * sin_phi * yd + cos_lambda * zd; // xNorth
            y = -sin_phi * xd + cos_phi * yd; // yEast
            z = -cos_lambda * cos_phi * xd + cos_lambda * sin_phi * yd + sin_lambda * zd; // zDown

            return this;
        }

        @Override
        public String toString() {
            return "{x=" + x + ", y=" + y + ", z=" + z + "}";
        }
    }

    /**
     * DEBUG
     */
    private String dummyJson(int num) {
        StringBuilder payload = new StringBuilder();

        String dummyObject =
                "{\"origin\": {\"lat\": \"34.00000048\", \"lon\": \"-117.3335693\", \"alt\": \"251.702\"}, \"destination\": {\"lat\": \"34.00000048\", \"lon\": \"-117.3335693\", \"alt\": \"251.702\"}, \"reference\": {\"lat\": \"34.00000048\", \"lon\": \"-117.3335693\", \"alt\": \"251.702\"}, \"velocity\": {\"x\": \"1.5\", \"y\": \"1.5\", \"z\": \"0.5\"}}";

        for (int i = 1; i <= num; i++) {
            payload.append("{\"192.168.1.").append(i).append("\": ").append(dummyObject)
                    .append("}");

            if (i < num) {
                payload.append(",");
            }
        }

        return payload.toString();
    }


    /**
     * TEST
     */
    private long handleHandoffsTest(String message, long receiveTime) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, ApRelocationTest> oldApRelocations;
        try {
            oldApRelocations = objectMapper.readValue(message, new TypeReference<Map<String, ApRelocationTest>>() {});
        } catch (IOException e) {
            oldApRelocations = Collections.emptyMap();
            e.printStackTrace();
        }

        HashSet<InetAddress> agents = new HashSet<>(getAgents());
        HashSet<OdinClient> clients = new HashSet<>(getClients());

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(clients.size());

        // Convert Strings to InetAddresses and get the longest flight time
        Map<InetAddress, ApRelocationTest> apRelocations = new HashMap<>();
        long longestFlight = 0L;

        for (Map.Entry<String, ApRelocationTest> entry : oldApRelocations.entrySet()) {
            String agent = entry.getKey();
            ApRelocationTest apRelocation = entry.getValue();

            InetAddress agentAddress = null;
            try {
                agentAddress = InetAddress.getByName(agent);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            if (agentAddress != null) {
                apRelocations.put(agentAddress, apRelocation);

                long delayMillis = 2 * (apRelocation.handoffTime.getTime() - apRelocation.startTime.getTime());
                if (delayMillis > longestFlight) {
                    longestFlight = delayMillis;
                }
            }
        }

        // Foreach client in client_set
        for (OdinClient client : clients) {
            // Set future agent as the first AP without association to this client (2 agents only)
            InetAddress currentAgent = client.getLvap().getAgent().getIpAddress();
            InetAddress futureAgent = null;

            for (InetAddress agent : agents) {
                if (agent != currentAgent) {
                    futureAgent = agent;
                    break;
                }
            }

            // Check if client needs a handoff
            if (futureAgent == null || futureAgent.equals(currentAgent)) {
                continue;
            }

            long delay = apRelocations.get(futureAgent).handoffTime.getTime() - apRelocations.get(futureAgent).startTime.getTime();

            if (delay > 0) {
                // Schedule handoff
                scheduler.schedule(
                        new RunHandoffClientToAp(client.getMacAddress(), futureAgent),
                        delay - (System.nanoTime() - receiveTime) / 1000000,
                        TimeUnit.MILLISECONDS);
            }
        }

        // Gracefully shutdown the scheduler (i.e., still finishes old tasks)
        scheduler.shutdown();


        return longestFlight;
    }

    /**
     * TEST
     */
    public static String dummyJsonTest(int num) {
        StringBuilder payload = new StringBuilder();

        String dummyObject = "{\"startTime\": \"2019-06-03 02:03:25.940\", \"handoffTime\": \"2019-06-03 02:03:35.321\"}";

        for (int i = 1; i <= num; i++) {
            payload.append("{\"192.168.1.").append(i).append("\": ").append(dummyObject)
                    .append("}");

            if (i < num) {
                payload.append(",");
            }
        }

        return payload.toString();
    }

    /**
     * TEST
     */
    public static class ApRelocationTest {
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "Portugal")
        private Timestamp startTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "Portugal")
        private Timestamp handoffTime;

        @JsonCreator
        public ApRelocationTest(
                @JsonProperty("startTime") Timestamp startTime,
                @JsonProperty("handoffTime") Timestamp handoffTime
        ) {
            this.startTime = startTime;
            this.handoffTime = handoffTime;
        }

        @Override
        public String toString() {
            return "{"
                    + "\n  startTime = "
                    + startTime + ","
                    + "\n  stopTime = "
                    + handoffTime
                    + "\n}";
        }
    }
}
