package com.assess.tcploadbalancer.backend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class BackendEchoServer {
	public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: java BackendEchoServer <port>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Backend echo server running on port " + port);
            while (true) {
                Socket client = server.accept();
                new Thread(() -> handleClient(client, port)).start();
            }
        }
    }

    private static void handleClient(Socket client, int port) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
            String line;
            while ((line = in.readLine()) != null) {
                out.println("Backend " + port + " says: " + line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
