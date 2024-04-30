package com.github.tndavidson;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.springframework.context.annotation.ComponentScan.Filter;


@SpringBootApplication
//@Import({HttpWebClientConfig.class, SwaggerDocConfig.class})
@Import({HttpWebClientConfig.class})
@ComponentScan(basePackages = "com.github.tndavidson", excludeFilters = @Filter(Configuration.class))
public class WebFluxTestApp {

	public static void main(String[] args) {
		new SpringApplicationBuilder(WebFluxTestApp.class).build().run(args);

	}

}

