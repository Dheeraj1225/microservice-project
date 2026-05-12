package com.example.orderservice.service;

import com.example.orderservice.dto.OrderLineItemsDto;
import com.example.orderservice.dto.OrderRequest;
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
    private final WebClient.Builder webClientBuilder; // Inject the builder

    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        Boolean isInStock = webClientBuilder.build().get()
                .uri("http://inventory-service:8083/api/inventory/" + orderLineItems.get(0).getSkuCode())
                .retrieve()
                .bodyToMono(Boolean.class)
                .block(); // .block() makes it synchronous

        if(Boolean.TRUE.equals(isInStock)) {
            orderRepository.save(order);
        } else {
            throw new IllegalArgumentException("Product is not in stock, please try again later");
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto dto) {
        OrderLineItems items = new OrderLineItems();
        items.setPrice(dto.getPrice());
        items.setQuantity(dto.getQuantity());
        items.setSkuCode(dto.getSkuCode());
        return items;
    }
}
