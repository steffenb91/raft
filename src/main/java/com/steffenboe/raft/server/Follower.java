/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.steffenboe.raft.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;

class Follower implements ServerState {

    private final Server server;

    private long electionTimeout = 3L;
    private String votedFor;
    private Thread heartbeatWait;

    public Follower(Server server) {
        this.server = server;
    }

    public Follower(Server server, long electionTimeout) {
        this(server);
        this.electionTimeout = electionTimeout;
    }

    @Override
    public synchronized boolean processMessage(BufferedReader in, PrintWriter out) throws IOException {
        Message message = new Message(in.readLine());
        if (message.isAppendEntryMessage()) {
            if (!message.isFromLeader()) {
                return false;
            }
            processHeartbeat();
            return true;
        }
        if (message.isRequestVoteMessage()) {
            if(votedFor == null || votedFor.isEmpty()){
                votedFor = message.candidateId();
                System.out.println("Voting for candidate with id: " + votedFor);
                out.println("true");
            } else {
                out.println("false");
            }
            
            return true;
        }
        return false;
    }

    private Thread waitForHeartbeat() {
        return Thread.ofVirtual().start(() -> {
            try {
                System.out.println("Waiting for next heartbeat for " + electionTimeout + "s");
                waitForTimeout();
                System.out.println("No heartbeat received, notifiying election timeout...");
                server.onNewElection();
            } catch (InterruptedException ex) {}
        });
    }

    private void waitForTimeout() throws InterruptedException {
        Thread.sleep(Duration.ofSeconds(electionTimeout));
    }

    private void processHeartbeat() {
        System.out.println("Received heartbeat...");
        heartbeatWait.interrupt();
        heartbeatWait = waitForHeartbeat();
    }

    @Override
    public void initialize() {
        this.heartbeatWait = waitForHeartbeat();
    }

}
