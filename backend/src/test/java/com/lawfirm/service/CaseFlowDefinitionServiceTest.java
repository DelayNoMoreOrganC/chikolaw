package com.lawfirm.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CaseFlowDefinitionServiceTest {

    @Autowired
    private CaseFlowDefinitionService caseFlowDefinitionService;

    @Test
    void civilStagesMatchPrd() {
        List<String> stages = caseFlowDefinitionService.getStageNames("CIVIL");
        assertEquals(9, stages.size());
        assertEquals("咨询", stages.get(0));
        assertEquals("结案归档", stages.get(stages.size() - 1));
    }

    @Test
    void canonicalAliasMapsLegacyNames() {
        assertEquals("待立案", caseFlowDefinitionService.canonicalStageName("立案"));
        assertEquals("一审审理中", caseFlowDefinitionService.canonicalStageName("一审"));
    }

    @Test
    void arbitrationIncludesDraftStage() {
        List<String> stages = caseFlowDefinitionService.getStageNames("ARBITRATION");
        assertTrue(stages.contains("起草文书"));
        assertTrue(stages.contains("申请仲裁"));
    }
}
