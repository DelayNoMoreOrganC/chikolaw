package com.lawfirm.util;

import com.lawfirm.enums.DocumentTemplateType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 文书类型 canonical code 与 UI/旧接口别名归一（PRD §4.2）。
 */
public final class DocumentTypeAliasResolver {

    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        register("COMPLAINT", "complaint", "起诉状", "民事起诉状");
        register("DEFENSE_STATEMENT", "defense", "defense_statement", "答辩状", "民事答辩状");
        register("BRIEF", "brief", "legalbrief", "legal_brief", "agent_brief", "opinion", "代理词", "庭审代理词");
        register("LEGAL_OPINION", "legal_opinion", "legalopinion", "法律意见书", "法律意见");
        register("LAWYER_LETTER", "letter", "lawyer_letter", "律师函", "律函");
    }

    private DocumentTypeAliasResolver() {
    }

    private static void register(String canonical, String... aliases) {
        ALIASES.put(canonical.toUpperCase(Locale.ROOT), canonical);
        for (String alias : aliases) {
            ALIASES.put(alias.toUpperCase(Locale.ROOT), canonical);
            ALIASES.put(alias, canonical);
        }
    }

    /**
     * @return canonical code，无法识别时返回大写 trim 后的原值
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String trimmed = raw.trim();
        String mapped = ALIASES.get(trimmed);
        if (mapped != null) {
            return mapped;
        }
        mapped = ALIASES.get(trimmed.toUpperCase(Locale.ROOT));
        if (mapped != null) {
            return mapped;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    public static Optional<DocumentTemplateType> toTemplateType(String raw) {
        String code = normalize(raw);
        try {
            return Optional.of(DocumentTemplateType.valueOf(code));
        } catch (IllegalArgumentException e) {
            if ("LAWYER_LETTER".equals(code)) {
                return Optional.empty();
            }
            return Optional.empty();
        }
    }

    public static String displayName(String raw) {
        String code = normalize(raw);
        switch (code) {
            case "COMPLAINT":
                return "起诉状";
            case "DEFENSE_STATEMENT":
                return "答辩状";
            case "BRIEF":
                return "代理词";
            case "LEGAL_OPINION":
                return "法律意见书";
            case "LAWYER_LETTER":
                return "律师函";
            default:
                return raw != null ? raw : "";
        }
    }

    public static boolean isLegacyDocumentType(String raw) {
        return "LAWYER_LETTER".equals(normalize(raw));
    }
}
