package com.assess.tcploadbalancer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LoadBalancerStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void statusEndpointReturnsBackends() throws Exception {
        mockMvc.perform(get("/lb/status"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("127.0.0.1:9001")))
                .andExpect(content().string(containsString("127.0.0.1:9002")));
    }
}
