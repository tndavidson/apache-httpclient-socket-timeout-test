package com.github.tndavidson;

import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;


@Component
public class TlsWebClientZombie {
	
	private static final Logger LOG = LoggerFactory.getLogger(TlsWebClientZombie.class);
	
	@Autowired
	@Qualifier("DefaultZombieWebClient")
	private WebClient webClient;
	
	@Autowired
	@Qualifier("PooledZombieWebClientWithTimeouts")
	private WebClient webClientWithTimeouts;

	
	public Mono<String> getResponse() {
		LOG.debug("calling zombie service");
		return webClient
					.get()
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.bodyToMono(String.class)
					.onErrorResume(ex -> Mono.empty());
	}
	
	public Mono<String> getResponseWithTimeouts() {
		LOG.debug("calling zombie service with timeouts");
		return webClientWithTimeouts
					.get()
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.bodyToMono(String.class)
					.onErrorResume(ex -> Mono.empty());
	}

}

