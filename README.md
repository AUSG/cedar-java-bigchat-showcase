# 빌드하는 법

먼저 다음 조건을 충족시킨다.

- cargo 설치 (https://www.rust-lang.org/tools/install)
- java 17 이상 설치 (`brew install --cask java`)

이후 ./CedarJava/README.md 설명하는 것처럼,

```sh
# 프로젝트 루트 디렉토리에서 실행할 것
cd CedarJavaFFI
cargo build
cargo test
cd ../CedarJava
bash config.sh
./gradlew build
```

까지 완료한다. 이렇게까지 해두고, CedarJava 디렉토리를 Intellij 로 열면 제대로 인식될 것이다.

# 맛보는 법

프로젝트 자체가 스프링 서버같은게 아니라, Rust 로 작성된 Cedar Engine 을 호출하는 인터페이스이기 때문에, main 패키지를 보지 말고 바로 테스트 코드를 확인하면 된다.

# 포크하면서 변경된 부분들

## 가상의 notion 어플리케이션을 스키마로 만들고 테스트코드로 cedar 엔진 작동 확인

IntegrationTests.java 의 `testNotionSchema` 테스트를 보면 된다.

아래 내용은 해당 테스트의 실행 결과이다. (이렇게 나와야 정상이다) 

```text
User: 김수빈, Action: VIEW_PAGE, Page: 운영진 회의록 --> O
User: 김수빈, Action: VIEW_PAGE, Page: 버디 논의 --> O
User: 김수빈, Action: VIEW_PAGE, Page: 박영진 자기소개 --> O
User: 김수빈, Action: VIEW_PAGE, Page: 김칠기 자기소개 --> O
User: 김수빈, Action: EDIT_PAGE, Page: 운영진 회의록 --> O
User: 김수빈, Action: EDIT_PAGE, Page: 버디 논의 --> O
User: 김수빈, Action: EDIT_PAGE, Page: 박영진 자기소개 --> O
User: 김수빈, Action: EDIT_PAGE, Page: 김칠기 자기소개 --> O
User: 김수빈, Action: TOGGLE_PUBLISH_PAGE, Page: 운영진 회의록 --> O
User: 김수빈, Action: TOGGLE_PUBLISH_PAGE, Page: 버디 논의 --> O
User: 김수빈, Action: TOGGLE_PUBLISH_PAGE, Page: 박영진 자기소개 --> O
User: 김수빈, Action: TOGGLE_PUBLISH_PAGE, Page: 김칠기 자기소개 --> O

User: 문성혁, Action: VIEW_PAGE, Page: 운영진 회의록 --> O
User: 문성혁, Action: VIEW_PAGE, Page: 버디 논의 --> O
User: 문성혁, Action: VIEW_PAGE, Page: 박영진 자기소개 --> O
User: 문성혁, Action: VIEW_PAGE, Page: 김칠기 자기소개 --> O
User: 문성혁, Action: EDIT_PAGE, Page: 운영진 회의록 --> O
User: 문성혁, Action: EDIT_PAGE, Page: 버디 논의 --> O
User: 문성혁, Action: EDIT_PAGE, Page: 박영진 자기소개 --> O
User: 문성혁, Action: EDIT_PAGE, Page: 김칠기 자기소개 --> O
User: 문성혁, Action: TOGGLE_PUBLISH_PAGE, Page: 운영진 회의록 --> X
User: 문성혁, Action: TOGGLE_PUBLISH_PAGE, Page: 버디 논의 --> X
User: 문성혁, Action: TOGGLE_PUBLISH_PAGE, Page: 박영진 자기소개 --> X
User: 문성혁, Action: TOGGLE_PUBLISH_PAGE, Page: 김칠기 자기소개 --> X

User: 박영진, Action: VIEW_PAGE, Page: 운영진 회의록 --> X
User: 박영진, Action: VIEW_PAGE, Page: 버디 논의 --> X
User: 박영진, Action: VIEW_PAGE, Page: 박영진 자기소개 --> O
User: 박영진, Action: VIEW_PAGE, Page: 김칠기 자기소개 --> O
User: 박영진, Action: EDIT_PAGE, Page: 운영진 회의록 --> X
User: 박영진, Action: EDIT_PAGE, Page: 버디 논의 --> X
User: 박영진, Action: EDIT_PAGE, Page: 박영진 자기소개 --> O
User: 박영진, Action: EDIT_PAGE, Page: 김칠기 자기소개 --> X
User: 박영진, Action: TOGGLE_PUBLISH_PAGE, Page: 운영진 회의록 --> X
User: 박영진, Action: TOGGLE_PUBLISH_PAGE, Page: 버디 논의 --> X
User: 박영진, Action: TOGGLE_PUBLISH_PAGE, Page: 박영진 자기소개 --> X
User: 박영진, Action: TOGGLE_PUBLISH_PAGE, Page: 김칠기 자기소개 --> X

User: 김칠기, Action: VIEW_PAGE, Page: 운영진 회의록 --> X
User: 김칠기, Action: VIEW_PAGE, Page: 버디 논의 --> X
User: 김칠기, Action: VIEW_PAGE, Page: 박영진 자기소개 --> O
User: 김칠기, Action: VIEW_PAGE, Page: 김칠기 자기소개 --> O
User: 김칠기, Action: EDIT_PAGE, Page: 운영진 회의록 --> X
User: 김칠기, Action: EDIT_PAGE, Page: 버디 논의 --> X
User: 김칠기, Action: EDIT_PAGE, Page: 박영진 자기소개 --> X
User: 김칠기, Action: EDIT_PAGE, Page: 김칠기 자기소개 --> O
User: 김칠기, Action: TOGGLE_PUBLISH_PAGE, Page: 운영진 회의록 --> X
User: 김칠기, Action: TOGGLE_PUBLISH_PAGE, Page: 버디 논의 --> X
User: 김칠기, Action: TOGGLE_PUBLISH_PAGE, Page: 박영진 자기소개 --> X
User: 김칠기, Action: TOGGLE_PUBLISH_PAGE, Page: 김칠기 자기소개 --> X
```
