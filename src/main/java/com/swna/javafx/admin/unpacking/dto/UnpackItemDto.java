package com.swna.javafx.admin.unpacking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.swna.javafx.admin.unpacking.model.UnpackItem;

import lombok.Builder;

/**
 * 서버 com.swna.server.unpack.dto.UnpackItemDto 와 필드/이름을 동일하게 맞춘
 * REST 통신용 DTO. selected/lineNo/priceoutEstimated 는 클라이언트 UI 전용 상태이므로
 * 서버와 주고받는 이 DTO에는 포함하지 않는다 (fromModel/toModel에서 별도 유지됨).
 * 필드가 20개 넘어 Lombok @Builder를 적용한다.
 */
@Builder
public record UnpackItemDto(
        Long id,
        LocalDateTime created,
        LocalDateTime updated,
        Long unpackId,
        String invoice,
        String barcode,
        String code,
        String description,
        String supplier,
        String category,
        String abbr,
        String comment,
        Integer qty,
        Integer stock,
        Integer minOrderQty,
        Integer minStock,
        BigDecimal amount,
        BigDecimal pricein,
        BigDecimal priceout,
        BigDecimal oldPricein,
        Boolean confirm,
        Boolean isSaved,
        Boolean isNew
) {
    /** JavaFX Model -> DTO (서버 전송용) */
    public static UnpackItemDto fromModel(UnpackItem model) {
        if (model == null) return null;

        return UnpackItemDto.builder()
                .id(model.getId() == 0 ? null : model.getId())
                .unpackId(model.getUnpackId())
                .invoice(model.getInvoice())
                .barcode(model.getBarcode())
                .code(model.getCode())
                .description(model.getDescription())
                .supplier(model.getSupplier())
                .category(model.getCategory())
                .abbr(model.getAbbr())
                .comment(model.getComment())
                .qty(model.getQty())
                .stock(model.getStock())
                .minOrderQty(model.getMinOrderQty())
                .minStock(model.getMinStock())
                .amount(model.getAmount())
                .pricein(model.getPricein())
                .priceout(model.getPriceout())
                .oldPricein(model.getOldPricein())
                .confirm(model.getConfirm())
                .isSaved(model.getIsSaved())
                .isNew(model.getIsNew())
                .build();
    }

    /** DTO -> JavaFX Model (서버 응답 -> 화면 바인딩용). UI 전용 필드는 기본값으로 초기화됨 */
    public UnpackItem toModel() {
        UnpackItem model = new UnpackItem();
        if (id != null) model.setId(id);
        model.setCreated(created);
        model.setUpdated(updated);
        model.setUnpackId(unpackId);
        model.setInvoice(invoice);
        model.setBarcode(barcode);
        model.setCode(code);
        model.setDescription(description);
        model.setSupplier(supplier);
        model.setCategory(category);
        model.setAbbr(abbr);
        model.setComment(comment);
        if (qty != null) model.setQty(qty);
        if (stock != null) model.setStock(stock);
        if (minOrderQty != null) model.setMinOrderQty(minOrderQty);
        if (minStock != null) model.setMinStock(minStock);
        if (amount != null) model.setAmount(amount);
        if (pricein != null) model.setPricein(pricein);
        if (priceout != null) model.setPriceout(priceout);
        if (oldPricein != null) model.setOldPricein(oldPricein);
        if (confirm != null) model.setConfirm(confirm);
        if (isSaved != null) model.setIsSaved(isSaved);
        if (isNew != null) model.setIsNew(isNew);
        return model;
    }
}