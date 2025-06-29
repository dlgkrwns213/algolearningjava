# AlgoLearningJava

이 프로젝트는 Java와 Spring Boot를 기반으로 한 알고리즘 학습 및 실습 웹 애플리케이션입니다. 백엔드(Spring Boot)와 프론트엔드(HTML, CSS, JS, Thymeleaf 등)를 모두 포함하여, 알고리즘 문제 풀이와 웹 개발을 함께 경험할 수 있습니다.

## 프로젝트 구조

```
algolearningjava/
├── src/
│   ├── main/
│   │   ├── java/           # 백엔드(Spring Boot) 및 알고리즘 풀이 코드
│   │   ├── resources/      # 설정 파일, 정적 리소스
│   │   ├── static/         # 프론트엔드 정적 파일(HTML, CSS, JS)
│   │   └── templates/      # Thymeleaf 템플릿
│   └── test/               # 테스트 코드
├── build.gradle            # Gradle 빌드 설정
├── README.MD               # 프로젝트 소개 및 안내
└── ...
```

## 주요 기능

- 다양한 알고리즘 문제 풀이 예제(JAVA)
- Spring Boot 기반 REST API 및 웹 애플리케이션
- 프론트엔드와 백엔드 연동 실습
- 학습한 내용 및 참고 자료 정리

## 시작하는 방법

1. 저장소를 클론합니다.
2. Java 17 이상과 Gradle이 설치되어 있는지 확인합니다.
3. IDE에서 프로젝트를 열고, 터미널에서 `./gradlew bootRun`을 실행합니다.
4. 웹 브라우저에서 `http://localhost:8080`에 접속하여 애플리케이션을 확인합니다.

## 기여 방법

- 이슈 등록 또는 PR(Pull Request) 제출
- 코드 컨벤션 및 프로젝트 구조 준수

## 라이선스

MIT License