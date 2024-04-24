package com.github.tndavidson;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tndavidson.pojo.CommunicationPermissionBio;
import com.github.tndavidson.pojo.ContactInformationBio;
import com.github.tndavidson.pojo.ProfileResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;


@Service
public class ProfileAggregationService {
	
	private static final Logger LOG = LoggerFactory.getLogger(ProfileAggregationService.class);
	
	@Autowired
    private TlsWebClientCI contactInfoClient;
	
	@Autowired
    private TlsWebClientCommPerm commPermClient;
	
	@Autowired
    private TlsWebClientZombie zombieClient;
	
	private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Webflux route that returns one result/Mono
     * Route fans out 3 https web client requests async and aggregates responses
     * Route execution has a hard timeout/cutoff at 5 seconds
     * @return
     */
    public ProfileResponse getUserProfileData() {
    	LOG.debug("getUserProfileData start");
    	ProfileResponse response = new ProfileResponse();
    	Mono<String> ciDataMono = contactInfoClient.getContactInformation();
    	Mono<String> commPermDataMono = commPermClient.getCommPerm();
    	Mono<String> zombieDataMono = zombieClient.getResponse();
    	ciDataMono.subscribe(
  			  value -> {
  				  try {
					response.setContactInfo(mapper.readValue(value, ContactInformationBio.class));
				} catch (JsonProcessingException e) {
				}
  				System.out.println("ciData Mono got ContactInformationBio=" + value);
  			  },
  			  error -> error.printStackTrace());
    	commPermDataMono.subscribe(
    		  value -> {
				  try {
					response.setCommPerm(mapper.readValue(value, CommunicationPermissionBio.class));
				} catch (JsonProcessingException e) {
				}
				System.out.println("commPermData Mono got CommPermBio=" + value);
			  },
			  error -> error.printStackTrace());
    	zombieDataMono.subscribe(
  			  value -> {
  				  response.setZombieResponse(value);
  				  System.out.println("zombieData Mono got response=" + value);
  			  }, 
  			  error -> error.printStackTrace());
//    	LOG.debug("start blocking/waiting for responseCI");
//    	String responseCI = waitForResponse(ciDataMono, "CI Service");
//    	LOG.debug("start blocking/waiting for responseCommPerm");
//    	String responseCommPerm = waitForResponse(commPermDataMono, "Comm Perm Service");
//    	LOG.debug("start blocking/waiting for responseZombie");
//    	String responseZombie = waitForResponse(zombieDataMono, "Zombie Service");
//    	LOG.debug("getUserProfileData done blocking waiting and has responses: ci={}, commperm={}, zombie={}", 
//    			responseCI.toString(), responseCommPerm.toString(), responseZombie);   	

        
//    	LOG.debug("getUserProfileData blocking waiting for responseTuple");
//    	Tuple3<String, String, String> responseTuple = waitForAggregateResponse(
//    			Mono.zip(ciDataMono, commPermDataMono, zombieDataMono));
//    			.block(Duration.ofSeconds(5));
//    			.map(this::combine);
//    	String responseCI = responseTuple.getT1();
//    	String responseCommPerm = responseTuple.getT2();
//    	String responseZombie = responseTuple.getT3();

    	try {
    	    Mono.zip(ciDataMono, commPermDataMono, zombieDataMono).block(Duration.ofSeconds(5));
    	}catch(Exception e) {
    		LOG.debug("Timeout exception (ignore): ", e);
    	}
    	
    	
//    	LOG.debug("getUserProfileData has responses ci={}, commPerm={}, and zombie={}", 
//    			responseCI, responseCommPerm, responseZombie);
//    	ObjectMapper mapper = new ObjectMapper();
//    	try {
//	        return new ProfileResponse(mapper.readValue(responseCI, ContactInformationBio.class), 
//	        		mapper.readValue(responseCommPerm, CommunicationPermissionBio.class), responseZombie);
//    	}catch(Exception e) {
//    		LOG.error("Json parse exception: ", e);
//    	}
        return response;
    }
    
    private Tuple3<String, String, String> waitForAggregateResponse(Mono<Tuple3<String, String, String>> responseMono) {
    	Tuple3<String, String, String> response = null;
    	try {
    		response = responseMono.block(Duration.ofSeconds(5));
    	} catch(Exception e) {
    		LOG.debug("timeout fired waiting for a service response");
    	}
    	return response;
    }
    
    
    private String waitForResponse(Mono<String> responseMono, String serviceName) {
    	String response = "";
    	try {
    		response = responseMono.block(Duration.ofSeconds(5));
    	} catch(Exception e) {
    		LOG.debug("timeout fired for {}", serviceName);
    		response = "Timeout Fired and No Response From " + serviceName;
    	}
    	return response;
    }
    
    public ProfileResponse getUserProfileDataWithTimeouts() {
    	LOG.debug("getUserProfileDataWithTimeouts start");
//        return Mono.zip(
//                        this.contactInfoClient.getContactInformationWithTimeouts(),
//                        this.commPermClient.getCommPermWithTimeouts(),
//                        this.zombieClient.getResponseWithTimeouts()
//                )
//                .map(this::combine)
//                .timeout(Duration.ofSeconds(5))
//                .block();
    	return null;
    }

    private ProfileResponse combine(Tuple3<String, String, String> tuple) {
    	LOG.debug("combine responses has CI response {} and CommPerm response {} and zombie response {}", 
    			tuple.getT1(), tuple.getT2(), tuple.getT3());
    	try {
	        return new ProfileResponse(
	        		mapper.readValue(tuple.getT1(), ContactInformationBio.class),
	        		mapper.readValue(tuple.getT2(), CommunicationPermissionBio.class),
	                tuple.getT3()
	        );
    	}catch(Exception e) {
    		LOG.error("Json parse exception: ", e);
    	}
    	return null;
    }
}
