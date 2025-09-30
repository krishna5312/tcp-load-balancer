package com.assess.tcploadbalancer;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.assess.tcploadbalancer.backend.Backend;
import com.assess.tcploadbalancer.backend.BackendProperties;
import com.assess.tcploadbalancer.backend.BackendStatus;

public class TcpLoadBalancerTest {

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newCachedThreadPool();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void backendPropertiesLoadCorrectly() {
        BackendProperties props = new BackendProperties();
        props.setListenPort(9090);
        props.setBackends(List.of(new Backend("127.0.0.1", 9001)));

        assertThat(props.getListenPort()).isEqualTo(9090);
        assertThat(props.getBackends()).hasSize(1);
        assertThat(props.getBackends().get(0).toString()).isEqualTo("127.0.0.1:9001");
    }

    @Test
    void loadBalancerChoosesBackendsRoundRobin() {
    	 List<BackendStatus> backends = Arrays.asList(
                 new BackendStatus(new Backend("127.0.0.1", 9001)),
                 new BackendStatus(new Backend("127.0.0.1", 9002))
         );
        TcpLoadBalancer lb = new TcpLoadBalancer(0, backends); // port=0 → not actually listening

        try {
            var method = TcpLoadBalancer.class.getDeclaredMethod("chooseBackend");
            method.setAccessible(true);

            Backend b1 = (Backend) method.invoke(lb);
            Backend b2 = (Backend) method.invoke(lb);
            Backend b3 = (Backend) method.invoke(lb);

            assertThat(b1.getPort()).isEqualTo(9001);
            assertThat(b2.getPort()).isEqualTo(9002);
            assertThat(b3.getPort()).isEqualTo(9001); // round robin wraps
        } catch (Exception e) {
            Assertions.fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    void tcpProxyForwardsDataToBackend() throws Exception {
        int backendPort;
        try (ServerSocket backendServer = new ServerSocket(0)) {
            backendPort = backendServer.getLocalPort();

            // start backend echo server
            executor.submit(() -> {
                while (true) {
                    try (Socket s = backendServer.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                         PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
                        String line;
                        while ((line = in.readLine()) != null) {
                            out.println("Echo: " + line);
                        }
                    } catch (IOException ignored) {}
                }
            });

            // start LB
            int lbPort;
            try (ServerSocket test = new ServerSocket(0)) { lbPort = test.getLocalPort(); }
            TcpLoadBalancer lb = new TcpLoadBalancer(lbPort,
                    List.of(new BackendStatus(new Backend("127.0.0.1", backendPort))));
            executor.submit(lb::start);

            // connect client
            try (Socket client = new Socket("127.0.0.1", lbPort);
                 BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                 PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {

                out.println("hello");
                String response = in.readLine();
                assertThat(response).isEqualTo("Echo: hello");
            }
        }
    }

    @Test
    void loadBalancerAlternatesBetweenTwoBackends() throws Exception {
        // backend 1
        ServerSocket backend1 = new ServerSocket(0);
        int port1 = backend1.getLocalPort();
        executor.submit(() -> handleEchoServer(backend1, "B1"));

        // backend 2
        ServerSocket backend2 = new ServerSocket(0);
        int port2 = backend2.getLocalPort();
        executor.submit(() -> handleEchoServer(backend2, "B2"));

        // start LB
        int lbPort;
        try (ServerSocket test = new ServerSocket(0)) { lbPort = test.getLocalPort(); }
        TcpLoadBalancer lb = new TcpLoadBalancer(lbPort, List.of(
                new BackendStatus(new Backend("127.0.0.1", port1)),
                new BackendStatus(new Backend("127.0.0.1", port2))
        ));
        executor.submit(lb::start);

        // first client → B1
        String r1 = sendMessage(lbPort, "hi");
        // second client → B2
        String r2 = sendMessage(lbPort, "hi");

        assertThat(r1).contains("B1");
        assertThat(r2).contains("B2");
    }

    private void handleEchoServer(ServerSocket server, String tag) {
        try {
            while (true) {
                try (Socket s = server.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                     PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        out.println(tag + " Echo: " + line);
                    }
                }
            }
        } catch (IOException ignored) {}
    }

    private String sendMessage(int lbPort, String msg) throws IOException {
        try (Socket client = new Socket("127.0.0.1", lbPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
            out.println(msg);
            return in.readLine();
        }
    }
    
    @Test
    void loadBalancerClosesConnectionIfAllBackendsDown() throws Exception {
        BackendStatus status1 = new BackendStatus(new Backend("127.0.0.1", 12345));
        BackendStatus status2 = new BackendStatus(new Backend("127.0.0.1", 12346));
        status1.setAlive(false);
        status2.setAlive(false);

        int lbPort;
        try (ServerSocket test = new ServerSocket(0)) { lbPort = test.getLocalPort(); }
        TcpLoadBalancer lb = new TcpLoadBalancer(lbPort, List.of(status1, status2));
        executor.submit(lb::start);

        try (Socket client = new Socket("127.0.0.1", lbPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
            String response = in.readLine();
            assertThat(response).contains("503 Service Unavailable");
        }
    }

}

