# 맛남의 광장 프로젝트 점검 결과

점검일: 2026-10-06. 요청에 따라 분석만 수행했으며 앱·서버 소스와 운영 DB 데이터는 수정하지 않았다.

## 점검 대상과 주의사항

- Android: GitHub `baegi7780-png/1006matA`, 커밋 `c8cc2a8b2e2d2dc015cc0f1197ba3f5806595b69`.
- 서버: GitHub `baegi7780-png/1006matS`, 커밋 `eff413dc01f0be545fce5b2b327c4fa73c072ca1`.
- SQL: `C:\Users\qorlc\Desktop\new\mat3.sql`.
- 실제 DB: 로컬 MySQL `motjip_db`의 스키마 및 일부 집계만 읽기 전용 확인.
- GitHub 코드와 기존 로컬 작업본이 달라 `review-1006matA`, `review-1006matS` 폴더에 별도 복제해 검사했다. 실제 작업 폴더를 덮어쓰지 않았다.
- Android 검사용 복제본에는 기존 환경의 local.properties와 google-services.json만 복사했다. 원본 설정은 수정하지 않았다.
- 정적 코드 분석으로 확인한 결함과 실제 빌드 결과를 구분한다. 타인의 메시지 삭제, 계정 위장, DB 초기화 등 위험한 재현은 하지 않았다. 모든 화면·외부 서비스의 종단 간 동작을 보증하는 결과는 아니다.

## 가장 먼저 해결할 문제

### 1. [높음] GitHub와 현재 작업본의 기능 구성이 다름

요청한 두 GitHub 저장소에는 기존 로컬에 있는 신고·관리자·채팅 필터 관련 구현이 포함되어 있지 않다. 서버 ReportController/ReportService, 관리자 관련 클래스, ChatModerationService와 단어 자료, Android 관리자·신고 클래스 등이 누락되어 있다.

영향: 해당 저장소를 새로 받아 실행하면 지금까지 작업한 기능을 그대로 재현할 수 없다. DB에 ADMIN 역할이 있어도 관리자 API·화면이 없는 버전에서는 그 기능을 사용할 수 없다.

권장: 먼저 어느 로컬 작업본이 배포 기준인지 확정한 뒤, 기존 변경을 보존하면서 누락 파일과 관련 변경을 함께 검토한다. 단순히 GitHub 코드를 기존 폴더에 덮어쓰면 안 된다.

### 2. [매우 높음] 로그인 사용자와 요청 memberId를 연결하지 않는 API

근거: 서버 ChatController.java:32, 45, 57, 117, 133. ReviewController.java:122, 159 및 ReviewService.java:313, 373.

인증 자체는 SecurityConfig에서 요구하지만, 여러 채팅 API가 요청의 memberId/roomId/messageId를 신뢰하며 실제 로그인 사용자와 방 참여 여부·작성자 여부를 대조하지 않는다. 리뷰 수정·삭제도 요청자가 전달한 memberId를 작성자와 비교한다.

영향: 로그인한 사용자가 요청 ID를 바꾸면 타인의 채팅 목록·메시지에 접근하거나 메시지를 삭제할 수 있는 코드 경로가 있다. 리뷰는 작성자 ID를 함께 전달해 소유자 검사 우회가 가능하다. 파괴적인 실제 요청은 수행하지 않았다.

권장: JWT에서 서버가 사용자 ID를 결정하고, 각 API에서 활성 방 참여자·작성자 권한을 검증한다. 클라이언트가 버튼을 숨기는 것만으로는 해결되지 않는다.

### 3. [매우 높음] 실시간 채팅 발신자·구독 권한 검증 누락

근거: 서버 ChatController.java:104, ChatMessageService.java:649, WebSocketConfig.java.

메시지 전송이 ChatMessage 엔티티를 직접 받아 senderId와 roomId를 사용해 저장한다. STOMP SEND/SUBSCRIBE에 사용자·방 권한을 확인하는 inbound interceptor가 없다. HTTP 연결 인증과 메시지 단위 권한 검증은 별개다.

