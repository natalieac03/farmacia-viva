package br.org.cremic.farmaciaviva.config;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Habilita a auditoria de datas do Spring Data JPA.
 *
 * As datas de criacao e atualizacao das entidades passam a ser preenchidas a
 * partir do bean Clock da aplicacao, o que mantem um unico ponto de verdade
 * para o tempo e permite fixar o relogio nos testes.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.<TemporalAccessor>of(OffsetDateTime.now(clock));
    }
}
