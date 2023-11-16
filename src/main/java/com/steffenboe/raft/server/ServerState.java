package com.steffenboe.raft.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

interface ServerState {

    public boolean processMessage(BufferedReader in, PrintWriter out) throws IOException;
}