영향: 인증된 연결에서 senderId를 바꿔 다른 사람 이름으로 보내거나, 권한 없는 방의 토픽을 구독할 위험이 있다.

권장: 발신자를 인증 Principal에 고정하고, SEND와 SUBSCRIBE 모두 활성 참여자 검증을 추가한다. 입력 DTO에 클라이언트가 수정하면 안 되는 ID·시스템 메시지 속성을 노출하지 않는다.

### 4. [높음] mat3.sql과 서버/실제 DB의 스키마 불일치

근거: mat3.sql:74의 communities 정의, 서버 domain/Community.java:68.

서버는 communities.chat_link를 매핑하지만 SQL에는 컬럼이 없다. 실제 DB에는 이 컬럼이 있다. 새 DB를 SQL로만 만들면 Hibernate 설정에 따라 스키마 검증 실패 또는 컬럼 조회 오류가 발생할 수 있다.

또한 기존 로컬의 확장 기능에 필요한 members의 관리자 역할·정지 컬럼, communities/reviews의 숨김 컬럼, 제재·필터 로그 테이블 등이 실제 DB에는 있지만 SQL에는 반영되지 않았다. SQL의 community_members.role은 사용자 관리자 권한 컬럼과 다른 용도다.

특히 SQL 첫 줄은 DROP DATABASE IF EXISTS motjip_db다. 운영 DB에 전체 SQL을 재실행하는 것은 업데이트가 아니라 기존 데이터를 삭제하는 초기화다.

권장: 기존 데이터를 유지하는 버전별 변경 SQL을 관리하고, 초기 설치 SQL도 최신 스키마와 일치시킨다. 현재 DB 백업을 확보한 후 진행한다.

## 채팅 흐름과 관련된 결함

### 5. [높음] 나가기/초대 성공을 연결 실패로 표시할 수 있음

근거: 서버 ChatController.java:133, 178. Android API/ApiService.java:157, 184, API/RetrofitClient.java:320, MessageActivity.java:1571.

서버는 성공 시 JSON이 아닌 일반 문자열을 보내는데 Android는 GsonConverterFactory의 Call<String>으로 읽는다. 응답 본문 변환에 실패하면 서버에서 처리가 끝났어도 onFailure가 실행되어 연결 실패로 표시되고 화면 이동을 하지 않는다.

권장: 응답을 공통 JSON DTO로 통일하거나, 본문이 필요 없는 API는 서버·클라이언트 계약에 맞게 Void로 처리한다. 서버 성공/HTTP 실패/네트워크 실패/후속 목록 갱신 실패를 구분해야 한다.

### 6. [높음] 마지막 참여자 나가기와 방 삭제의 트랜잭션 분리

근거: ChatController.java:133, ChatService.java:476, ChatMessageService.java:1207, ChatMessageRepository.java:99.

나가기 상태 저장은 트랜잭션 안에서 끝나지만, 이어 호출하는 빈 방 삭제는 트랜잭션 밖에서 파생 deleteByRoomId를 수행한다. 트랜잭션 요구 오류가 발생할 수 있으며, 이미 나간 상태만 저장된 뒤 요청 전체가 실패할 수 있다.

추가로 communities.room_id는 채팅방을 참조하는 외래키이며 SQL에 삭제 cascade가 없어, 모임과 연결된 방의 물리 삭제 정책도 필요하다.

권장: 나가기·방장 승계·빈 방 정리를 한 서비스의 트랜잭션으로 설계하고, 모임 연결 및 관련 읽음/참여자 데이터 처리 순서를 명확히 한다. 재요청을 안전하게 처리하는 정책도 필요하다. 실제 방 삭제 재현은 하지 않았다.

### 7. [높음] 1명 선택도 GROUP 생성, DIRECT 초대도 제한 없음

근거: Android GroupCreateActivity.java:713, 791. 서버 ChatMessageService.java:443, ChatService.java:130, 275.

그룹 생성 화면은 선택 인원과 무관하게 GROUP을 보낸다. 서버의 기존 1:1 재사용 로직은 DIRECT와 총 참여자 2명 조건에만 적용된다. 따라서 상대 1명을 선택해도 친구/모임의 1:1 경로와 다른 방이 생길 수 있다.

