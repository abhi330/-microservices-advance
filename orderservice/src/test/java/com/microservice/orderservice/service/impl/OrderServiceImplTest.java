package com.microservice.orderservice.service.impl;
import com.microservice.orderservice.exception.CustomException;
import com.microservice.orderservice.external.client.PaymentService;
import com.microservice.orderservice.external.client.ProductService;
import com.microservice.orderservice.model.Order;
import com.microservice.orderservice.payload.request.OrderRequest;
import com.microservice.orderservice.payload.request.PaymentRequest;
import com.microservice.orderservice.payload.response.OrderResponse;
import com.microservice.orderservice.payload.response.PaymentResponse;
import com.microservice.orderservice.payload.response.ProductResponse;
import com.microservice.orderservice.repository.OrderRepository;
import com.microservice.orderservice.service.OrderService;
import com.microservice.orderservice.utils.PaymentMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.time.Instant;
import java.util.Optional;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;


@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = {"spring.cloud.config.enabled=false"}
)

class OrderServiceImplTest {
    @Mock
    private OrderRepository orderRepository;
     @Mock
    private ProductService productService;
     @Mock
    private PaymentService paymentService;
     @Mock
    private RestTemplate restTemplate;

     @InjectMocks
     OrderService orderService =new OrderServiceImpl();

     @DisplayName("check get order is success or not")
     @Test
     void test_success(){
         Order order=getMockOrder();

         //mocking
         Mockito.when(orderRepository.findById(ArgumentMatchers.anyLong())).thenReturn(Optional.of(order));
         Mockito.when(restTemplate.getForObject("http://PRODUCT-SERVICE/product/" + order.getProductId(),
                         ProductResponse.class)).thenReturn(getMockProductResponse());
         Mockito.when(restTemplate
                 .getForObject("http://PAYMENT-SERVICE/payment/order/" + order.getId(), PaymentResponse.class)).thenReturn(getMockPaymentResponse());
         //actual
        OrderResponse orderResponse= orderService.getOrderDetails(1);
         //verification

         Mockito.verify(orderRepository,Mockito.times(1)).findById(ArgumentMatchers.anyLong());
         Mockito.verify(restTemplate,Mockito.times(1))
                 .getForObject("http://PRODUCT-SERVICE/product/" + order.getProductId(),ProductResponse.class);
         Mockito.verify(restTemplate,Mockito.times(1))
                 .getForObject("http://PAYMENT-SERVICE/payment/order/" + order.getId(), PaymentResponse.class);


         //assertion
        Assertions.assertNotNull(orderResponse);
         Assertions.assertEquals(order.getId(),orderResponse.getOrderId());

     }

     @DisplayName("test for get order details for failure case")
     @Test
     void test_failure(){

         Mockito.when(orderRepository.findById(ArgumentMatchers.anyLong()))
                 .thenReturn(Optional.ofNullable(null));
         CustomException exception=Assertions.assertThrows(CustomException.class,()->orderService.getOrderDetails(1));

         Assertions.assertEquals("NOT_FOUND",exception.getErrorCode());

         Assertions.assertEquals(404,exception.getStatus());

         Mockito.verify(orderRepository,Mockito.times(1))
                 .findById(ArgumentMatchers.anyLong());

     }
     @Test
     @DisplayName("testing_place_order_success")
     void testing_place_order(){
         OrderRequest orderRequest=getMockOrderRequest();
         Order order=getMockOrder();
         Mockito.when(orderRepository.save(ArgumentMatchers.any(Order.class))).thenReturn(order);
         Mockito.when(productService.reduceQuantity(ArgumentMatchers.anyLong(),ArgumentMatchers.anyLong()))
                 .thenReturn(new ResponseEntity<Void>(HttpStatus.OK));
         Mockito.when(paymentService.doPayment(ArgumentMatchers.any(PaymentRequest.class)))
                 .thenReturn(new ResponseEntity<Long>(HttpStatus.OK));

         Long orderId=orderService.placeOrder(orderRequest);
         Mockito.verify(orderRepository,Mockito.times(2)).save(ArgumentMatchers.any());
         Mockito.verify(productService,Mockito.times(1)).reduceQuantity(ArgumentMatchers.anyLong(),ArgumentMatchers.anyLong());
         Mockito.verify(paymentService,Mockito.times(1)).doPayment(ArgumentMatchers.any());

         Assertions.assertNotNull(order);
         Assertions.assertEquals(order.getId(),orderId);



     }
     @Test
     @DisplayName("testin_failure_case")

     void place_order_failure_case(){
         OrderRequest orderRequest=getMockOrderRequest();
         Order order=getMockOrder();
         Mockito.when(orderRepository.save(ArgumentMatchers.any(Order.class))).thenReturn(order);
         Mockito.when(productService.reduceQuantity(ArgumentMatchers.anyLong(),ArgumentMatchers.anyLong()))
                 .thenReturn(new ResponseEntity<Void>(HttpStatus.OK));
         Mockito.when(paymentService.doPayment(ArgumentMatchers.any(PaymentRequest.class))).thenThrow(new RuntimeException());

         Long orderId=orderService.placeOrder(orderRequest);
         Mockito.verify(orderRepository,Mockito.times(2)).save(ArgumentMatchers.any());
         Mockito.verify(productService,Mockito.times(1)).reduceQuantity(ArgumentMatchers.anyLong(),ArgumentMatchers.anyLong());
         Mockito.verify(paymentService,Mockito.times(1)).doPayment(ArgumentMatchers.any());

         Assertions.assertNotNull(order);
         Assertions.assertEquals(order.getId(),orderId);

     }

    private OrderRequest getMockOrderRequest() {
         return  OrderRequest.builder()
                 .paymentMode(PaymentMode.CASH)
                 .productId(1)
                 .quantity(100)
                 .totalAmount(400)
                 .build();
    }

    private PaymentResponse getMockPaymentResponse() {
         return PaymentResponse.builder()
                 .orderId(1)
                 .paymentId(1)
                 .paymentDate(Instant.now())
                 .amount(100)
                 .paymentMode(PaymentMode.CASH)
                 .status("ACCEPTED").build();
    }

    private ProductResponse getMockProductResponse() {
         return  ProductResponse.builder().productId(2).productName("boat")
                 .quantity(100)
                 .price(200).build();

    }

    private Order getMockOrder() {
         Order order=Order.builder()
                 .orderStatus("PLACED")
                 .orderDate(Instant.now())
                 .id(1)
                 .amount(100)
                 .quantity(100)
                 .productId(1).build();
         return order;
    }


}