package com.gurmeet.coindevta.domain.usecase

import com.gurmeet.coindevta.domain.model.Coin
import com.gurmeet.coindevta.domain.repository.CoinRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveCoinsUseCase @Inject constructor(
    private val repository: CoinRepository
) {
    operator fun invoke(): Flow<List<Coin>> {
        return repository.observeCoins()
    }
}