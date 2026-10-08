package com.swna.javafx.admin.user.factory;

import static com.swna.javafx.common.tableutils.TableColumnUtils.buttonColumn;
import static com.swna.javafx.common.tableutils.TableColumnUtils.stringColumn;

import com.swna.javafx.admin.user.domain.Role;
import com.swna.javafx.admin.user.domain.User;
import com.swna.javafx.admin.user.viewmodel.UserViewModel; // UserViewModel 패키지에 맞게 수정
import com.swna.javafx.common.constant.IconPaths;
import com.swna.javafx.common.tableutils.TableColumnUtils;
import com.swna.javafx.common.tableutils.TableUtil;
import com.swna.javafx.common.ui.table.TableColumnUtil;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Getter
@Slf4j
@Component
@RequiredArgsConstructor
public class UserTableFactory {

    private final UserViewModel viewModel;

    private TableView<User> tableView;

    private TableColumn<User, String> noColumn;
    private TableColumn<User, Long> idColumn;
    private TableColumn<User, String> emailColumn;
    private TableColumn<User, String> nameColumn;
    private TableColumn<User, Role> roleColumn; // Enum 타입 컬럼
    private TableColumn<User, String> cityColumn;
    private TableColumn<User, String> streetColumn;
    private TableColumn<User, String> zipcodeColumn;
    private TableColumn<User, String> phoneColumn;
    private TableColumn<User, String> mobileColumn;
    private TableColumn<User, Void> colButtonEdit;
    private TableColumn<User, Void> colButtonDelete;

    /**
     * TableView 초기화 및 컬럼 생성 바인딩
     */
    public TableView<User> initializeTable(
            TableView<User> existingTable,
            Consumer<User> onEditAction,
            Consumer<User> onDeleteAction
    ) {
        this.tableView = existingTable;
        createColumns(onEditAction, onDeleteAction);
        initTableClickHandlers();
        
        tableView.setItems(viewModel.getUsers()); // ViewModel에 유저 리스트 바인딩 가정
        return tableView;
    }

    private void createColumns(Consumer<User> onEditAction, Consumer<User> onDeleteAction) {
        // 1. 번호 및 선택 체크박스 컬럼
        TableUtil.createNumberColumn(tableView, "NO", 60);
        TableUtil.createCheckBoxHeaderColumn(tableView, User::selectedProperty, "", 50);

        // 2. 이메일 컬럼 (고유 ID 역할 및 식별 정보)
        this.emailColumn = stringColumn(tableView, "EMAIL", User::emailProperty)
                .visible(true)
                .alignment(TableUtil.LEFT)
                .fixedWidth(200)
                .build();

        // 3. 이름 컬럼 (편집 가능)
        this.nameColumn = stringColumn(tableView, "NAME", User::nameProperty)
                .setter(User::setName)
                .dirtyConsumer(viewModel::markAsDirty)
                .editable(true)
                .visible(true)
                .alignment(TableUtil.LEFT)
                .fixedWidth(130)
                .build();

        // 4. 권한(Role) 컬럼 - ObjectProperty 활용
        // 필요에 따라 ComboBox 셀 팩토리 등을 연동하여 편집 가능하게 확장할 수 있습니다.
      //   TableColumnUtils.objectColumn(tableView, "ROLE", User::roleProperty)
      //           .setter(User::setRole)
      //           .dirtyConsumer(viewModel::markAsDirty)
      //           .width(100)
      //           .build();

        // 5. 우편번호 컬럼 (편집 가능)
        this.zipcodeColumn = stringColumn(tableView, "ZIPCODE", User::zipcodeProperty)
                .setter(User::setZipcode)
                .dirtyConsumer(viewModel::markAsDirty)
                .editable(true)
                .visible(true)
                .alignment(TableUtil.CENTER)
                .fixedWidth(90)
                .build();

        // 6. 도시 컬럼 (편집 가능)
        this.cityColumn = stringColumn(tableView, "CITY", User::cityProperty)
                .setter(User::setCity)
                .dirtyConsumer(viewModel::markAsDirty)
                .editable(true)
                .visible(true)
                .alignment(TableUtil.LEFT)
                .fixedWidth(130)
                .build();

        // 7. 상세 주소 컬럼 (편집 가능)
        this.streetColumn = stringColumn(tableView, "STREET", User::streetProperty)
                .setter(User::setStreet)
                .dirtyConsumer(viewModel::markAsDirty)
                .editable(true)
                .visible(true)
                .alignment(TableUtil.LEFT)
                .fixedWidth(220)
                .build();

        // 8. 전화번호 컬럼 (편집 가능)
        this.phoneColumn = stringColumn(tableView, "PHONE", User::phoneProperty)
                .setter(User::setPhone)
                .dirtyConsumer(viewModel::markAsDirty)
                .editable(true)
                .visible(true)
                .alignment(TableUtil.CENTER)
                .fixedWidth(140)
                .build();

        // 9. 휴대전화 컬럼 (편집 가능)
        this.mobileColumn = stringColumn(tableView, "MOBILE", User::mobileProperty)
                .setter(User::setMobile)
                .dirtyConsumer(viewModel::markAsDirty)
                .editable(true)
                .visible(true)
                .alignment(TableUtil.CENTER)
                .fixedWidth(140)
                .build();

        // 10. 액션 버튼 컬럼들 (수정 / 삭제)
        this.colButtonDelete = buttonColumn(tableView)
                .iconPath(IconPaths.DELETE)
                .width(IconPaths.BUTTOM_WIDTH)
                .action(onDeleteAction)
                .build();
                
        this.colButtonEdit = buttonColumn(tableView)
                .iconPath(IconPaths.SAVE)
                .width(IconPaths.BUTTOM_WIDTH)
                .action(onEditAction)
                .build();
    }

    // =========================================================================
    // TableView Click Handlers
    // =========================================================================
    private void initTableClickHandlers() {
        tableView.setOnMouseClicked(event -> {
            User selectedItem = tableView.getSelectionModel().getSelectedItem();
            if (selectedItem == null) return;

            var posList = tableView.getSelectionModel().getSelectedCells();
            if (posList.isEmpty()) return;

            TableColumn<?, ?> col = posList.get(0).getTableColumn();
            if (col == null || col.getText() == null) return;
        });
    }
}
