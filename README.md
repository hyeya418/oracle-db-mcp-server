# Oracle DB MCP Server

Spring AI 기반의 Oracle DB 읽기 전용 MCP(Model Context Protocol) 서버입니다.
Claude Desktop / Claude Code에서 자연어로 Oracle DB를 조회할 수 있습니다.

## 기술 스택

- Java 17
- Spring Boot 3.5.15
- Spring AI 1.1.7 (`spring-ai-starter-mcp-server`)
- ojdbc11 23.4.0.24.05
- 전송 방식: STDIO

## 제공 도구 (MCP Tools)

| 도구 | 설명 |
|------|------|
| `listTables` | 현재 스키마의 테이블 목록 조회 |
| `describeTable` | 특정 테이블의 컬럼명 / 타입 / Nullable 조회 |
| `executeQuery` | SELECT 쿼리 실행 (최대 100행) |

## 보안 (읽기 전용 강제)

`SqlValidator`가 실행 전 SQL을 검증합니다.

- `SELECT` 또는 `WITH`(CTE)로 시작하는 단일 쿼리만 허용
- `INSERT / UPDATE / DELETE / DROP / ALTER / TRUNCATE / MERGE / CREATE` 등 차단
- 세미콜론(`;`)으로 여러 쿼리 연결 차단
- `--` / `/* */` 주석을 제거한 뒤 검증 (주석으로 키워드 우회 방지)
- `FETCH FIRST 100 ROWS ONLY` 자동 적용 (이미 제한이 있으면 존중)

## 환경 변수

하드코딩 없이 환경변수로 DB 접속 정보를 주입합니다.

| 변수 | 예시 |
|------|------|
| `DB_URL` | `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` |
| `DB_USERNAME` | `shop` |
| `DB_PASSWORD` | `****` |

## 빌드

```bash
# JDK 17 필요
./mvnw clean package -DskipTests
```

빌드 결과: `target/oracle-db-mcp-server-0.0.1-SNAPSHOT.jar`

## Claude Desktop 연동

`%APPDATA%\Claude\claude_desktop_config.json`에 추가:

```json
{
  "mcpServers": {
    "oracle-db": {
      "command": "C:\\Program Files\\Java\\jdk17\\bin\\java.exe",
      "args": [
        "-jar",
        "C:\\project\\oracle-db-mcp-server\\target\\oracle-db-mcp-server-0.0.1-SNAPSHOT.jar"
      ],
      "env": {
        "DB_URL": "jdbc:oracle:thin:@//localhost:1521/FREEPDB1",
        "DB_USERNAME": "your_username",
        "DB_PASSWORD": "your_password"
      }
    }
  }
}
```

설정 후 Claude Desktop 재시작.

## Claude Code 연동

프로젝트 루트의 `.mcp.json`으로 자동 인식됩니다 (`.gitignore`에 포함되어 있으므로 직접 생성):

```json
{
  "mcpServers": {
    "oracle-db": {
      "command": "C:\\Program Files\\Java\\jdk17\\bin\\java.exe",
      "args": [
        "-jar",
        "C:\\project\\oracle-db-mcp-server\\target\\oracle-db-mcp-server-0.0.1-SNAPSHOT.jar"
      ],
      "env": {
        "DB_URL": "jdbc:oracle:thin:@//localhost:1521/FREEPDB1",
        "DB_USERNAME": "your_username",
        "DB_PASSWORD": "your_password"
      }
    }
  }
}
```

## 로그

STDIO 전송 특성상 stdout은 JSON-RPC 전용이므로, 로그는 파일로만 출력됩니다.

기본 위치: `~/oracle-db-mcp-server.log`
