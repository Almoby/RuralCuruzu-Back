package com.almoby.ruralcuruzu.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.almoby.ruralcuruzu.domain.TokenRevocado;

public interface TokenRevocadoRepository extends MongoRepository<TokenRevocado, String> {
}
