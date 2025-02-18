package com.microservice.orderservice.ordercontrollerTest;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest({"server.port=0"})
@AutoConfigureMockMvc
@EnableConfigurationProperties
public class OrderControllerTest {

}
