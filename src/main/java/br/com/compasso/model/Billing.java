package br.com.compasso.model;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class Billing {
	private String code;
    private BigDecimal originalAmount;
}