반대로 DIRECT 방에 추가 참여자를 초대하는 서버 로직에는 방 유형 제한이 없어 1:1 타입이면서 실제로 3명 이상인 방이 될 수 있다. DIRECT 재사용도 DB의 사용자 쌍 고유 제약 없이 검색하므로 동시 생성 시 중복 가능성이 남는다.

권장: 상대 1명은 DIRECT 재사용, 상대 2명 이상은 GROUP 생성으로 모든 진입점을 통일하고, DIRECT의 추가 초대를 금지하거나 명시적인 그룹 전환 정책을 정한다. 기존 그룹 데이터의 병합/삭제는 별도 선택 사항이다.

### 8. [중간] 방장 나가기 후 승계 없음

근거: ChatService.java:477의 leaveChatRoom.

현재 GitHub 구현에는 나가는 사용자가 owner인지 확인하고 다음 활성 참여자에게 ownerId를 넘기는 코드가 없다. 이후 실제 참여자가 아닌 사용자가 owner로 남을 수 있다.

권장: 남은 참여자에 대한 확정적인 정렬 기준을 정하고 해당 첫 사용자에게 승계한다. 현재 실제 DB 집계에서 나간 방장이 남은 사례는 발견되지 않았으므로 현재 발생 중인 오류로 단정하지 않는다.

### 9. [중간] 읽음 숫자와 현재 접속 상태 계산의 경계 조건

근거: ChatMessageService.java:901, 932; ChatService.java:36.

읽지 않은 인원은 현재 활성 참여자 수에서 해당 메시지의 전체 읽음 기록 수를 빼는 방식이다. 나간 사람의 읽음 기록과 새 참여자의 과거 메시지 접근 기준이 분리되지 않아 인원 변경 시 잘못 계산될 수 있다. 예: A/B/C 중 A와 B가 읽고 B가 나가면, C가 안 읽었어도 2-2=0이 된다.

현재 보고 있는 방은 메모리의 memberId→roomId 한 개로 관리한다. 비정상 종료·통신 단절의 해제/만료 처리가 없어 보고 있지 않은 사용자의 메시지를 읽음 처리하거나 알림을 생략할 위험도 있다. 읽음 저장 오류를 광범위하게 무시하는 코드도 있다.

권장: 활성 참여자 기준으로 읽음 집계를 계산하고 가입 시점/퇴장 정책을 정한다. 접속 상태는 연결별 수명 및 disconnect/timeout 처리를 갖춘다.

### 10. [중간] 메시지 최근 50개만 제공

근거: ChatMessageService.java:355의 PageRequest.of(0, 50), ChatController.java:57.

메시지 API에 이전 페이지를 요청하는 인자가 없다. 로컬 캐시가 없는 새 기기/재설치에서는 오래된 대화를 불러올 수 없다. 채팅 목록 및 읽음 계산에는 방별 전체 메시지 조회·반복 조회도 있어 데이터가 늘면 느려질 수 있다.

권장: 메시지 ID/시간 기반 이전 내역 페이지 조회와 읽음 집계 쿼리를 도입하고, 실제 데이터량으로 조회 성능을 측정한다.

## 보안·입력·운영 관련 추가 사항

