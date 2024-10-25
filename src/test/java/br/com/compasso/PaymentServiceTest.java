package br.com.compasso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.compasso.exception.NotFoundException;
import br.com.compasso.model.Billing;
import br.com.compasso.model.Payment;
import br.com.compasso.model.PaymentRequest;
import br.com.compasso.model.PaymentStatus;
import br.com.compasso.model.Seller;
import br.com.compasso.repository.BillingRepository;
import br.com.compasso.repository.SellerRepository;
import br.com.compasso.service.PaymentService;
import br.com.compasso.sqs.SqsClient;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

	@Mock
	private SellerRepository sellerRepository;

	@Mock
	private BillingRepository billingRepository;

	@Mock
	private SqsClient sqsClient;

	@InjectMocks
	private PaymentService paymentService;

	@BeforeEach
	public void setUp() {
		ReflectionTestUtils.setField(paymentService, "partialQueueUrl", "partialQueueUrl");
		ReflectionTestUtils.setField(paymentService, "totalQueueUrl", "totalQueueUrl");
		ReflectionTestUtils.setField(paymentService, "overpaidQueueUrl", "overpaidQueueUrl");
	}

	@Test
	public void testProcessPayments_Success() throws Exception {
		// Dados de entrada
		PaymentRequest request = new PaymentRequest();
		request.setSellerCode("V001");
		Payment payment = new Payment();
		payment.setBillingCode("C001");
		payment.setAmount(new BigDecimal("100.00"));
		request.setPayments(Collections.singletonList(payment));

		// Mock do vendedor
		when(sellerRepository.findByCode("V001")).thenReturn(Optional.of(new Seller()));

		// Mock da cobrança
		Billing billing = new Billing();
		billing.setOriginalAmount(new BigDecimal("100.00"));
		when(billingRepository.findByCode("C001")).thenReturn(Optional.of(billing));

		// Execução do método
		PaymentRequest response = paymentService.processPayments(request);

		// Verificações
		assertEquals(PaymentStatus.TOTAL, response.getPayments().get(0).getStatus());
		verify(sqsClient, times(1)).sendMessage(eq("totalQueueUrl"), anyString());
	}

	@Test
	public void testProcessPayments_SellerNotFound() {
		// Dados de entrada
		PaymentRequest request = new PaymentRequest();
		request.setSellerCode("V999");

		// Mock do vendedor não encontrado
		when(sellerRepository.findByCode("V999")).thenReturn(Optional.empty());

		// Execução do método e verificação da exceção
		Exception exception = assertThrows(NotFoundException.class, () -> {
			paymentService.processPayments(request);
		});

		assertEquals("Vendedor não encontrado", exception.getMessage());
	}

	@Test
	public void testProcessPayments_BillingNotFound() {
		// Dados de entrada
		PaymentRequest request = new PaymentRequest();
		request.setSellerCode("V001");
		Payment payment = new Payment();
		payment.setBillingCode("C999");
		payment.setAmount(new BigDecimal("50.00"));
		request.setPayments(Collections.singletonList(payment));

		// Mock do vendedor
		when(sellerRepository.findByCode("V001")).thenReturn(Optional.of(new Seller()));

		// Mock da cobrança não encontrada
		when(billingRepository.findByCode("C999")).thenReturn(Optional.empty());

		// Execução do método e verificação da exceção
		Exception exception = assertThrows(NotFoundException.class, () -> {
			paymentService.processPayments(request);
		});

		assertEquals("Código da cobrança não encontrado: C999", exception.getMessage());
	}

	@Test
	public void testProcessPayments_PartialPayment() throws Exception {
		// Dados de entrada
		PaymentRequest request = new PaymentRequest();
		request.setSellerCode("V001");
		Payment payment = new Payment();
		payment.setBillingCode("C001");
		payment.setAmount(new BigDecimal("50.00"));
		request.setPayments(Collections.singletonList(payment));

		// Mock do vendedor e cobrança
		when(sellerRepository.findByCode("V001")).thenReturn(Optional.of(new Seller()));
		Billing billing = new Billing();
		billing.setOriginalAmount(new BigDecimal("100.00"));
		when(billingRepository.findByCode("C001")).thenReturn(Optional.of(billing));

		// Execução do método
		PaymentRequest response = paymentService.processPayments(request);

		// Verificações
		assertEquals(PaymentStatus.PARCIAL, response.getPayments().get(0).getStatus());
		verify(sqsClient, times(1)).sendMessage(eq("partialQueueUrl"), anyString());
	}

	@Test
	public void testProcessPayments_OverpaidPayment() throws Exception {
		// Dados de entrada
		PaymentRequest request = new PaymentRequest();
		request.setSellerCode("V001");
		Payment payment = new Payment();
		payment.setBillingCode("C001");
		payment.setAmount(new BigDecimal("150.00"));
		request.setPayments(Collections.singletonList(payment));

		// Mock do vendedor e cobrança
		when(sellerRepository.findByCode("V001")).thenReturn(Optional.of(new Seller()));
		Billing billing = new Billing();
		billing.setOriginalAmount(new BigDecimal("100.00"));
		when(billingRepository.findByCode("C001")).thenReturn(Optional.of(billing));

		// Execução do método
		PaymentRequest response = paymentService.processPayments(request);

		// Verificações
		assertEquals(PaymentStatus.EXCEDENTE, response.getPayments().get(0).getStatus());
		verify(sqsClient, times(1)).sendMessage(eq("overpaidQueueUrl"), anyString());
	}

}
