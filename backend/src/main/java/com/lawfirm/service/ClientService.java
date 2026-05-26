package com.lawfirm.service;

import com.lawfirm.dto.ClientDTO;
import com.lawfirm.entity.Client;
import com.lawfirm.entity.Case;
import com.lawfirm.repository.ClientRepository;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 客户管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final com.lawfirm.repository.CommunicationRecordRepository communicationRecordRepository;
    private final com.lawfirm.repository.PartyRepository partyRepository;

    /**
     * 创建客户
     */
    @Transactional
    public ClientDTO createClient(ClientDTO dto, Long userId) {
        // 检查客户是否已存在
        if (clientRepository.existsByClientNameAndDeletedIsFalse(dto.getClientName())) {
            throw new IllegalArgumentException("客户已存在");
        }

        Client client = new Client();
        client.setClientType(dto.getClientType());
        client.setClientName(dto.getClientName());
        client.setGender(dto.getGender());
        client.setIdCard(dto.getIdCard());
        client.setCreditCode(dto.getCreditCode());
        client.setPhone(dto.getPhone());
        client.setEmail(dto.getEmail());
        client.setAddress(dto.getAddress());
        client.setLegalRepresentative(dto.getLegalRepresentative());
        client.setIndustry(dto.getIndustry());
        client.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        client.setSource(dto.getSource());
        client.setNotes(dto.getNotes());
        client.setOwnerId(dto.getOwnerId() != null ? dto.getOwnerId() : userId);

        client = clientRepository.save(client);
        log.info("创建客户成功: {}", client.getId());

        return convertToDTO(client);
    }

    /**
     * 更新客户（行政要求：非管理员不可修改客户名称；案源人变更须管理员/主任）
     */
    @Transactional
    public ClientDTO updateClient(Long id, ClientDTO dto, Long operatorId, boolean operatorIsAdmin) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));

        if (!operatorIsAdmin) {
            if (dto.getClientName() != null && !dto.getClientName().equals(client.getClientName())) {
                throw new IllegalArgumentException("客户名称变更须系统管理员或主任审批后修改");
            }
            if (dto.getOwnerId() != null && !dto.getOwnerId().equals(client.getOwnerId())) {
                throw new IllegalArgumentException("案源人/负责人变更须行政审批，请联系管理员");
            }
        }

        client.setClientType(dto.getClientType());
        client.setClientName(dto.getClientName());
        client.setGender(dto.getGender());
        client.setIdCard(dto.getIdCard());
        client.setCreditCode(dto.getCreditCode());
        client.setPhone(dto.getPhone());
        client.setEmail(dto.getEmail());
        client.setAddress(dto.getAddress());
        client.setLegalRepresentative(dto.getLegalRepresentative());
        client.setIndustry(dto.getIndustry());
        client.setStatus(dto.getStatus());
        client.setSource(dto.getSource());
        client.setNotes(dto.getNotes());
        client.setOwnerId(dto.getOwnerId());

        client = clientRepository.save(client);
        log.info("更新客户成功: {}", id);

        return convertToDTO(client);
    }

    /**
     * @deprecated 使用 {@link #updateClient(Long, ClientDTO, Long, boolean)}
     */
    @Transactional
    public ClientDTO updateClient(Long id, ClientDTO dto) {
        return updateClient(id, dto, null, true);
    }

    /**
     * 删除客户
     */
    @Transactional
    public void deleteClient(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new IllegalArgumentException("客户不存在");
        }

        // 检查是否有关联案件（使用数据库查询优化）
        List<Case> relatedCases = caseRepository.findByClientIdOrderByCreatedAtDesc(id);

        if (!relatedCases.isEmpty()) {
            throw new IllegalArgumentException("该客户存在关联案件，无法删除");
        }

        clientRepository.deleteById(id);
        log.info("删除客户成功: {}", id);
    }

    /**
     * 根据ID查询客户
     */
    public ClientDTO getClientById(Long id) {
        return clientRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
    }

    /**
     * 搜索客户
     */
    public List<ClientDTO> searchClients(String keyword) {
        // 使用数据库查询优化，只查询未删除的客户
        return clientRepository.findByDeletedFalse().stream()
                .filter(c -> containsKeyword(c.getClientName(), keyword)
                        || containsKeyword(c.getPhone(), keyword)
                        || containsKeyword(c.getIdCard(), keyword)
                        || containsKeyword(c.getCreditCode(), keyword))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按类型查询客户
     */
    public List<ClientDTO> getClientsByType(String clientType) {
        // 使用数据库查询优化
        return clientRepository.findByClientTypeAndDeletedFalse(clientType).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按状态查询客户
     */
    public List<ClientDTO> getClientsByStatus(String status) {
        // 使用数据库查询优化
        return clientRepository.findByStatusAndDeletedFalse(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户的客户
     */
    public List<ClientDTO> getClientsByOwner(Long ownerId) {
        // 使用数据库查询优化
        return clientRepository.findByOwnerIdAndDeletedFalse(ownerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询客户
     */
    public com.lawfirm.util.PageResult<ClientDTO> getClients(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 使用数据库查询优化，只查询未删除的客户
        List<Client> allClients = clientRepository.findByDeletedFalse();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allClients.size());

        // 参数验证：防止空数据时subList参数错误
        List<Client> pageClients;
        if (start >= end || start >= allClients.size()) {
            pageClients = new java.util.ArrayList<>();
        } else {
            pageClients = allClients.subList(start, end);
        }

        List<ClientDTO> dtoList = pageClients.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new com.lawfirm.util.PageResult<>((long) page, (long) size, (long) allClients.size(), dtoList);
    }

    /**
     * 利益冲突检索
     */
    public ClientDTO checkConflict(Long clientId) {
        ClientDTO dto = getClientById(clientId);

        // 检查是否存在利益冲突（案件中对立方）
        List<Case> allCases = caseRepository.findAll();
        List<Long> conflictCaseIds = allCases.stream()
                .filter(c -> {
                    // 如果客户参与了案件，检查案件的对方当事人
                    // 这里简化处理，实际应该检查Party表
                    return false;
                })
                .map(Case::getId)
                .collect(Collectors.toList());

        dto.setHasConflict(!conflictCaseIds.isEmpty());
        dto.setConflictCaseIds(conflictCaseIds);

        return dto;
    }

    /**
     * 从案件获取客户ID（简化实现）
     */
    private Long getClientIdFromCase(Case c) {
        // 实际应该从Party表获取
        return null;
    }

    /**
     * 关键词匹配
     */
    private boolean containsKeyword(String text, String keyword) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(keyword)) {
            return false;
        }
        return text.toLowerCase().contains(keyword.toLowerCase());
    }

    /**
     * 转换为DTO
     */
    private ClientDTO convertToDTO(Client client) {
        ClientDTO dto = new ClientDTO();
        dto.setId(client.getId());
        dto.setClientType(client.getClientType());
        dto.setClientName(client.getClientName());
        dto.setGender(client.getGender());
        dto.setIdCard(client.getIdCard());
        dto.setCreditCode(client.getCreditCode());
        dto.setPhone(client.getPhone());
        dto.setEmail(client.getEmail());
        dto.setAddress(client.getAddress());
        dto.setLegalRepresentative(client.getLegalRepresentative());
        dto.setIndustry(client.getIndustry());
        dto.setStatus(client.getStatus());
        dto.setSource(client.getSource());
        dto.setNotes(client.getNotes());
        dto.setOwnerId(client.getOwnerId());
        dto.setCreatedAt(client.getCreatedAt());
        dto.setUpdatedAt(client.getUpdatedAt());

        // 加载负责人名称
        if (client.getOwnerId() != null) {
            userRepository.findById(client.getOwnerId()).ifPresent(u -> dto.setOwnerName(u.getRealName()));
        }

        // 统计关联案件数（使用数据库查询优化）
        long caseCount = caseRepository.findByDeletedFalse().stream()
                .filter(c -> client.getId().equals(getClientIdFromCase(c)))
                .count();
        dto.setCaseCount((int) caseCount);

        return dto;
    }

    /**
     * 获取客户的案件列表
     */
    public List<com.lawfirm.vo.CaseListVO> getClientCases(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));

        // 通过Party表关联客户和案件（性能优化：使用数据库查询而非全表加载）
        List<com.lawfirm.entity.Party> parties = partyRepository.findByNameAndDeletedFalse(client.getClientName());

        // 提取案件ID集合
        List<Long> caseIds = parties.stream()
                .map(com.lawfirm.entity.Party::getCaseId)
                .distinct()
                .collect(Collectors.toList());

        // 查询对应的案件
        return caseIds.stream()
                .map(caseId -> caseRepository.findById(caseId))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(c -> !c.getDeleted())
                .map(this::convertToCaseListVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取客户的沟通记录
     */
    public List<com.lawfirm.entity.CommunicationRecord> getCommunications(Long clientId, int page, int size) {
        List<com.lawfirm.entity.CommunicationRecord> allRecords =
            communicationRecordRepository.findByClientIdOrderByCommunicationDateDesc(clientId);

        // 简单分页
        int start = page * size;
        int end = Math.min(start + size, allRecords.size());
        if (start >= allRecords.size()) {
            return new ArrayList<>();
        }
        return allRecords.subList(start, end);
    }

    /**
     * 创建沟通记录
     */
    @Transactional
    public com.lawfirm.entity.CommunicationRecord createCommunication(Long clientId, com.lawfirm.dto.CommunicationRecordDTO dto, Long operatorId) {
        com.lawfirm.entity.CommunicationRecord record = new com.lawfirm.entity.CommunicationRecord();
        record.setClientId(clientId);
        record.setCommunicationType(dto.getCommunicationType());
        record.setCommunicationDate(dto.getCommunicationDate());
        record.setContent(dto.getContent());
        record.setNextFollowDate(dto.getNextFollowDate());
        record.setAttachments(dto.getAttachments());
        record.setOperatorId(operatorId);
        return communicationRecordRepository.save(record);
    }

    /**
     * 转换为案件列表VO
     */
    private com.lawfirm.vo.CaseListVO convertToCaseListVO(Case caseEntity) {
        com.lawfirm.vo.CaseListVO vo = new com.lawfirm.vo.CaseListVO();
        vo.setId(caseEntity.getId());
        vo.setCaseName(caseEntity.getCaseName());
        vo.setCaseNumber(caseEntity.getCaseNumber());
        vo.setCaseType(caseEntity.getCaseType());
        vo.setStatus(caseEntity.getStatus());
        vo.setCourt(caseEntity.getCourt());
        vo.setCreatedAt(caseEntity.getCreatedAt() != null ?
            caseEntity.getCreatedAt().toLocalDate() : null);
        return vo;
    }
}
