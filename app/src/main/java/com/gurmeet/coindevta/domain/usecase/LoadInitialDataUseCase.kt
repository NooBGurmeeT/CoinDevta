package com.gurmeet.coindevta.domain.usecase

import com.gurmeet.coindevta.domain.repository.CoinRepository
import com.gurmeet.coindevta.util.Response
import javax.inject.Inject

class LoadInitialDataUseCase @Inject constructor(
    private val repository: CoinRepository
) {
    suspend operator fun invoke(): Response<Unit> {
        return repository.loadInitialData()
    }
}