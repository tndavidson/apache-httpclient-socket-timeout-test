package com.github.tndavidson;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tndavidson.pojo.ContactInformationBio;
import com.github.tndavidson.pojo.ProfileResponse;
import reactor.core.publisher.Mono;


@RestController
public class TestRestController {
	
	@Autowired
	private ProfileAggregationService profileService;
	
	@Autowired
	private TlsWebClientCI client;
	
	@Autowired
	private TlsWebClientZombie clientZombie;
	
	
//    @GetMapping("/test-with-timeouts")
//    public ProfileResponse getProfileDataWithTimeouts() {
//    	long startTime = System.currentTimeMillis();
//    	ProfileResponse response = profileService.getUserProfileDataWithTimeouts();
//        long endTime = System.currentTimeMillis();
//        System.out.println("*******  aggregate response total time (millis): " + Long.toString(endTime - startTime) + "  ******");
//    	return response;
//    }
    
    @GetMapping("/test-no-timeouts")
    public ProfileResponse getProfileData() {
    	long startTime = System.currentTimeMillis();
    	ProfileResponse response = profileService.getUserProfileData();
        long endTime = System.currentTimeMillis();
        System.out.println("*******  aggregate response total time (millis): " + Long.toString(endTime - startTime) + "  ******");
    	return response;
    }

    @GetMapping("/test")
    public Mono<String> test() {
    	ContactInformationBio responseBio = new ContactInformationBio();
    	long startTime = System.currentTimeMillis();
    	//Mono<String> responseMono = client.getContactInformation();
    	
    	Mono<String> responseMono = clientZombie.getResponse();
    	
    	responseMono.subscribe(
    			  value -> System.out.println("responseMono got value=" + value.toString()), 
    			  error -> error.printStackTrace());
//    	System.out.println("start blocking for mono");
//    	String response = responseMono.block();
//    	if(response != null) {
//    	    System.out.println("ContactInformationBio response=" + response.toString());
//    	} else {
//    		System.out.println("ContactInformationBio is null!!!");
//    	}
//        long endTime = System.currentTimeMillis();
//        System.out.println("*******  aggregate get total time (millis): " + Long.toString(endTime - startTime) + "  ******");
//        ObjectMapper mapper = new ObjectMapper();
//        
//        try {
//			responseBio = mapper.readValue(response, ContactInformationBio.class);
//		} catch (Exception e) {
//			System.out.println("Json parse exception");
//		}
        return responseMono;
    }

}