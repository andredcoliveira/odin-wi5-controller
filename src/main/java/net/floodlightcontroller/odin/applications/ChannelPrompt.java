package net.floodlightcontroller.odin.applications;

import net.floodlightcontroller.odin.master.OdinApplication;

import java.net.InetAddress;
import java.util.Scanner;

public class ChannelPrompt extends OdinApplication {

    // IMPORTANT: this application only works if all the agents in the
    //poolfile are activated before 15 seconds.
    // Otherwise, the application looks for an object that does not exist
    //and gets stopped

    private InetAddress apAddr;
    private int channel;

    private Scanner in = new Scanner(System.in);

    @Override public void run() {

        try {
            Thread.sleep(15000); // Wait 15 seconds to start
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        while (true) {
            for (InetAddress agentAddr : getAgents()) {
                apAddr = agentAddr;
                channel = getChannelFromAgent(apAddr);
                System.out.println(
                        "[ChannelAssignment] AP " + apAddr + " in channel: "
                        + channel);
                System.out.print("[ChannelAssignment] Select channel for AP "
                                 + apAddr + ":");
                channel = in.nextInt();
                setChannelToAgent(apAddr, channel);
            }
        }
    }
}
