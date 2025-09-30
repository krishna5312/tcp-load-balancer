package com.assess.tcploadbalancer.backend;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "lb")
public class BackendProperties {
    private int listenPort;
    private List<Backend> backends;

    public int getListenPort() { return listenPort; }
    public void setListenPort(int listenPort) { this.listenPort = listenPort; }

    public List<Backend> getBackends() { return backends; }
    public void setBackends(List<Backend> backends) { this.backends = backends; }
}

