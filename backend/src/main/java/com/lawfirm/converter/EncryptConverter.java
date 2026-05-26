package com.lawfirm.converter;

import com.lawfirm.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.regex.Pattern;

/**
 * JPA 敏感字段加解密。解密失败时不阻断实体加载（避免登录时因密钥与历史数据不一致而整表读失败）。
 */
@Slf4j
@Converter
@Component
@RequiredArgsConstructor
public class EncryptConverter implements AttributeConverter<String, String> {

    private static final Pattern PLAIN_PHONE = Pattern.compile("^[+\\d\\s-]{6,20}$");

    private final CryptoUtil cryptoUtil;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        return cryptoUtil.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        try {
            return cryptoUtil.decrypt(dbData);
        } catch (Exception e) {
            // 历史明文或未加密数据
            if (looksLikePlaintext(dbData)) {
                log.debug("敏感字段按明文兼容读取（非密文或旧数据）");
                return dbData;
            }
            log.warn("敏感字段解密失败，已置空（请检查 CRYPTO_SECRET_KEY 是否与写入数据时一致）: {}", e.getMessage());
            return null;
        }
    }

    private static boolean looksLikePlaintext(String s) {
        if (s.contains("@")) {
            return true;
        }
        return PLAIN_PHONE.matcher(s.trim()).matches();
    }
}
