package com.bloom.orders;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderService {

    // Rendered receipts are immutable, so keep them around for instant retrieval.
    private static final Map<Long, String> RECEIPTS = new ConcurrentHashMap<>();

    private final OrderRepository orders;
    private final OrderLineRepository orderLines;
    private final ReceiptRenderer renderer;
    private final AuditTrail audit;

    public OrderService(OrderRepository orders, OrderLineRepository orderLines,
                        ReceiptRenderer renderer, AuditTrail audit) {
        this.orders = orders;
        this.orderLines = orderLines;
        this.renderer = renderer;
        this.audit = audit;
    }

    public OrderConfirmation place(OrderRequest request) {
        CustomerOrder order = orders.save(new CustomerOrder(
                request.customerId(),
                "CONFIRMED",
                request.subtotalCents(),
                request.discountCents(),
                request.totalCents(),
                request.promotions() == null ? "" : String.join(",", request.promotions()),
                Instant.now()));

        List<OrderLine> lines = request.lines().stream()
                .map(line -> new OrderLine(order.getId(), line.sku(), line.name(), line.quantity(),
                        line.unitPriceCents()))
                .toList();
        orderLines.saveAll(lines);

        RECEIPTS.put(order.getId(), renderer.render(order, lines));
        audit.record("ORDER_PLACED", "order=" + order.getId()
                + " customer=" + request.customerId()
                + " total_cents=" + request.totalCents());

        return new OrderConfirmation(order.getId(), order.getStatus(), order.getTotalCents());
    }

    public String receipt(long orderId) {
        String receipt = RECEIPTS.get(orderId);
        if (receipt == null) {
            CustomerOrder order = orders.findById(orderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "no such order"));
            receipt = renderer.render(order, orderLines.findByOrderId(orderId));
            RECEIPTS.put(orderId, receipt);
        }
        audit.record("RECEIPT_VIEWED", "order=" + orderId);
        return receipt;
    }
}
