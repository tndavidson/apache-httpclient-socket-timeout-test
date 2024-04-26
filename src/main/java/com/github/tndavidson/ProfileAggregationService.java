package com.github.tndavidson;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.tndavidson.pojo.CommunicationPermissionBio;
import com.github.tndavidson.pojo.ContactInformationBio;
import com.github.tndavidson.pojo.ProfileResponse;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;


@Service
public class ProfileAggregationService {
	
	private static final Logger LOG = LoggerFactory.getLogger(ProfileAggregationService.class);
	
	private static final ExecutorService executor = Executors.newFixedThreadPool(20);
	
	@Autowired
    private TlsWebClientCI contactInfoClient;
	
	@Autowired
    private TlsWebClientCommPerm commPermClient;
	
	@Autowired
    private TlsWebClientZombie zombieClient;
	

    /**
     * Webflux route that returns aggeregated ProfileResponse
     * Route fans out 3 https web client requests async and aggregates responses
     * Route execution has a hard timeout/cutoff at 5 seconds
     * @return
     */
    public ProfileResponse getUserProfileData(long aggregateTimeoutSeconds) {
    	LOG.debug("getUserProfileData start with aggregateTimeoutSeconds={}", Long.toString(aggregateTimeoutSeconds));
    	ProfileResponse response = new ProfileResponse();
    	Mono<ContactInformationBio> ciDataMono = contactInfoClient.getContactInformation();
    	Mono<CommunicationPermissionBio> commPermDataMono = commPermClient.getCommPerm();
    	Mono<String> zombieDataMono = zombieClient.getResponse();
    	ciDataMono.subscribe(
  			  value -> {
  				response.setContactInfo(value);
  				System.out.println("ciData Mono got ContactInformationBio=" + value.toString());
  			  },
  			  error -> error.printStackTrace());
    	commPermDataMono.subscribe(
    		  value -> {
    		    response.setCommPerm(value);
				System.out.println("commPermData Mono got CommPermBio=" + value.toString());
			  },
			  error -> error.printStackTrace());
    	zombieDataMono.subscribe(
  			  value -> {
  				  response.setZombieResponse(value);
  				  System.out.println("zombieData Mono got response=" + value);
  			  }, 
  			  error -> error.printStackTrace());
    	
//    	LOG.debug("start blocking/waiting for responseCI");
//    	ContactInformationBio responseCI = ciDataMono.block();
//    	LOG.debug("start blocking/waiting for responseCommPerm");
//    	CommunicationPermissionBio responseCommPerm = commPermDataMono.block();
//    	LOG.debug("start blocking/waiting for responseZombie");
//    	String responseZombie = zombieDataMono.block(); 	
//    	LOG.debug("getUserProfileData has responses ci={}, commPerm={}, and zombie={}", 
//		    responseCI, responseCommPerm, responseZombie);
    	
    	
//    	response = Mono.zip(
//                this.contactInfoClient.getContactInformation(),
//                this.commPermClient.getCommPerm(),
//                this.zombieClient.getResponse()
//        )
//        .map(this::combine)
//        .block();


    	
    	try {
    	    Mono.zip(ciDataMono, commPermDataMono, zombieDataMono).block(Duration.ofSeconds(aggregateTimeoutSeconds));
    	}catch(Exception e) {
    		LOG.debug("Timeout exception waiting for a service response (ignoring) and returning what got so far");
    	}
        return response;
    }
    
    public ProfileResponse getUserProfileDataWithTimeouts(long aggregateTimeoutSeconds) {
    	LOG.debug("getUserProfileDataWithTimeouts start with aggregateTimeoutSeconds={}", Long.toString(aggregateTimeoutSeconds));
    	ProfileResponse response = new ProfileResponse();
    	Mono<ContactInformationBio> ciDataMono = contactInfoClient.getContactInformationWithTimeouts();
    	Mono<CommunicationPermissionBio> commPermDataMono = commPermClient.getCommPermWithTimeouts();
    	Mono<String> zombieDataMono = zombieClient.getResponseWithTimeouts();
    	ciDataMono.subscribe(
			  value -> {
	  				response.setContactInfo(value);
	  				System.out.println("ciData Mono got ContactInformationBio=" + value.toString());
	  			  },
	  			  error -> error.printStackTrace());
    	commPermDataMono.subscribe(
      		  value -> {
      		    response.setCommPerm(value);
  				System.out.println("commPermData Mono got CommPermBio=" + value.toString());
  			  },
  			  error -> error.printStackTrace());
    	zombieDataMono.subscribe(
  			  value -> {
  				  response.setZombieResponse(value);
  				  System.out.println("zombieData Mono got response=" + value);
  			  }, 
  			  error -> error.printStackTrace());
    	
    	try {
    	    Mono.zip(ciDataMono, commPermDataMono, zombieDataMono).block(Duration.ofSeconds(aggregateTimeoutSeconds));
    	}catch(Exception e) {
    		LOG.debug("Timeout exception waiting for a service response (ignoring) and returning what got so far");
    	}
        return response;
    }

    
    public List<ProfileResponse> loadUserProfileData(boolean useClientsWithTimeouts, long aggregateTimeoutSeconds) {
    	LOG.debug("loadUserProfileData start");
    	List<ProfileResponse> responseList = new ArrayList<>();
    	List<Callable<ProfileResponse>> runList = new ArrayList<>();
    	for(int i = 0; i < 50; i++) {
    		if(useClientsWithTimeouts) {
	    		Callable<ProfileResponse> callable = new Callable<>() {
	    			@Override
	    			public ProfileResponse call() {
	    				return getUserProfileDataWithTimeouts(aggregateTimeoutSeconds);
	    			}
	    		};
	    		runList.add(callable);
    		} else {
	    		Callable<ProfileResponse> callable = new Callable<>() {
	    			@Override
	    			public ProfileResponse call() {
	    				return getUserProfileData(aggregateTimeoutSeconds);
	    			}
	    		};
	    		runList.add(callable);
    		}
    	}
    	try {
			List<Future<ProfileResponse>> futureResponseList = executor.invokeAll(runList);
			futureResponseList.stream().forEach(future -> {
				try {
					responseList.add(future.get());
				} catch (InterruptedException | ExecutionException e) {
					System.out.println("Future get interrupted");
				}
			});
		} catch (InterruptedException e) {
			System.out.println("Executor invokeAll interrupted");
		}
    	return responseList;
    }
    
    private ProfileResponse combine(Tuple3<ContactInformationBio, CommunicationPermissionBio, String> tuple) {
    	LOG.debug("combine responses has CI response {} and CommPerm response {} and zombie response {}", 
    			tuple.getT1(), tuple.getT2(), tuple.getT3());
	    return new ProfileResponse(tuple.getT1(), tuple.getT2(), tuple.getT3());
    }
}
