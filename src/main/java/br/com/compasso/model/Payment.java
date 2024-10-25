package br.com.compasso.model;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class Payment {
	private String billingCode;
    private BigDecimal amount;
    private PaymentStatus status;
}
