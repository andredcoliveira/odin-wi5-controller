package net.floodlightcontroller.odin.applications;

import java.util.concurrent.atomic.AtomicReference;
import net.floodlightcontroller.odin.master.OdinApplication;
import net.floodlightcontroller.odin.master.OdinClient;
import net.floodlightcontroller.util.MACAddress;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Application that handles predictive mobility management in flying networks
 *
 * @author André Oliveira <andreduartecoliveira@gmail.com>
 */
public class FlyingNetworkManager extends OdinApplication {

    // TODO: Implementar versão de teste (JSON com data exata + tempo para handoff)
    String VERSION = "TEST"; // "TEST" or "PRODUCTION"

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

        long receiveTime;

        // infinite loop if server socket is valid (i.e., listening on port 6666)
        while (serverSocket != null) {
            String message = null;
            try {
                clientSocket = serverSocket.accept();  // blocks until someone connects
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                message = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            receiveTime = System.nanoTime();

            if (isPossiblyJson(message)) {
                setApplicationState("SmartApSelection", State.HALTING);
                synchronized (getLock()) {
                    while (!getApplicationState("SmartApSelection").equals(State.HALTING)) {
                        try {
                            getLock().wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    long operationTime = handleHandoffs(message, receiveTime);

                    ScheduledExecutorService scheduler = Executors
                            .newSingleThreadScheduledExecutor();

                    // Resume SmartApSelection() after the APs are done moving around
                    scheduler.schedule(
                            () -> {
                                setApplicationState("SmartApSelection", State.RUNNING);
                                getLock().notifyAll();
                            },
                            Math.round(operationTime * 1000)
                                    - (System.nanoTime() - receiveTime) / 1000000,
                            TimeUnit.MILLISECONDS
                    );
                    scheduler.shutdown();
                }
            }
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

    private boolean isPossiblyJson(String message) {
        return message != null && !message.trim().isEmpty() && (
                (message.startsWith("[") && message.endsWith("]")) || (message.startsWith("{")
                        && message.endsWith("}")));
    }

    private long handleHandoffs(String message, long receiveTime) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, ApRelocation> apRelocations;
        try {
            apRelocations = objectMapper
                    .readValue(message, new TypeReference<Map<String, ApRelocation>>() {
                    });
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

        AtomicReference<Double> longestFlight = new AtomicReference<>();

        apRelocations.forEach((agent, apRelocation) -> {
            InetAddress agentAddress = null;
            try {
                agentAddress = InetAddress.getByName(agent);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            if (agentAddress != null) {
                Cartesian originNed = apRelocation.origin.toEcef().toNed(
                        apRelocation.reference.lat,
                        apRelocation.reference.lon,
                        apRelocation.reference.alt
                );
                agentCoordinatesNedOrigin.put(agentAddress, originNed);

                Cartesian destinationNed = apRelocation.destination.toEcef().toNed(
                        apRelocation.reference.lat,
                        apRelocation.reference.lon,
                        apRelocation.reference.alt
                );
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
                Double distanceSqr = distanceSquared(
                        clientCoordinatesNed.get(client.getMacAddress()),
                        agentCoordinatesNedDestination.get(agent)
                );

                if (distanceSqr < min) {
                    min = distanceSqr;
                    futureAgent = agent;
                }
            }

            // consider client handled
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
                    v1.x * v1.x + v1.y * v1.y + v1.z * v1.z
                            - v2.x * v2.x - v2.y * v2.y - v2.z * v2.z,
                    2 * (v1.x * (p1.x - p.x)
                            + v1.y * (p1.y - p.y)
                            + v1.z * (p1.z - p.z)
                            - v2.x * (p2.x - p.x)
                            - v2.y * (p2.y - p.y)
                            - v2.z * (p2.z - p.z)),
                    p1.x * p1.x + p1.y * p1.y + p1.z * p1.z
                            - p2.x * p2.x - p2.y * p2.y - p2.z * p2.z
                            - 2 * (p.x * (p1.x - p2.x)
                            + p.y * (p1.y - p2.y)
                            + p.z * (p1.z - p2.z))
            ); // seconds

            if (delay != null) {
                // Schedule handoff
                scheduler.schedule(
                        new HandoffRunnable(client.getMacAddress(), futureAgent),
                        Math.round(delay * 1000) - (System.nanoTime() - receiveTime) / 1000000,
                        TimeUnit.MILLISECONDS
                );
            }
        }

        // Gracefully shutdown the scheduler: no new tasks will be accepted
        scheduler.shutdown();

        return Math.round(longestFlight.get() * 1000) - (System.nanoTime() - receiveTime) / 1000000;
    }

    public class HandoffRunnable implements Runnable {

        private final MACAddress staHwAddr;
        private final InetAddress futureAgent;

        public HandoffRunnable(MACAddress staHwAddr, InetAddress futureAgent) {
            this.staHwAddr = staHwAddr;
            this.futureAgent = futureAgent;
        }

        @Override
        public void run() {
            handoffClientToAp(staHwAddr, futureAgent);
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
                @JsonProperty("velocity") Cartesian velocity
        ) {
            this.origin = origin;
            this.destination = destination;
            this.reference = reference;
            this.velocity = velocity;
        }

        @Override
        public String toString() {
            return "{\n" +
                    "\n  origin=" + origin +
                    "\n  destination=" + destination +
                    "\n  reference=" + reference +
                    "\n  velocity=" + velocity +
                    "\n}";
        }
    }

    @SuppressWarnings("Duplicates")
    public static class GpsCoordinates {

        double lat;  // degrees
        double lon;  // degrees
        double alt;

        @JsonCreator
        public GpsCoordinates(
                @JsonProperty("lat") double lat,
                @JsonProperty("lon") double lon,
                @JsonProperty("alt") double alt
        ) {
            this.lat = lat;
            this.lon = lon;
            this.alt = alt;
        }

        public Cartesian toEcef() {
            //TODO: test

            // WGS-84 geodetic constants
            final double a = 6378137.0;         // WGS-84 Earth semimajor axis (m)
            final double b = 6356752.314245;    // Derived Earth semiminor axis (m)
            final double f = (a - b) / a;       // Ellipsoid Flatness

            double e_sq = f * (2 - f);           // Square of Eccentricity

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
                    (alt + (1 - e_sq) * N) * sin_lambda
            );
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
                @JsonProperty("x") double x,
                @JsonProperty("y") double y,
                @JsonProperty("z") double z
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Cartesian toNed(double lat0, double lon0, double alt0) {
            //TODO: test

            // WGS-84 geodetic constants
            final double a = 6378137.0;         // WGS-84 Earth semimajor axis (m)
            final double b = 6356752.314245;    // Derived Earth semiminor axis (m)
            final double f = (a - b) / a;       // Ellipsoid Flatness

            double e_sq = f * (2 - f);          // Square of Eccentricity

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
            x = -cos_phi * sin_lambda * xd - sin_lambda * sin_phi * yd + cos_lambda * zd;  // xNorth
            y = -sin_phi * xd + cos_phi * yd;  // yEast
            z = -cos_lambda * cos_phi * xd + cos_lambda * sin_phi * yd + sin_lambda * zd;  // zDown

            return this;
        }

        @Override
        public String toString() {
            return "{x=" + x + ", y=" + y + ", z=" + z + "}";
        }
    }

    public double distanceSquared(Cartesian p1, Cartesian p2) {
        return ((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y) + (p1.z - p2.z) * (
                p1.z
                        - p2.z));
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
        if ((answerPlus < 0 && answerMinus < 0) || (Double.isNaN(answerPlus) && Double
                .isNaN(answerMinus))) {
            return null;
        } else if (answerPlus > 0 && answerMinus > 0) {
            return Math.min(answerPlus, answerMinus);
        }

        // return that which is > 0
        return (answerPlus > answerMinus) ? answerPlus : answerMinus;
    }

    private String dummyJson(int num) {
        StringBuilder payload = new StringBuilder();

        String dummyObject = "{\"origin\": {\"lat\": \"34.00000048\", \"lon\": \"-117.3335693\", \"alt\": \"251.702\"}, \"destination\": {\"lat\": \"34.00000048\", \"lon\": \"-117.3335693\", \"alt\": \"251.702\"}, \"reference\": {\"lat\": \"34.00000048\", \"lon\": \"-117.3335693\", \"alt\": \"251.702\"}, \"velocity\": {\"x\": \"1.5\", \"y\": \"1.5\", \"z\": \"0.5\"}}";

        for (int i = 1; i <= num; i++) {
            payload.append("{\"192.168.1.").append(i).append("\": ").append(dummyObject)
                    .append("}");

            if (i < num) {
                payload.append(",");
            }
        }

        return payload.toString();
    }
}
