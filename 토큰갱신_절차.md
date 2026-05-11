1. 클라이언트 측 호출 흐름 (JavaFX / WebClient)
클라이언트에서 API 요청 중 토큰이 만료되었을 때의 흐름입니다.

CommonApiClient.postForData(): 서비스 레이어에서 API 호출 시작.

AuthWebClientFilter.filter(): 요청 헤더에 현재(만료된) Access Token 삽입.

ExchangeFunction.exchange(): 서버로 HTTP POST 요청 전송 (/sales 등).

ReAuthWebClientFilter.filter() (응답 수신): 서버로부터 401 Unauthorized 응답을 받음.

ReAuthService.refreshToken(): 401 감지 후 토큰 재발급 로직 진입.

authWebClient.post(): 서버의 /auth/reissue 엔드포인트로 Refresh Token을 담아 요청.

TokenStore.save(): 서버가 새 토큰을 주면 로컬(메모리/파일)에 새 토큰 저장.

retry (재시도): 원래 실패했던 요청(/sales)을 새 토큰으로 다시 실행.


제공해주신 클라이언트와 서버 코드를 기반으로, Access Token이 만료되었을 때 재발급(Reissue)되는 전체 과정의 Call Stack(호출 흐름)을 시각화하여 정리해 드립니다.

이 흐름은 클라이언트의 ReAuthWebClientFilter가 401 에러를 감지하는 순간부터 서버의 DB가 갱신될 때까지의 과정을 포함합니다.

1. 클라이언트 측 호출 흐름 (JavaFX / WebClient)
클라이언트에서 API 요청 중 토큰이 만료되었을 때의 흐름입니다.

CommonApiClient.postForData(): 서비스 레이어에서 API 호출 시작.

AuthWebClientFilter.filter(): 요청 헤더에 현재(만료된) Access Token 삽입.

ExchangeFunction.exchange(): 서버로 HTTP POST 요청 전송 (/sales 등).

ReAuthWebClientFilter.filter() (응답 수신): 서버로부터 401 Unauthorized 응답을 받음.

ReAuthService.refreshToken(): 401 감지 후 토큰 재발급 로직 진입.

authWebClient.post(): 서버의 /auth/reissue 엔드포인트로 Refresh Token을 담아 요청.

TokenStore.save(): 서버가 새 토큰을 주면 로컬(메모리/파일)에 새 토큰 저장.

retry (재시도): 원래 실패했던 요청(/sales)을 새 토큰으로 다시 실행.

2. 서버 측 호출 흐름 (Spring Boot / Security)
재발급 요청(/auth/reissue)을 받은 서버 내부의 동작 과정입니다.

JwtFilter.doFilterInternal(): 요청 수신.

주의: /auth/reissue는 shouldNotFilter에 의해 검증을 통과해야 함.

SecurityFilterChain: permitAll() 설정에 의해 /auth/reissue 컨트롤러로 전달.

AuthController.reissue(TokenRequest): 컨트롤러 진입.

ReissueTokenUseCase.execute(req): 비즈니스 로직 시작.

RefreshTokenRepository.findByToken(): 클라이언트가 보낸 Refresh Token이 DB에 있는지 확인.

AuthDomainService.validateNotExpired(): DB에 저장된 토큰의 만료 시간 확인.

JwtProvider.createAccessToken / createRefreshToken: 새 토큰 쌍 생성.

RefreshTokenRepository.delete(saved): RTR(Rotation)을 위해 기존 토큰 삭제.

RefreshTokenRepository.save(newEntity): 새 Refresh Token 정보 DB 저장.

ApiResponse.success(TokenResponse): 새 토큰 쌍을 클라이언트로 반환.