package com.assess.tcploadbalancer;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TcploadbalancerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TcploadbalancerApplication.class, args);
	}
	
	@Bean
	public List<BackendStatus> backendStatuses(BackendProperties properties) {
	    return properties.getBackends().stream()
	            .map(BackendStatus::new)
	            .toList();
	}
	
	 @Bean
	 CommandLineRunner startTcpLb(BackendProperties properties, List<BackendStatus> backendStatuses) {
		    return args -> {
		        new Thread(() -> {
		            try {
		                TcpLoadBalancer lb = new TcpLoadBalancer(
		                        properties.getListenPort(),
		                        backendStatuses
		                );
		                lb.start();
		            } catch (Exception e) {
		                e.printStackTrace();
		            }
		        }, "tcp-lb-thread").start();

		        new Thread(new BackendHealthChecker(backendStatuses, 2000), "lb-health-checker").start();
		    };
		}

}
