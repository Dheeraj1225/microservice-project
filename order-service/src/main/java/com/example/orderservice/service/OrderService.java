package com.example.orderservice.service;

import com.example.orderservice.dto.OrderLineItemsDto;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderLineItems;
import com.example.orderservice.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        // TODO: Update this URL once your inventory service is deployed to Fargate!
        // To bypass this check temporarily for cloud testing, you can hardcode: Boolean isInStock = true;
        Boolean isInStock = webClientBuilder.build().get()
                .uri("http://inventory-service:8083/api/inventory/" + orderLineItems.get(0).getSkuCode())
                .retrieve()
                .bodyToMono(Boolean.class)
                .block();

        if (Boolean.TRUE.equals(isInStock)) {
            orderRepository.save(order);
        } else {
            throw new IllegalArgumentException("Product is not in stock, please try again later");
        }
    }

    // New GET method to retrieve all orders cleanly using the OrderResponse DTO
    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAll();

        return orders.stream()
                .map(this::mapToOrderResponse)
                .toList();
    }

    private OrderResponse mapToOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderLineItemsList(order.getOrderLineItemsList().stream()
                        .map(this::mapToLineItemsDto)
                        .toList())
                .build();
    }

    private OrderLineItemsDto mapToLineItemsDto(OrderLineItems orderLineItems) {
        OrderLineItemsDto dto = new OrderLineItemsDto();
        dto.setId(orderLineItems.getId());
        dto.setSkuCode(orderLineItems.getSkuCode());
        dto.setPrice(orderLineItems.getPrice());
        dto.setQuantity(orderLineItems.getQuantity());
        return dto;
    }

    private OrderLineItems mapToDto(OrderLineItemsDto dto) {
        OrderLineItems items = new OrderLineItems();
        items.setPrice(dto.getPrice());
        items.setQuantity(dto.getQuantity());
        items.setSkuCode(dto.getSkuCode());
        return items;
    }
}