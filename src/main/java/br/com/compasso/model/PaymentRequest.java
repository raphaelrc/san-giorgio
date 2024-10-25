package br.com.compasso.model;

import java.util.List;

import lombok.Data;

@Data
public class PaymentRequest {
	private String sellerCode;
    private List<Payment> payments;
}
