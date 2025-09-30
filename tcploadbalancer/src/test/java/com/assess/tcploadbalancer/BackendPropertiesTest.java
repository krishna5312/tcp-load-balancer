package com.assess.tcploadbalancer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test") // will use application-test.properties if present
class BackendPropertiesTest {

    @Autowired
    private BackendProperties backendProperties;

    @Test
    void propertiesAreBoundFromApplicationProperties() {
        assertThat(backendProperties.getListenPort()).isEqualTo(9090);

        List<Backend> backends = backendProperties.getBackends();
        assertThat(backends).hasSize(2);

        assertThat(backends.get(0).getHost()).isEqualTo("127.0.0.1");
        assertThat(backends.get(0).getPort()).isEqualTo(9001);

        assertThat(backends.get(1).getHost()).isEqualTo("127.0.0.1");
        assertThat(backends.get(1).getPort()).isEqualTo(9002);
    }
}