- **[높음] 토큰 로그 노출:** config/JwtAuthenticationFilter.java:90에서 Authorization 헤더를 그대로 기록한다. 로그 열람자가 인증 토큰을 얻을 수 있으므로 값 자체를 기록하지 않아야 한다.
- **[중간] 공개 테스트 푸시 경로:** SecurityConfig에서 /fcm/test를 인증 없이 허용한다. 배포 환경에서는 제거 또는 관리자/개발환경 제한이 필요하다.
- **[중간] 업로드 검증과 채팅 사진 접근:** FileUploadController는 원래 확장자를 유지하며 파일 타입 검증 없이 저장한다. /uploads/**는 인증 없이 접근 가능하다. UUID 주소가 비밀방 참여 권한을 대신하지는 못한다. 타입/크기/콘텐츠 검증과 사진 공개 범위를 정해야 한다.
- **[중간] 입력 기준 불일치:** CommunityController는 제목 25자, CommunityValidator는 20자 기준을 사용하며 Android에도 25/20 표시가 섞여 있다. Validator의 링크 최대 300자와 Community 컬럼 255자도 다르다. 하나의 기준으로 맞춰야 한다.
- **[중간] 토큰 갱신 동시 요청:** Android Retrofit의 401 처리와 서버의 refresh-token 교체가 동시에 여러 번 실행되면 한 요청은 성공하고 다른 요청은 이전 refresh-token으로 실패할 수 있다. 단일 갱신 공유 처리가 필요하다. 병렬 재현은 하지 않았다.
- **[중간] 재현 가능한 설치 설정 부족:** GitHub 서버에는 실행용 resources/설정 예시가 없고 Android 빌드는 local.properties를 직접 읽는다. 비밀 설정을 Git에 넣으면 안 되지만, 예시 설정·환경변수 목록·별도 테스트 프로필·실행 안내는 필요하다.
- **테스트 범위 부족:** 서버 테스트는 contextLoads 1개뿐이며 사용자 권한, 1:1 중복, 마지막 나가기, 읽음, 신고/제재 등을 검증하지 않는다. Android의 예제 테스트만으로 실제 기능 흐름을 보증할 수 없다.

## 검증 결과

- 서버 compileJava: 통과.
- 서버 test: 1개 실행, contextLoads 실패. 제공된 GitHub 설정만으로 DataSource를 구성하지 못함. 오류는 `Failed to determine a suitable driver class`이며 JDBC 설정 누락 상태로 확인했다. 운영 DB 설정을 복사해 테스트하지 않았다.
- Android compileDebugJavaWithJavac 및 assembleDebug: 통과. 검사용 복제본에서 app-debug.apk 생성 확인.
- Android lintDebug: 실패, 오류 4개·경고 515개. 경고 개수가 실제 버그 개수는 아니다.
  - WriteActivity.java:912 — MissingSuperCall. 작성 취소 확인창을 위한 의도적인 뒤로가기 재정의이므로, 무조건 super 호출을 추가하면 기존 흐름이 바뀐다. OnBackPressedDispatcher 방식으로 정리하거나 의도를 검증한 뒤 처리해야 한다.
  - Fragment/ChatFragment.java:724 및 Fragment/ProfileFragment.java:257 — UnspecifiedRegisterReceiverFlag. 다만 코드 확인 결과 Android 13 이상 분기에는 RECEIVER_NOT_EXPORTED가 이미 들어 있고, 지적된 호출은 그 미만 분기다. **lint가 보고했지만 Android 14에서 반드시 충돌하는 확정 결함은 아니다.** ContextCompat 방식으로 통일해 경고를 해소하고 API별 확인하는 것이 적절하다.
  - res/layout/item_suggestion.xml:15 — AppCompat 이미지 tint에 android:tint 대신 app:tint 사용 필요.
- 서버 테스트 보고서: `C:\Users\qorlc\Desktop\motjipS\review-1006matS\build\reports\tests\test\index.html`.
- Android lint 보고서: `C:\Users\qorlc\Desktop\matzidoA\review-1006matA\app\build\reports\lint-results-debug.html`.
- 소스 컴파일 통과는 권한·DB 스키마·실시간 채팅·화면 동작이 정상이라는 뜻이 아니다.

## 권장 작업 순서

1. GitHub/로컬/DB의 배포 기준 버전부터 맞추기. 기존 기능을 덮어쓰지 않고 누락 차이를 검토.
2. HTTP·WebSocket 사용자/방 권한과 토큰 로그를 우선 수정.
3. 응답 형식, 마지막 참여자 나가기, 1:1 생성 및 방장 승계 수정.
4. 스키마 변경 SQL·테스트 환경·기능 회귀 테스트 구축.
5. 읽음/접속 상태, 과거 대화 조회, 성능 및 업로드 정책 보완.

UI 개편과 기존 방 데이터 병합/삭제는 이번 분석에 포함하지 않았으며 별도 요청과 선택이 필요하다.
