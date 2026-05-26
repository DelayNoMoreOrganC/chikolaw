package com.lawfirm.service;

import com.lawfirm.config.LLMProperties;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.enums.AIModelUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIModelRoutingServiceTest {

    @Mock
    private AIConfigService aiConfigService;

    @Mock
    private LLMProperties llmProperties;

    @Mock
    private LLMProperties.RoutingConfig routingConfig;

    @InjectMocks
    private AIModelRoutingService aimodelRoutingService;

    @BeforeEach
    void setup() {
        lenient().when(llmProperties.getRouting()).thenReturn(routingConfig);
        lenient().when(routingConfig.getLegalChat()).thenReturn("deepseek");
        lenient().when(routingConfig.getRag()).thenReturn("lmstudio");
    }

    @Test
    void resolveReturnsFirstEnabledMatchingProvider() {
        AIConfig cfg = new AIConfig();
        cfg.setId(10L);
        cfg.setProviderType("deepseek");
        cfg.setModelName("deepseek-chat");
        when(aiConfigService.findFirstEnabledByProviderIgnoreCase("deepseek")).thenReturn(Optional.of(cfg));

        AIConfig out = aimodelRoutingService.resolveForUseCase(AIModelUseCase.LEGAL_CHAT);
        assertEquals(10L, out.getId());
        assertEquals("deepseek", out.getProviderType());
    }

    @Test
    void resolveFallsBackToDefaultWhenProviderMissing() {
        when(aiConfigService.findFirstEnabledByProviderIgnoreCase("lmstudio")).thenReturn(Optional.empty());
        AIConfig def = new AIConfig();
        def.setId(2L);
        def.setProviderType("ollama");
        when(aiConfigService.findDefaultConfigOptional()).thenReturn(Optional.of(def));

        when(routingConfig.getRag()).thenReturn("lmstudio");
        AIConfig out = aimodelRoutingService.resolveForUseCase(AIModelUseCase.RAG);
        assertEquals(2L, out.getId());
    }

    @Test
    void resolveThrowsWhenNoMatchAndNoDefault() {
        when(aiConfigService.findFirstEnabledByProviderIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(aiConfigService.findDefaultConfigOptional()).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> aimodelRoutingService.resolveForUseCase(AIModelUseCase.RAG));
    }
}
