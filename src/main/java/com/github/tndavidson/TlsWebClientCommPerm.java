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
	private WebClient webClient;
	
	@Autowired
	@Qualifier("PooledCommPermWebClientWithTimeouts")
	private WebClient webClientWithTimeouts;


	public Mono<CommunicationPermissionBio> getCommPerm() {
		LOG.debug("Commperm WebClient get permission");
		return webClient
					.get()
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.bodyToMono(CommunicationPermissionBio.class)
					.onErrorResume(ex -> Mono.empty());
	}
	
	public Mono<CommunicationPermissionBio> getCommPermWithTimeouts() {
		LOG.debug("Commperm WebClient get permission");
		return webClientWithTimeouts
					.get()
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.bodyToMono(CommunicationPermissionBio.class)
					.onErrorResume(ex -> Mono.empty());
	}

}

