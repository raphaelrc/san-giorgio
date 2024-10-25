package br.com.compasso.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.compasso.exception.NotFoundException;
import br.com.compasso.model.Billing;
import br.com.compasso.model.Payment;
import br.com.compasso.model.PaymentRequest;
import br.com.compasso.model.PaymentStatus;
import br.com.compasso.repository.BillingRepository;
import br.com.compasso.repository.SellerRepository;
import br.com.compasso.sqs.SqsClient;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

	private final SellerRepository sellerRepository;
	private final BillingRepository billingRepository;
	private final SqsClient sqsClient;

	@Value("${sqs.partial.queue.url}")
	private String partialQueueUrl;

	@Value("${sqs.total.queue.url}")
	private String totalQueueUrl;

	@Value("${sqs.overpaid.queue.url}")
	private String overpaidQueueUrl;

	public PaymentRequest processPayments(PaymentRequest request) throws JsonProcessingException, NotFoundException {
		// Validação do vendedor
		sellerRepository.findByCode(request.getSellerCode())
				.orElseThrow(() -> new NotFoundException("Vendedor não encontrado"));

		for (Payment payment : request.getPayments()) {
			// Validação do código da cobrança
			Billing billing = billingRepository.findByCode(payment.getBillingCode())
					.orElseThrow(() -> new NotFoundException("Código da cobrança não encontrado: " + payment.getBillingCode()));

			// Comparação dos valores
			Integer comparison = payment.getAmount().compareTo(billing.getOriginalAmount());

			if (comparison < 0) {
				payment.setStatus(PaymentStatus.PARCIAL);
				sendToQueue(partialQueueUrl, payment);
			} else if (comparison == 0) {
				payment.setStatus(PaymentStatus.TOTAL);
				sendToQueue(totalQueueUrl, payment);
			} else {
				payment.setStatus(PaymentStatus.EXCEDENTE);
				sendToQueue(overpaidQueueUrl, payment);
			}
		}

		return request;
	}

	private void sendToQueue(String queueUrl, Payment payment) throws JsonProcessingException {
		String messageBody = serialize(payment);
		sqsClient.sendMessage(queueUrl, messageBody);
	}

	private String serialize(Payment payment) throws JsonProcessingException {
		// Serialização do objeto Payment para JSON
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(payment);
	}

}
