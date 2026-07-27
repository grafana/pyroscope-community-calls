package com.bloom.orders;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReceiptRenderer {

    public String render(CustomerOrder order, List<OrderLine> lines) {
        String receipt = "";
        receipt += "+----------------------------------------------------+\n";
        receipt += "|                 BLOOM  FLOWER  SHOP                |\n";
        receipt += "|               www.bloom.example.com                |\n";
        receipt += "+----------------------------------------------------+\n";
        receipt += String.format("Order:    #%d%n", order.getId());
        receipt += String.format("Customer: %s%n", order.getCustomerId());
        receipt += String.format("Placed:   %s%n", order.getCreatedAt());
        receipt += "------------------------------------------------------\n";
        for (OrderLine line : lines) {
            receipt += String.format("%-30s %2d x %9s %10s%n",
                    shorten(line.getName()),
                    line.getQuantity(),
                    money(line.getUnitPriceCents()),
                    money(line.getQuantity() * line.getUnitPriceCents()));
        }
        receipt += "------------------------------------------------------\n";
        receipt += String.format("%-42s %11s%n", "Subtotal", money(order.getSubtotalCents()));
        receipt += String.format("%-42s -%10s%n", "Discounts", money(order.getDiscountCents()));
        if (order.getPromotions() != null && !order.getPromotions().isBlank()) {
            for (String promotion : order.getPromotions().split(",")) {
                receipt += String.format("  applied: %s%n", promotion);
            }
        }
        receipt += String.format("%-42s %11s%n", "TOTAL", money(order.getTotalCents()));
        receipt += "------------------------------------------------------\n";
        receipt += "Flowers are packed fresh on the day of delivery.\n";
        receipt += "Keep stems in clean water and out of direct sun.\n";
        receipt += "Thank you for shopping with Bloom!\n";
        return receipt;
    }

    private static String shorten(String name) {
        if (name == null) {
            return "";
        }
        return name.length() <= 30 ? name : name.substring(0, 27) + "...";
    }

    private static String money(long cents) {
        return String.format("$%,.2f", cents / 100.0);
    }
}
