package com.lawfirm.service;

import com.lawfirm.dto.CaseSearchRequest;
import com.lawfirm.entity.Case;
import com.lawfirm.exception.BusinessException;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.vo.CaseSearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 类案检索Service
 *
 * 核心算法：多维度相似度计算
 * 1. 案由匹配（权重：40%）
 * 2. 案件类型匹配（权重：25%）
 * 3. 争议金额相似度（权重：20%）
 * 4. 法院层级匹配（权重：15%）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseSearchService {

    private final CaseRepository caseRepository;
    private final EmbeddingService embeddingService;

    @org.springframework.beans.factory.annotation.Value("${case-search.semantic.enabled:false}")
    private boolean semanticEnabled;

    /**
     * 根据检索条件查找相似案例
     */
    public List<CaseSearchResultVO> searchSimilarCases(CaseSearchRequest request) {
        log.info("开始类案检索: {}", request);

        // 获取所有候选案例（排除自己）
        List<Case> allCases = caseRepository.findAll();
        List<Case> candidates = allCases.stream()
                .filter(c -> request.getExcludeCaseId() == null || !c.getId().equals(request.getExcludeCaseId()))
                .collect(Collectors.toList());

        // 计算每个案例的相似度
        List<CaseSearchResultVO> results = new ArrayList<>();
        for (Case candidate : candidates) {
            double similarity = calculateSimilarity(request, candidate);
            if (similarity > 0.3) { // 相似度阈值：30%
                results.add(buildResult(candidate, similarity));
            }
        }

        // 按相似度降序排序，返回Top 20
        return results.stream()
                .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
                .limit(20)
                .collect(Collectors.toList());
    }

    /**
     * 根据案件ID检索相似案例
     */
    public List<CaseSearchResultVO> searchSimilarByCaseId(Long caseId, int limit) {
        // 获取目标案件
        Case targetCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("案件不存在: " + caseId));

        // 构建检索请求
        CaseSearchRequest request = new CaseSearchRequest();
        request.setCaseReason(targetCase.getCaseReason());
        request.setCaseType(targetCase.getCaseType());
        request.setAmount(targetCase.getAmount());
        request.setCourt(targetCase.getCourt());
        request.setExcludeCaseId(caseId);
        request.setLimit(limit);

        // 执行检索
        return searchSimilarCases(request);
    }

    /**
     * 计算相似度（多维度加权算法）
     *
     * @return 相似度分数（0.0-1.0）
     */
    private double calculateSimilarity(CaseSearchRequest request, Case candidate) {
        double totalScore = 0.0;

        // 1. 案由匹配（权重：40%）
        double reasonScore = calculateReasonScore(request.getCaseReason(), candidate.getCaseReason());
        totalScore += reasonScore * 0.40;

        // 2. 案件类型匹配（权重：25%）
        double typeScore = calculateTypeScore(request.getCaseType(), candidate.getCaseType());
        totalScore += typeScore * 0.25;

        // 3. 争议金额相似度（权重：20%）
        double amountScore = calculateAmountScore(request.getAmount(), candidate.getAmount());
        totalScore += amountScore * 0.20;

        // 4. 法院层级匹配（权重：15%）
        double courtScore = calculateCourtScore(request.getCourt(), candidate.getCourt());
        totalScore += courtScore * 0.15;

        return Math.min(totalScore, 1.0); // 最大不超过1.0
    }

    /**
     * 案由相似度（完全匹配=1.0，部分匹配=0.5，不匹配=0.0）
     */
    private double calculateReasonScore(String reason1, String reason2) {
        if (reason1 == null || reason2 == null) return 0.0;
        if (reason1.equals(reason2)) return 1.0;

        // 检查部分匹配（包含关系）
        if (reason1.contains(reason2) || reason2.contains(reason1)) {
            return 0.5;
        }

        if (semanticEnabled) {
            try {
                double semantic = embeddingCosine(reason1, reason2);
                if (semantic > 0.75) {
                    return Math.max(0.5, semantic);
                }
            } catch (Exception e) {
                log.debug("案由语义相似度回退文本匹配: {}", e.getMessage());
            }
        }

        return 0.0;
    }

    private double embeddingCosine(String a, String b) {
        java.util.List<Double> va = embeddingService.embedText(a);
        java.util.List<Double> vb = embeddingService.embedText(b);
        double dot = 0, na = 0, nb = 0;
        int len = Math.min(va.size(), vb.size());
        for (int i = 0; i < len; i++) {
            double x = va.get(i);
            double y = vb.get(i);
            dot += x * y;
            na += x * x;
            nb += y * y;
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /**
     * 案件类型相似度（完全匹配=1.0，同类=0.7，不同类=0.0）
     */
    private double calculateTypeScore(String type1, String type2) {
        if (type1 == null || type2 == null) return 0.0;
        if (type1.equals(type2)) return 1.0;

        // 民事和商事相似度高
        if ((type1.contains("民事") && type2.contains("商事")) ||
            (type1.contains("商事") && type2.contains("民事"))) {
            return 0.7;
        }

        return 0.0;
    }

    /**
     * 争议金额相似度（使用对数尺度，避免大金额主导）
     */
    private double calculateAmountScore(BigDecimal amount1, BigDecimal amount2) {
        if (amount1 == null || amount2 == null) {
            return 0.5; // 如果一方为空，给中性分
        }

        if (amount1.compareTo(BigDecimal.ZERO) == 0 ||
            amount2.compareTo(BigDecimal.ZERO) == 0) {
            return amount1.equals(amount2) ? 1.0 : 0.0;
        }

        // 使用对数差异计算相似度
        double log1 = Math.log(amount1.doubleValue());
        double log2 = Math.log(amount2.doubleValue());
        double diff = Math.abs(log1 - log2);

        // 对数差异小于0.5（约1.65倍）算相似
        if (diff < 0.5) return 1.0 - (diff / 0.5) * 0.3;

        // 对数差异小于1.0（约2.7倍）算部分相似
        if (diff < 1.0) return 0.7 - ((diff - 0.5) / 0.5) * 0.4;

        return 0.0;
    }

    /**
     * 法院层级相似度
     * 完全匹配=1.0，同级法院=0.8，上下级=0.5，不同体系=0.0
     */
    private double calculateCourtScore(String court1, String court2) {
        if (court1 == null || court2 == null) return 0.0;
        if (court1.equals(court2)) return 1.0;

        // 提取法院层级
        String level1 = extractCourtLevel(court1);
        String level2 = extractCourtLevel(court2);

        if (level1.equals(level2)) return 0.8; // 同级法院

        // 上下级关系
        if (isHierarchical(level1, level2)) return 0.5;

        return 0.0;
    }

    /**
     * 提取法院层级
     */
    private String extractCourtLevel(String court) {
        if (court.contains("最高")) return "supreme";
        if (court.contains("高级")) return "high";
        if (court.contains("中级")) return "intermediate";
        if (court.contains("基层") || court.contains("区") || court.contains("县")) return "basic";
        return "unknown";
    }

    /**
     * 判断是否为上下级关系
     */
    private boolean isHierarchical(String level1, String level2) {
        List<String> hierarchy = Arrays.asList("supreme", "high", "intermediate", "basic");
        int idx1 = hierarchy.indexOf(level1);
        int idx2 = hierarchy.indexOf(level2);
        return idx1 >= 0 && idx2 >= 0 && Math.abs(idx1 - idx2) == 1;
    }

    /**
     * 构建检索结果
     */
    private CaseSearchResultVO buildResult(Case caseEntity, double similarity) {
        CaseSearchResultVO result = new CaseSearchResultVO();
        result.setCaseId(caseEntity.getId());
        result.setCaseName(caseEntity.getCaseName());
        result.setCaseNumber(caseEntity.getCaseNumber());
        result.setCaseReason(caseEntity.getCaseReason());
        result.setCaseType(caseEntity.getCaseType());
        result.setCourt(caseEntity.getCourt());
        result.setAmount(caseEntity.getAmount());
        result.setSummary(caseEntity.getSummary());
        result.setSimilarity(similarity);
        result.setSimilarityPercent(String.format("%.1f%%", similarity * 100));
        return result;
    }
}