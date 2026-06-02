package ec.edu.uteq.distribuidas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class RegistroDistribuidoApplication {
    public static void main(String[] args) {
        SpringApplication.run(RegistroDistribuidoApplication.class, args);
    }
}
