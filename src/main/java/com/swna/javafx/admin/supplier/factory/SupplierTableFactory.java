package com.swna.javafx.admin.supplier.factory;

import static com.swna.javafx.common.tableutils.TableColumnUtils.buttonColumn;
import static com.swna.javafx.common.tableutils.TableColumnUtils.stringColumn;

import java.util.Comparator;
import java.util.List;

import com.swna.javafx.admin.supplier.domain.Supplier;
import com.swna.javafx.admin.supplier.viewmodel.SupplierViewModel;
import com.swna.javafx.common.constant.IconPaths;
import com.swna.javafx.common.tableutils.TableColumnUtils;
import com.swna.javafx.common.tableutils.TableUtil;
import com.swna.javafx.common.ui.table.TableColumnUtil;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Getter
@Slf4j
@Component
@RequiredArgsConstructor
public class SupplierTableFactory {

    private final SupplierViewModel viewModel;

    private TableView<Supplier> tableView;

    private TableColumn<Supplier, String> noColumn;
    private TableColumn<Supplier, Long> idColumn;
    private TableColumn<Supplier, String> abbrColumn;
    private TableColumn<Supplier, String> nameColumn;
    private TableColumn<Supplier, String> companyColumn;
    private TableColumn<Supplier, String> phoneColumn;
    private TableColumn<Supplier, String> emailColumn;
    private TableColumn<Supplier, String> addressColumn;
    private TableColumn<Supplier, Boolean> activeColumn;
    private TableColumn<Supplier, Void>   colButtonSave;
    private TableColumn<Supplier, Void>   colButtonDelete;

    /**
     * TableView 초기화 및 컬럼 생성 바인딩
     */
    public TableView<Supplier> initializeTable(TableView<Supplier> existingTable) {
        this.tableView = existingTable;
        createColumns();
        configureSorting();
        initTableClickHandlers();

        // 헤더 클릭 정렬 -> SortedList 에 전달
        viewModel.getSortedSuppliers().comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(viewModel.getSuppliers());
        return tableView;
    }

    private void createColumns() {
        // 1. 번호 컬럼
        TableUtil.createNumberColumn(tableView, "NO", 60);
        TableUtil.createCheckBoxHeaderColumn(tableView, Supplier::selectedProperty, "", 50);

        // 2. 약어 컬럼
        this.abbrColumn = stringColumn(tableView, "ABBR", Supplier::abbrProperty)
                .visible(true)
                .alignment(TableUtil.CENTER)
                .fixedWidth(120)
                .build();

        // 3. 담당자명 컬럼
        this.nameColumn = stringColumn(tableView, "NAME", Supplier::nameProperty)
                .setter(Supplier::setName)
                .dirtyConsumer(viewModel::markAsDirty)
                .editable(true)
                .visible(true)
                .alignment(TableUtil.LEFT)
                .fixedWidth(250)
                .build();

        // 4. 회사명 컬럼
        this.companyColumn = stringColumn(tableView, "COMPANY", Supplier::companyProperty)
                .setter(Supplier::setCompany)
                .dirtyConsumer(viewModel::markAsDirty)
                .editable(true)
                .visible(true)
                .alignment(TableUtil.LEFT)
                .fixedWidth(300)
                .build();

        // 5. 전화번호 컬럼
        this.phoneColumn = stringColumn(tableView, "PHONE", Supplier::phoneProperty)
                .setter(Supplier::setPhone)
                .dirtyConsumer(viewModel::markAsDirty)
                .editable(true)
                .visible(true)
                .alignment(TableUtil.CENTER)
                .fixedWidth(150)
                .build();

        // 6. 이메일 컬럼
        this.emailColumn = stringColumn(tableView, "EMAIL", Supplier::emailProperty)
                .setter(Supplier::setEmail)
                .dirtyConsumer(viewModel::markAsDirty)
                .editable(true)
                .visible(true)
                .alignment(TableColumnUtil.LEFT)
                .fixedWidth(200)
                .build();

        // 7. 주소 컬럼
        this.addressColumn = stringColumn(tableView, "ADDRESS", Supplier::addressProperty)
                .setter(Supplier::setAddress)
                .dirtyConsumer(viewModel::markAsDirty)
                .editable(true)
                .visible(true)
                .alignment(TableColumnUtil.LEFT)
                .fixedWidth(300)
                .build();

        // 8. 상태 컬럼 (Boolean 타입 처리 빌더 활용)
        TableColumnUtils.booleanColumn(tableView, "ACTIVE", Supplier::activeProperty)
                .setter((supplier, newValue) -> supplier.setActive(newValue))
                .dirtyConsumer(supplier -> viewModel.markAsDirty(supplier))
                .width(70)
                .build();

        // 9. 액션 버튼 컬럼들
        this.colButtonDelete = buttonColumn(tableView).iconPath(IconPaths.DELETE).width(IconPaths.BUTTOM_WIDTH).action(this::onDeleteAction).build();
        this.colButtonSave = buttonColumn(tableView).iconPath(IconPaths.SAVE).width(IconPaths.BUTTOM_WIDTH).action(this::onSaveAction).build();
    }

    /**
     * 헤더 클릭 정렬 설정
     */
    private void configureSorting() {
        // 문자열 컬럼: 대소문자 무시 + null 안전
        Comparator<String> textComparator = Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER);
        for (TableColumn<Supplier, String> col :
                List.of(abbrColumn, nameColumn, companyColumn, phoneColumn, emailColumn, addressColumn)) {
            col.setSortable(true);
            col.setComparator(textComparator);
        }

        // 번호 / 체크박스 / 버튼 컬럼은 정렬 대상에서 제외
        for (TableColumn<Supplier, ?> col : tableView.getColumns()) {
            String header = col.getText();
            boolean isActionColumn = header == null || header.isBlank() || "NO".equals(header);
            if (isActionColumn) {
                col.setSortable(false);
            }
        }

        // (선택) 기본 정렬: NAME 오름차순 - 필요 없으면 아래 2줄 삭제
        nameColumn.setSortType(TableColumn.SortType.ASCENDING);
        tableView.getSortOrder().add(nameColumn);
    }

    private void onSaveAction(Supplier supplier) {
        if (supplier == null) return;
        log.info("[TableFactory] Save clicked. id={}", supplier.getId());
        viewModel.saveSupplier(supplier);
    }

    private void onDeleteAction(Supplier supplier) {
        if (supplier == null) return;
        log.info("[TableFactory] Delete clicked. id={}", supplier.getId());

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete the supplier '" + supplier.getFullName() + "'?",
                ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(null);

        alert.showAndWait()
                .filter(btn -> btn == ButtonType.OK)
                .ifPresent(btn -> viewModel.deleteSupplier(supplier));
    }

    // =========================================================================
    // TableView Click Handlers
    // =========================================================================
    private void initTableClickHandlers() {
        tableView.setOnMouseClicked(event -> {
            Supplier selectedItem = tableView.getSelectionModel().getSelectedItem();
            if (selectedItem == null) return;

            var posList = tableView.getSelectionModel().getSelectedCells();
            if (posList.isEmpty()) return;

            TableColumn<?, ?> col = posList.get(0).getTableColumn();
            if (col == null || col.getText() == null) return;
        });
    }
}
