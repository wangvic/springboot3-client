package dev.vicwang.springboot3client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class Springboot3ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(Springboot3ClientApplication.class, args);
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> readyEventApplicationListener(CustomerClient cc) {
        return event -> cc.all().subscribe(System.out::println);
    }

    @Bean
    CustomerClient customerClient(WebClient.Builder builder) {
        return HttpServiceProxyFactory
                .builder(WebClientAdapter.forClient(builder.baseUrl("http://localhost:8080").build()))
                .build()
                .createClient(CustomerClient.class);
    }

    @Bean
    RouteLocator gateway(RouteLocatorBuilder routeLocatorBuilder) {
        return routeLocatorBuilder
                .routes()
                .route(rs -> rs.path("/proxy")
                        .filters(fs -> fs
                                .setPath("/customers")
                                .addResponseHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                        )
                        .uri("http://localhost:8080"))
                .build();
    }
}

record Customer(Integer id, String name) {
}

@Controller
class CustomerGraphqlController {
    private final CustomerClient cc;

    public CustomerGraphqlController(CustomerClient cc) {
        this.cc = cc;
    }

    @QueryMapping
    Flux<Customer> customers() {
        return this.cc.all();
    }

    @QueryMapping
    Flux<Customer> customersByName(@Argument String name) {
        return this.cc.byName(name);
    }

    @SchemaMapping(typeName = "Customer")
    Mono<Profile> profile(Customer customer) {
        return Mono.just(new Profile(customer.id()));
    }
}

record Profile(Integer id) {
}

interface CustomerClient {
    @GetExchange("/customers")
    Flux<Customer> all();

    @GetExchange("/customers/{name}")
    Flux<Customer> byName(@PathVariable String name);

}
