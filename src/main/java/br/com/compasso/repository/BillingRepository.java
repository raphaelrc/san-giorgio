package br.com.compasso.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import br.com.compasso.model.Billing;

@Repository
public interface BillingRepository {
	Optional<Billing> findByCode(String code);
}
