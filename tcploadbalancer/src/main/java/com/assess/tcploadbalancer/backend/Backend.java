package com.assess.tcploadbalancer.backend;

public class Backend {
    private String host;
    private int port;

    public Backend() {}
    public Backend(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}

