package com.assess.tcploadbalancer;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class LoadBalancerStatusController {

    private final List<BackendStatus> backendStatuses;

    public LoadBalancerStatusController(List<BackendStatus> backendStatuses) {
        this.backendStatuses = backendStatuses;
    }

    @GetMapping("/lb/status")
    public List<String> status() {
        return backendStatuses.stream()
                .map(BackendStatus::toString)
                .collect(Collectors.toList());
    }
}

