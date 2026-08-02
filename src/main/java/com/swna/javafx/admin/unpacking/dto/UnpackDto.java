package com.swna.javafx.admin.unpacking.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.swna.javafx.admin.unpacking.model.Unpack;

import lombok.Builder;

/**
 * 서버 com.swna.server.unpack.dto.UnpackDto 와 필드/이름을 동일하게 맞춘
 * REST 통신용 DTO (JSON (de)serialize 대상).
 * 필드가 많아 Lombok @Builder를 적용해 fromModel에서 이름 기반으로 생성한다.
 */
@Builder
public record UnpackDto(
        Long id,
        LocalDateTime created,
        LocalDateTime updated,
        LocalDateTime unpacked,
        String invoice,
        String supplierAbbr,
        String comment,
        Double amount,
        Boolean sync,
        List<UnpackItemDto> items
) {
    /** JavaFX Model -> DTO (서버 전송용) */
    public static UnpackDto fromModel(Unpack model) {
        if (model == null) return null;

        List<UnpackItemDto> itemDtos = model.getItems().stream()
                .map(UnpackItemDto::fromModel)
                .collect(Collectors.toList());

        return UnpackDto.builder()
                .id(model.getId() == 0 ? null : model.getId())
                .created(model.getCreated())
                .updated(model.getUpdated())
                .unpacked(model.getUnpacked())
                .invoice(model.getInvoice())
                .supplierAbbr(model.getSupplierAbbr())
                .comment(model.getComment())
                .amount(model.getAmount())
                .sync(model.getSync())
                .items(itemDtos)
                .build();
    }

    /** DTO -> JavaFX Model (서버 응답 -> 화면 바인딩용) */
    public Unpack toModel() {
        Unpack model = new Unpack();
        if (id != null) model.setId(id);
        model.setCreated(created);
        model.setUpdated(updated);
        model.setUnpacked(unpacked);
        model.setInvoice(invoice);
        model.setSupplierAbbr(supplierAbbr);
        model.setComment(comment);
        if (amount != null) model.setAmount(amount);
        if (sync != null) model.setSync(sync);
        if (items != null) {
            model.setItems(items.stream().map(UnpackItemDto::toModel).collect(Collectors.toList()));
        }
        return model;
    }
}