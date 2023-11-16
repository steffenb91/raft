package com.steffenboe.raft.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Server that listens for incoming requests.
 */
public class Server {

    private final Integer[] ports;
    private ServerSocket serverSocket;
    private int index = 0;
    private Thread thread;

    private ServerState state = new Follower();

    public Server(Integer[] ports) {
        this.ports = ports;
    }

    public void start(){
        this.thread = new Thread(this::listen);
        thread.start();
    }
    /**
     * Server listens for Client-Connections and responds to requests.
     */
    private void listen() {
        System.out.println("Trying startup on port " + ports[index] + "...");
        try (ServerSocket socket = new ServerSocket(ports[index])) {
            this.serverSocket = socket;
            System.out.println("Server is up and running on port " + serverSocket.getLocalPort());
            while (true) {
                acceptConnection(serverSocket);
            }
        } catch (BindException e) {
            index++;
            listen();

        } catch (NumberFormatException | IOException e) {
            System.err.println(String.format("Unrecoverable error: %s, shutting down.", e.getMessage()));
            throw new RuntimeException(e);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("No available port found in given list, starting server failed.");
        }
    }

    private void acceptConnection(ServerSocket serverSocket) throws IOException {
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connection established.");
        Thread.ofVirtual().start(() -> {
            try (PrintWriter out = getPrintWriter(clientSocket); BufferedReader in = getBufferedReader(clientSocket);) {
                boolean result = state.processMessage(in, out);
                if(!result){
                    out.println("closing connection");
                    System.out.println("Closing client connection.");
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private BufferedReader getBufferedReader(Socket clientSocket) throws IOException {
        return new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
    }

    private PrintWriter getPrintWriter(Socket clientSocket) throws IOException {
        return new PrintWriter(clientSocket.getOutputStream(), true);
    }

    public int getPort() {
        return ports[index];
    }

    public void stop() {
        thread.interrupt();
    }

    public ServerState state() {
        return state;
    }
}