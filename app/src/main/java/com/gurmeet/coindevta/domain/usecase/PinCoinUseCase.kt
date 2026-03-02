package com.gurmeet.coindevta.domain.usecase

import com.gurmeet.coindevta.domain.repository.CoinRepository
import javax.inject.Inject

class PinCoinUseCase @Inject constructor(
    private val repository: CoinRepository
) {
    suspend operator fun invoke(symbol: String) {
        repository.pinCoin(symbol)
    }
}