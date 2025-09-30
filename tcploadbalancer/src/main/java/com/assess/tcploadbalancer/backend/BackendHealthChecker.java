package com.assess.tcploadbalancer.backend;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class BackendHealthChecker implements Runnable {
    private final List<BackendStatus> backendStatuses;
    private final int checkIntervalMs;

    public BackendHealthChecker(List<BackendStatus> backendStatuses, int checkIntervalMs) {
        this.backendStatuses = backendStatuses;
        this.checkIntervalMs = checkIntervalMs;
    }

    @Override
    public void run() {
        while (true) {
            for (BackendStatus status : backendStatuses) {
                boolean alive = ping(status.getBackend());
                status.setAlive(alive);
            }
            try {
                Thread.sleep(checkIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private boolean ping(Backend backend) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(backend.getHost(), backend.getPort()), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

