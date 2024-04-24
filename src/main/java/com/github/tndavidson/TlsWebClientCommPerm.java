package com.github.tndavidson;

import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.github.tndavidson.pojo.CommunicationPermissionBio;

@Component
public class TlsWebClientCommPerm {
	
	private static final Logger LOG = LoggerFactory.getLogger(TlsWebClientCommPerm.class);

	@Autowired
	@Qualifier("DefaultCommPermWebClient")
	private WebClient client;
	
	@Autowired
	@Qualifier("PooledCommPermWebClientWithTimeouts")
	private WebClient clientWithTimeouts;


	public Mono<String> getCommPerm() {
		LOG.debug("Commperm WebClient get permission");
		return this.client
					.get()
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.bodyToMono(String.class)
					.onErrorResume(ex -> Mono.empty());
	}
	
	public Mono<String> getCommPermWithTimeouts() {
		LOG.debug("Commperm WebClient get permission");
		return this.clientWithTimeouts
					.get()
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.bodyToMono(String.class)
					.onErrorResume(ex -> Mono.empty());
	}

}

