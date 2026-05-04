package com.lawfirm.service;

import com.lawfirm.dto.OfficeSuppliesDTO;
import com.lawfirm.entity.OfficeSupplies;
import com.lawfirm.repository.OfficeSuppliesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 办公用品服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfficeSuppliesService {

    private final OfficeSuppliesRepository repository;

    /**
     * 创建办公用品
     */
    public OfficeSuppliesDTO create(OfficeSuppliesDTO dto) {
        OfficeSupplies entity = new OfficeSupplies();
        BeanUtils.copyProperties(dto, entity);

        // 自动计算状态
        updateStatus(entity);

        OfficeSupplies saved = repository.save(entity);
        log.info("创建办公用品成功: id={}, name={}, category={}", saved.getId(),
                saved.getName(), saved.getCategory());

        return toVO(saved);
    }

    /**
     * 更新办公用品
     */
    public OfficeSuppliesDTO update(Long id, OfficeSuppliesDTO dto) {
        OfficeSupplies entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("办公用品不存在"));

        BeanUtils.copyProperties(dto, entity, "id");

        // 库存变化时更新状态和最后入库时间
        if (dto.getStockQuantity() != null && !dto.getStockQuantity().equals(entity.getStockQuantity())) {
            entity.setLastStockInDate(LocalDate.now());
            updateStatus(entity);
        }

        OfficeSupplies saved = repository.save(entity);
        log.info("更新办公用品成功: id={}", id);

        return toVO(saved);
    }

    /**
     * 删除办公用品（逻辑删除）
     */
    @Transactional
    public void delete(Long id) {
        OfficeSupplies entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("办公用品不存在"));

        entity.setDeleted(true);
        repository.save(entity);
        log.info("删除办公用品成功: id={}", id);
    }

    /**
     * 根据ID查询
     */
    public OfficeSuppliesDTO getById(Long id) {
        OfficeSupplies entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("办公用品不存在"));
        return toVO(entity);
    }

    /**
     * 查询所有办公用品
     */
    public List<OfficeSuppliesDTO> getAll() {
        List<OfficeSupplies> entities = repository.findByDeletedFalseOrderByCategoryAscNameAsc();
        return entities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 按类别查询
     */
    public List<OfficeSuppliesDTO> getByCategory(String category) {
        List<OfficeSupplies> entities = repository.findByCategoryAndDeletedFalseOrderByStockQuantityAsc(category);
        return entities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 查询库存不足的物品
     */
    public List<OfficeSuppliesDTO> getLowStockItems() {
        List<OfficeSupplies> entities = repository.findByStockQuantityLessThanAndDeletedFalse(5);
        return entities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 入库
     */
    @Transactional
    public OfficeSuppliesDTO stockIn(Long id, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("入库数量必须大于0");
        }

        OfficeSupplies entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("办公用品不存在"));

        entity.setStockQuantity(entity.getStockQuantity() + quantity);
        entity.setLastStockInDate(LocalDate.now());
        updateStatus(entity);

        OfficeSupplies saved = repository.save(entity);
        log.info("入库成功: id={}, quantity={}, newStock={}", id, quantity, saved.getStockQuantity());

        return toVO(saved);
    }

    /**
     * 入库（带记录）
     */
    @Transactional
    public OfficeSuppliesDTO inbound(Long id, Integer quantity, String operator, String remark) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("入库数量必须大于0");
        }

        OfficeSupplies entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("办公用品不存在"));

        entity.setStockQuantity(entity.getStockQuantity() + quantity);
        entity.setLastStockInDate(LocalDate.now());
        updateStatus(entity);

        OfficeSupplies saved = repository.save(entity);

        // TODO: 保存出入库记录到SupplyRecord表

        log.info("入库成功: id={}, quantity={}, operator={}, newStock={}", id, quantity, operator, saved.getStockQuantity());

        return toVO(saved);
    }

    /**
     * 出库
     */
    @Transactional
    public OfficeSuppliesDTO stockOut(Long id, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("出库数量必须大于0");
        }

        OfficeSupplies entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("办公用品不存在"));

        if (entity.getStockQuantity() < quantity) {
            throw new RuntimeException("库存不足，当前库存: " + entity.getStockQuantity());
        }

        entity.setStockQuantity(entity.getStockQuantity() - quantity);
        updateStatus(entity);

        OfficeSupplies saved = repository.save(entity);
        log.info("出库成功: id={}, quantity={}, newStock={}", id, quantity, saved.getStockQuantity());

        return toVO(saved);
    }

    /**
     * 出库（带记录）
     */
    @Transactional
    public OfficeSuppliesDTO outbound(Long id, Integer quantity, String receiver, String purpose) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("出库数量必须大于0");
        }

        OfficeSupplies entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("办公用品不存在"));

        if (entity.getStockQuantity() < quantity) {
            throw new RuntimeException("库存不足，当前库存: " + entity.getStockQuantity());
        }

        entity.setStockQuantity(entity.getStockQuantity() - quantity);
        updateStatus(entity);

        OfficeSupplies saved = repository.save(entity);

        // TODO: 保存出入库记录到SupplyRecord表

        log.info("出库成功: id={}, quantity={}, receiver={}, newStock={}", id, quantity, receiver, saved.getStockQuantity());

        return toVO(saved);
    }

    /**
     * 更新状态（根据库存数量）
     */
    private void updateStatus(OfficeSupplies entity) {
        if (entity.getStockQuantity() == 0) {
            entity.setStatus("OUT_OF_STOCK");
        } else if (entity.getStockQuantity() < entity.getMinStock()) {
            entity.setStatus("LOW_STOCK");
        } else {
            entity.setStatus("IN_STOCK");
        }
    }

    private OfficeSuppliesDTO toVO(OfficeSupplies entity) {
        OfficeSuppliesDTO vo = new OfficeSuppliesDTO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
