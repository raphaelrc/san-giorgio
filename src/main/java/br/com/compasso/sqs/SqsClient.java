package br.com.compasso.sqs;

import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SqsClient {
	
	private final QueueMessagingTemplate queueMessagingTemplate;

    public void sendMessage(String queueUrl, String messageBody) {
        queueMessagingTemplate.convertAndSend(queueUrl, messageBody);
    }

}
