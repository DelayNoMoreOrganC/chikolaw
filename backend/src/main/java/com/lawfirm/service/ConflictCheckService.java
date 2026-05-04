package com.lawfirm.service;

import com.lawfirm.dto.ConflictCheckResult;
import com.lawfirm.dto.PartyDTO;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 利益冲突审查服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConflictCheckService {

    private final ClientRepository clientRepository;
    private final CaseRepository caseRepository;

    /**
     * 检查客户名称冲突
     * @param clientName 客户名称
     * @return 冲突检查结果
     */
    public ConflictCheckResult checkClientNameConflict(String clientName) {
        ConflictCheckResult result = new ConflictCheckResult();
        result.setHasConflict(false);
        result.setConflictTypes(new ArrayList<>());
        result.setConflicts(new ArrayList<>());

        if (clientName == null || clientName.trim().isEmpty()) {
            result.setRecommendation("NO_ACTION");
            return result;
        }

        // 1. 精确匹配检查
        List<String> exactMatches = clientRepository.findByNameContaining(clientName);
        if (!exactMatches.isEmpty()) {
            for (String match : exactMatches) {
                if (match.equals(clientName)) {
                    // 精确匹配，严重冲突
                    ConflictCheckResult.ConflictDetail detail = new ConflictCheckResult.ConflictDetail();
                    detail.setType("CLIENT_NAME");
                    detail.setDescription("客户名称完全匹配：" + clientName);
                    detail.setRelatedName(match);
                    detail.setSeverity("HIGH");
                    result.getConflicts().add(detail);
                }
            }
        }

        // 2. 高度相似检查（模糊匹配）
        List<String> similarNames = findSimilarNames(clientName);
        for (String similar : similarNames) {
            if (!similar.equals(clientName)) {
                ConflictCheckResult.ConflictDetail detail = new ConflictCheckResult.ConflictDetail();
                detail.setType("HIGH_SIMILARITY");
                detail.setDescription("客户名称高度相似：" + similar);
                detail.setRelatedName(similar);
                detail.setSeverity("MEDIUM");
                result.getConflicts().add(detail);
            }
        }

        // 设置结果
        if (!result.getConflicts().isEmpty()) {
            result.setHasConflict(true);
            result.setConflictTypes(result.getConflicts().stream()
                    .map(ConflictCheckResult.ConflictDetail::getType)
                    .distinct()
                    .collect(Collectors.toList()));
            result.setRecommendation("APPLY_FOR_WAIVER");
        } else {
            result.setRecommendation("NO_ACTION");
        }

        return result;
    }

    /**
     * 检查当事人冲突
     * @param parties 当事人列表
     * @return 冲突检查结果
     */
    public ConflictCheckResult checkPartyConflict(List<PartyDTO> parties) {
        ConflictCheckResult result = new ConflictCheckResult();
        result.setHasConflict(false);
        result.setConflictTypes(new ArrayList<>());
        result.setConflicts(new ArrayList<>());

        if (parties == null || parties.isEmpty()) {
            result.setRecommendation("NO_ACTION");
            return result;
        }

        // 遍历每个当事人进行检查
        for (PartyDTO party : parties) {
            if (party.getName() == null || party.getName().trim().isEmpty()) {
                continue;
            }

            // 检查该当事人是否是现有案件的委托人
            List<String> existingClientCases = caseRepository.findCaseNumbersByClientName(party.getName());
            if (!existingClientCases.isEmpty()) {
                for (String caseNumber : existingClientCases) {
                    ConflictCheckResult.ConflictDetail detail = new ConflictCheckResult.ConflictDetail();
                    detail.setType("PARTY_CONFLICT");
                    detail.setDescription("当事人\"" + party.getName() + "\"是现有案件的委托人：" + caseNumber);
                    detail.setRelatedName(caseNumber);
                    detail.setSeverity("HIGH");
                    result.getConflicts().add(detail);
                }
            }

            // 检查该当事人是否是现有案件的对方当事人
            List<String> existingOpposingCases = caseRepository.findCaseNumbersByOpposingPartyName(party.getName());
            if (!existingOpposingCases.isEmpty()) {
                for (String caseNumber : existingOpposingCases) {
                    ConflictCheckResult.ConflictDetail detail = new ConflictCheckResult.ConflictDetail();
                    detail.setType("PARTY_CONFLICT");
                    detail.setDescription("当事人\"" + party.getName() + "\"是现有案件的对方当事人：" + caseNumber);
                    detail.setRelatedName(caseNumber);
                    detail.setSeverity("HIGH");
                    result.getConflicts().add(detail);
                }
            }
        }

        // 设置结果
        if (!result.getConflicts().isEmpty()) {
            result.setHasConflict(true);
            result.setConflictTypes(result.getConflicts().stream()
                    .map(ConflictCheckResult.ConflictDetail::getType)
                    .distinct()
                    .collect(Collectors.toList()));
            result.setRecommendation("APPLY_FOR_WAIVER");
        } else {
            result.setRecommendation("NO_ACTION");
        }

        return result;
    }

    /**
     * 综合利益冲突检查
     * @param parties 当事人列表
     * @return 冲突检查结果
     */
    public ConflictCheckResult comprehensiveCheck(List<PartyDTO> parties) {
        ConflictCheckResult result = new ConflictCheckResult();
        result.setHasConflict(false);
        result.setConflictTypes(new ArrayList<>());
        result.setConflicts(new ArrayList<>());

        if (parties == null || parties.isEmpty()) {
            result.setRecommendation("NO_ACTION");
            return result;
        }

        // 检查所有当事人
        for (PartyDTO party : parties) {
            // 检查客户名称冲突
            ConflictCheckResult clientResult = checkClientNameConflict(party.getName());
            result.getConflicts().addAll(clientResult.getConflicts());

            // 检查当事人冲突
            ConflictCheckResult partyResult = checkPartyConflict(Arrays.asList(party));
            result.getConflicts().addAll(partyResult.getConflicts());
        }

        // 设置结果
        if (!result.getConflicts().isEmpty()) {
            result.setHasConflict(true);
            result.setConflictTypes(result.getConflicts().stream()
                    .map(ConflictCheckResult.ConflictDetail::getType)
                    .distinct()
                    .collect(Collectors.toList()));
            result.setRecommendation("APPLY_FOR_WAIVER");
        } else {
            result.setRecommendation("NO_ACTION");
        }

        return result;
    }

    /**
     * 查找高度相似的客户名称
     * @param clientName 客户名称
     * @return 相似名称列表
     */
    private List<String> findSimilarNames(String clientName) {
        List<String> allNames = clientRepository.findAllNames();
        List<String> similarNames = new ArrayList<>();

        for (String name : allNames) {
            if (isSimilar(clientName, name)) {
                similarNames.add(name);
            }
        }

        return similarNames;
    }

    /**
     * 判断两个名称是否高度相似
     * 简单实现：包含关系或字符差异小
     */
    private boolean isSimilar(String name1, String name2) {
        if (name1.equals(name2)) {
            return false; // 完全相同不算相似，算精确匹配
        }

        // 1. 包含关系
        if (name1.contains(name2) || name2.contains(name1)) {
            return true;
        }

        // 2. 去除常见差异后比较（如：佛山XX公司 vs 佛山市XX公司）
        String normalized1 = normalizeName(name1);
        String normalized2 = normalizeName(name2);

        if (normalized1.equals(normalized2)) {
            return true;
        }

        // 3. 编辑距离检查（简单实现）
        int distance = levenshteinDistance(name1, name2);
        int maxLength = Math.max(name1.length(), name2.length());
        double similarity = 1.0 - (double) distance / maxLength;

        return similarity > 0.8; // 相似度大于80%
    }

    /**
     * 标准化名称（去除常见差异）
     */
    private String normalizeName(String name) {
        return name
                .replaceAll("市", "")
                .replaceAll("省", "")
                .replaceAll("有限公司", "公司")
                .replaceAll("股份有限公司", "公司")
                .replaceAll("有限责任公司", "公司")
                .replaceAll("\\s+", "")
                .trim();
    }

    /**
     * 计算编辑距离（Levenshtein距离）
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }
}
