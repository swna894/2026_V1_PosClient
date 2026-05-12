# 관련 파일
   1. ApiProperties.java : application.properties api: base-url: https://google.com 이 있으면 해당 값이 저장.
   2. WebClientConfig.java : ApiProperties 기본 url, token 적용


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