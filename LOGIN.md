# 🔥 로그인 흐름 (중요)
   LoginView (FXML)
      ↓
   LoginController
      ↓
   LoginViewModel.login()
      ↓
   AuthService.login()
      ↓
   AuthApiClient → Spring Boot 서버
      ↓
   JWT / Session 수신
      ↓
   UserContext 저장
      ↓
   화면 분기 (POS / Admin)