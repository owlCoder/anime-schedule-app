package com.owlcoder.animeschedule.domain.usecase

import com.owlcoder.animeschedule.domain.repository.AuthRepository
import javax.inject.Inject

class LoginWithMalUseCase @Inject constructor(private val authRepository: AuthRepository) {
    operator fun invoke() = authRepository.buildAuthUri() // returns Triple<Uri, verifier, state>
}

class HandleMalCallbackUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(code: String, verifier: String): Boolean =
        authRepository.handleOAuthCallback(code, verifier)
}

class LogoutFromMalUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke() = authRepository.logout()
}
