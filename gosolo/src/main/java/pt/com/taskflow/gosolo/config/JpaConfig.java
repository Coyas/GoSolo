package pt.com.taskflow.gosolo.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JpaConfig {

    /*
     * Houve um problema de concorrência entre o Flyway e o JPA, onde ambos tentavam
     * acessar o banco de dados ao mesmo tempo durante a inicialização da aplicação.
     * Para resolver isso, adicionei um BeanFactoryPostProcessor que garante que o
     * Flyway seja executado antes do JPA, definindo uma dependência explícita entre
     * eles. Assim, o Flyway migrará o banco de dados antes que o JPA tente acessar
     * as entidades, evitando conflitos e garantindo uma inicialização suave da
     * aplicação.
     * 
     */

    @Bean
    public static BeanFactoryPostProcessor flywayBeforeJpa() {
        return (ConfigurableListableBeanFactory beanFactory) -> {
            BeanDefinition bd = beanFactory.getBeanDefinition("entityManagerFactory");
            bd.setDependsOn("flyway");
        };
    }
}
