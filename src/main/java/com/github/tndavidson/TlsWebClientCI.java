package com.github.tndavidson;

import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.github.tndavidson.pojo.ContactInformationBio;

@Component
public class TlsWebClientCI {
	
	private static final Logger LOG = LoggerFactory.getLogger(TlsWebClientCI.class);

	@Autowired
	@Qualifier("DefaultCIWebClient")
	private WebClient client;
	
	@Autowired
	@Qualifier("PooledCIWebClientWithTimeouts")
	private WebClient clientWithTimeouts;


	public Mono<String> getContactInformation() {
		LOG.debug("CI WebClient get contact info");
		
		Mono<String> response = client.get()
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.bodyToMono(String.class)
					.onErrorResume(ex -> Mono.empty());
		            //.block();

		//LOG.debug("CI WebClient has response={}", response != null ? response.toString() : "null");
		return response;
	}
	
	public Mono<String> getContactInformationWithTimeouts() {
		LOG.debug("CI WebClient get contact info with timeouts");
		Mono<String> response =  this.clientWithTimeouts
					.get()
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.bodyToMono(String.class)
					.onErrorResume(ex -> Mono.empty());
		LOG.debug("CI WebClient get contact info with timeouts returned response Mono");
		return response;
	}

}

