package com.pink.family.assignment.database;

import com.pink.family.assignment.database.entity.PersonEntity;
import com.pink.family.assignment.database.repository.PersonRepository;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableJpaRepositories(basePackageClasses = PersonRepository.class)
@EntityScan(basePackageClasses = PersonEntity.class)
public class DBConfig {
}
