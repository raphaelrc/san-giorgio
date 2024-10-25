package br.com.compasso.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import br.com.compasso.model.Seller;

@Repository
public interface SellerRepository {
	Optional<Seller> findByCode(String code);
}
