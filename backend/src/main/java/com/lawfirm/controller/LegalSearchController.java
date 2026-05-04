package com.lawfirm.controller;

import com.lawfirm.dto.RAGSearchRequest;
import com.lawfirm.entity.KnowledgeArticle;
import com.lawfirm.repository.KnowledgeArticleRepository;
import com.lawfirm.service.RAGKnowledgeService;
import com.lawfirm.util.Result;
import com.lawfirm.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/legal-search")
@RequiredArgsConstructor
public class LegalSearchController {

    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final RAGKnowledgeService ragKnowledgeService;
    private final SecurityUtil securityUtil;

    @GetMapping("/regulations")
    public Result<Page<KnowledgeArticle>> searchRegulations(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<KnowledgeArticle> result;
        if (keyword != null && !keyword.isBlank()) {
            result = knowledgeArticleRepository.searchArticles(keyword, pageable);
        } else if (category != null && !category.isBlank()) {
            result = knowledgeArticleRepository.findByCategoryAndDeletedFalse(category, pageable);
        } else {
            result = knowledgeArticleRepository.findPublicArticles(pageable);
        }
        return Result.success(result);
    }

    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return Result.success(List.of("民法典", "民事诉讼法", "执行司法解释", "最高院指导案例", "参考案例", "地方法院裁判指引", "内部制度"));
    }

    @PostMapping("/ask")
    public Result<Map<String, Object>> ask(@RequestBody RAGSearchRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        return Result.success(ragKnowledgeService.ragSearch(request.getQuestion(), userId));
    }
}
