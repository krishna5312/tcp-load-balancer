package com.assess.tcploadbalancer.backend;

public class BackendStatus {
    private final Backend backend;
    private volatile boolean alive;

    public BackendStatus(Backend backend) {
        this.backend = backend;
        this.alive = true;
    }

    public Backend getBackend() {
        return backend;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    @Override
    public String toString() {
        return backend.toString() + " - " + (alive ? "UP" : "DOWN");
    }
}

