package com.joguecomigo.ai

import com.joguecomigo.data.AppSettings
import com.joguecomigo.data.GameMode

/**
 * Decide qual implementação de [AiAssistant] usar a partir das configurações.
 *
 * Só usa a IA externa quando o modo é "Com auxílio de IA" E há uma chave de
 * API configurada. Em qualquer outro caso usa o assistente local (offline),
 * garantindo que o app sempre funcione sem Internet.
 */
object AiAssistantFactory {
    fun create(settings: AppSettings): AiAssistant {
        return if (settings.gameMode == GameMode.AI && settings.hasApiKey) {
            ExternalAiAssistant(
                apiKey = settings.apiKey,
                apiUrl = settings.apiUrl,
                fallback = LocalMachineAssistant(),
            )
        } else {
            LocalMachineAssistant()
        }
    }
}
