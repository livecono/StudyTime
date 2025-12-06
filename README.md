# StudyTime - 안드로이드 공부 집중 앱

## 프로젝트 설정 및 실행 가이드

### 필수 요구사항
- Android Studio (최신 버전)
- Java 11 이상
- Android 8.0 (API 26) 이상의 기기 또는 에뮬레이터

### 프로젝트 설정 방법

#### 1단계: Android Studio에서 프로젝트 열기
1. Android Studio 실행
2. File → Open
3. `StudyTime` 폴더 선택
4. "Trust Project" 클릭

#### 2단계: Gradle 동기화
- Android Studio가 자동으로 Gradle 동기화를 제안할 때 "Sync Now" 클릭
- 또는 File → Sync Project with Gradle Files

#### 3단계: SDK 설정 확인
- File → Project Structure
- SDK Location이 올바르게 설정되어 있는지 확인
- "Android SDK" 항목에서 API 26 이상이 설치되어 있는지 확인

#### 4단계: 에뮬레이터/기기 준비
- AVD Manager에서 Android 8.0 이상 에뮬레이터 생성
- 또는 Android 기기를 USB로 연결

#### 5단계: 앱 실행
- 프로젝트에서 Run → Run 'app' 선택
- 또는 Shift + F10 단축키 사용
- 에뮬레이터 또는 기기 선택 후 실행

### 주요 기능

#### 1. 타이머
- **기본값**: 5, 10, 15, 25, 50분
- **커스텀 타이머**: 1~1440분 범위에서 자유롭게 추가 가능
- **자동 저장**: SharedPreferences에 설정값 저장

#### 2. 집중 모드
- **Device Admin**: 타이머 시작 시 기기 자동 잠금
- **화면 유지**: PowerManager.WakeLock으로 화면 항상 켜짐
- **첫 실행 권한**: 앱 첫 실행 시 Device Admin 권한 자동 요청

#### 3. 완료 보상
- **칭찬 메시지**: 20개의 랜덤 메시지 중 하나 표시
- **알림**: 완료음 재생 및 진동 피드백

#### 4. 설정 관리
- **커스텀 타이머 관리**: 추가된 타이머 확인 및 삭제
- **장기 누르기**: 커스텀 타이머 장기 누르기로 빠르게 삭제 가능

### 프로젝트 구조

```
StudyTime/
├── app/
│   ├── src/main/
│   │   ├── kotlin/com/studytime/
│   │   │   ├── MainActivity.kt              # 메인 타이머 화면
│   │   │   ├── SettingsActivity.kt          # 설정 화면
│   │   │   ├── AdminReceiver.kt             # Device Admin 리시버
│   │   │   ├── TimerManager.kt              # 타이머 로직
│   │   │   ├── MessageRepository.kt         # 칭찬 메시지 관리
│   │   │   └── PreferenceManager.kt         # SharedPreferences 관리
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml
│   │   │   │   └── activity_settings.xml
│   │   │   ├── xml/
│   │   │   │   └── admin_receiver.xml       # Device Admin 정책
│   │   │   └── values/
│   │   │       ├── strings.xml
│   │   │       └── themes.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
└── gradlew.bat
```

### 사용 방법

#### 타이머 시작
1. 기본값 버튼(5, 10, 15, 25, 50분) 중 하나 클릭 또는 커스텀 시간 입력
2. "시작" 버튼 클릭
3. Device Admin 권한이 있으면 기기가 자동으로 잠김
4. 타이머 진행 중 화면이 계속 켜져 있음

#### 타이머 일시정지/재개
1. 타이머 실행 중 "일시정지" 버튼 클릭
2. "시작" 버튼이 "재개"로 변경됨
3. "재개"를 클릭하면 남은 시간부터 다시 시작

#### 타이머 중지
1. 타이머 실행 중 "중지" 버튼 클릭
2. 타이머가 즉시 중지되고 리셋됨
3. 기기 잠금이 해제됨

#### 커스텀 타이머 추가
1. 메인 화면의 분 입력 필드에 원하는 분 수 입력
2. "추가" 버튼 클릭
3. 추가된 타이머가 커스텀 타이머 목록에 표시됨

#### 커스텀 타이머 삭제
1. 메인 화면: 커스텀 타이머 버튼을 장기 누르기 또는 설정 화면에서 삭제
2. 설정 화면: 삭제 버튼 클릭

### 권한 설정

#### Device Admin 권한 (필수)
- 집중 모드 사용을 위해 반드시 필요
- 앱 첫 실행 시 자동으로 요청
- 기기 설정 → 보안 → Device Admin에서 수동으로 관리 가능

#### 기타 권한
- WAKE_LOCK: 화면 유지
- VIBRATE: 진동 피드백
- SYSTEM_ALERT_WINDOW: Lock Screen 위 표시 (선택사항)

### 문제 해결

#### 앱이 실행되지 않는 경우
1. Android Studio에서 Gradle 동기화 재실행
2. File → Invalidate Caches → Invalidate and Restart
3. 에뮬레이터 재시작

#### Device Admin 권한이 요청되지 않는 경우
1. 앱 삭제 후 재설치
2. 또는 기기 설정에서 수동으로 권한 부여

#### 타이머가 작동하지 않는 경우
1. Device Admin 권한이 활성화되어 있는지 확인
2. 기기 배터리 최적화 설정 확인 (StudyTime 앱이 최적화 제외 목록에 있는지)

### 주의사항
- 타이머 실행 중 안드로이드 기기를 재부팅하거나 앱을 강제 종료해도 타이머가 복구되지 않습니다 (의도된 동작)
- Device Admin 권한은 한 번 설정하면 유지되므로, 권한을 해제하려면 기기 설정에서 수동으로 해제해야 합니다

### 라이선스
MIT License

### 개발자
StudyTime Development Team
