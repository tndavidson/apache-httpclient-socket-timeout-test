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
	@Qualifier("CIWebClientWithTimeouts")
	private WebClient clientWithTimeouts;


	public Mono<ContactInformationBio> getContactInformation() {
		LOG.debug("CI WebClient get contact info");
		
		Mono<ContactInformationBio> response = client.get()
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.bodyToMono(ContactInformationBio.class)
					.onErrorResume(ex -> Mono.empty());
		            //.block();
		return response;
	}
	
	public Mono<ContactInformationBio> getContactInformationWithTimeouts() {
		LOG.debug("CI WebClient get contact info with timeouts");
		Mono<ContactInformationBio> response =  clientWithTimeouts
					.get()
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.bodyToMono(ContactInformationBio.class)
					.onErrorResume(ex -> Mono.empty());
		return response;
	}

}

