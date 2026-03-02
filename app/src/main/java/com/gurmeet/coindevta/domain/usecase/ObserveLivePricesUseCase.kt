package com.gurmeet.coindevta.domain.usecase

import com.gurmeet.coindevta.domain.repository.CoinRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLivePricesUseCase @Inject constructor(
    private val repository: CoinRepository
) {
    operator fun invoke(): Flow<Pair<String, Double>> {
        return repository.observeLivePrices()
    }
}