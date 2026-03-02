package com.gurmeet.coindevta.domain.usecase

import com.gurmeet.coindevta.domain.model.Coin
import com.gurmeet.coindevta.domain.repository.CoinRepository
import javax.inject.Inject

class GetPinnedCoinUseCase @Inject constructor(
    private val repository: CoinRepository
) {
    suspend operator fun invoke(): Coin? {
        return repository.getPinnedCoin()
    }
}