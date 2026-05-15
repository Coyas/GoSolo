package pt.com.taskflow.gosolo.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    /*
     * configuração do Flyway para garantir que as migrações sejam aplicadas antes
     * do JPA tentar acessar o banco de dados. O método flyway() é anotado com @Bean
     * e initMethod = "migrate", o que significa que o Flyway será configurado e
     * executará as migrações automaticamente durante a inicialização da aplicação.
     * A dependência do DataSource é injetada para que o Flyway possa se conectar ao
     * banco de dados e aplicar as migrações localizadas em
     * "classpath:db/migration". Isso garante que o esquema do banco de dados esteja
     * atualizado antes que o JPA tente acessar as entidades, evitando conflitos e
     * erros de inicialização.
     * 
     */

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
    }
}
