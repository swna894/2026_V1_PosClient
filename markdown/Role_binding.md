public class AuthContext {
    private static final AuthContext instance = new AuthContext();
    
    private String token;
    private List<String> roles;

    private AuthContext() {}

    public static AuthContext getInstance() {
        return instance;
    }

    // 로그인 성공 시 호출하여 정보 저장
    public void initSession(String token, List<String> roles) {
        this.token = token;
        this.roles = roles;
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public void clear() {
        this.token = null;
        this.roles = null;
    }
}

===================================================




import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AuthContext {
    private static final AuthContext instance = new AuthContext();

    // 현재 사용자의 권한들을 담을 ObservableList
    private final ObservableList<String> roles = FXCollections.observableArrayList();
    
    // 현재 로그인한 사용자명을 담을 속성 (선택사항)
    private final SimpleObjectProperty<String> username = new SimpleObjectProperty<>();

    private AuthContext() {}

    public static AuthContext getInstance() {
        return instance;
    }

    public void initSession(String token, List<String> userRoles) {
        this.roles.setAll(userRoles);
    }

    public void clear() {
        this.roles.clear();
    }

    public ObservableList<String> getRoles() {
        return roles;
    }
}

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private Button btnSalesManagement; // ADMIN 전용
    @FXML private Button btnStockManagement; // ADMIN 또는 MANAGER용
    @FXML private Button btnPosOrder;         // 모든 권한 가능

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        AuthContext auth = AuthContext.getInstance();

        // 1. ROLE_ADMIN 포함 여부를 감시하는 바인딩 생성
        BooleanBinding isAdmin = Bindings.createBooleanBinding(
                () -> auth.getRoles().contains("ROLE_ADMIN"),
                auth.getRoles() // 이 리스트가 변경될 때마다 다시 계산됨
        );

        // 2. ROLE_ADMIN 또는 ROLE_MANAGER 포함 여부를 감시하는 바인딩 생성
        BooleanBinding isManagerOrAdmin = Bindings.createBooleanBinding(
                () -> auth.getRoles().contains("ROLE_ADMIN") || auth.getRoles().contains("ROLE_MANAGER"),
                auth.getRoles()
        );

        // ==========================================
        // 방법 A: 버튼의 활성화/비활성화 상태를 바인딩 (추천)
        // ================= =========================
        // 관리자가 아니면(!isAdmin) 매출 관리 버튼을 비활성화(Disable)한다.
        btnSalesManagement.disableProperty().bind(isAdmin.not());
        
        // 매니저나 관리자가 아니면 재고 관리 버튼을 비활성화한다.
        btnStockManagement.disableProperty().bind(isManagerOrAdmin.not());


        // ==========================================
        // 방법 B: 버튼의 숨김/보임 상태를 바인딩
        // ==========================================
        /*
        btnSalesManagement.visibleProperty().bind(isAdmin);
        btnStockManagement.visibleProperty().bind(isManagerOrAdmin);
        
        // 팁: visibleProperty만 바인딩하면 버튼이 안 보여도 그 자리가 비어 보일 수 있습니다.
        // 아래 속성까지 같이 바인딩해주면 화면 레이아웃에서 공간까지 완벽히 사라집니다.
        btnSalesManagement.managedProperty().bind(btnSalesManagement.visibleProperty());
        btnStockManagement.managedProperty().bind(btnStockManagement.visibleProperty());
        */
    }
}


import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import java.net.URL;
import java.util.ResourceBundle;

public class ProductListController implements Initializable {

    @FXML private TableView<Product> tvProduct;
    @FXML private TableColumn<Product, String> colProductId;   // 공통
    @FXML private TableColumn<Product, String> colProductName; // 공통
    @FXML private TableColumn<Product, Integer> colPrice;      // 공통 (판매가)
    @FXML private TableColumn<Product, Integer> colCost;       // ★ 관리자 전용 (원가)

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        AuthContext auth = AuthContext.getInstance();

        // 1. ROLE_ADMIN 권한 여부를 감시하는 바인딩 생성
        BooleanBinding isAdmin = Bindings.createBooleanBinding(
                () -> auth.getRoles().contains("ROLE_ADMIN"),
                auth.getRoles()
        );

        // 2. 원가(Cost) 컬럼의 보임 여부를 관리자 권한과 바인딩
        // 관리자일 때만 컬럼이 보이고, 아닐 때는 테이블에서 사라집니다.
        colCost.visibleProperty().bind(isAdmin);
        
        // 데이터 셀렉트 및 셋팅 로직 (기존 코드 유지)
        // tvProduct.setItems(productList);
    }
}
