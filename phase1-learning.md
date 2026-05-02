# Phase 1 学習ガイド

---

## 行うこと

1. Eclipse に Spring Boot プロジェクトをインポートする
2. プロジェクト構造を確認する
3. アプリを起動してブラウザでアクセスする
4. 最初の REST API エンドポイントを作る

---

## 解説

### 1. Eclipse に Spring Boot プロジェクトをインポートする

```
File → Import → Maven → Existing Maven Projects
Root Directory: D:\プログラミング\git\db-management-app\db-management
```

Spring Boot は **Maven** というビルドツールで依存ライブラリを管理している。
`pom.xml` がその設定ファイルで、Eclipseはこれを読んで必要なライブラリを自動ダウンロードする。
初回は数分かかる。

---

### 2. プロジェクト構造を確認する

インポート後、以下のような構造になっている。

```
db-management/
├── src/main/java/com/example/dbmanagement/
│   └── DbManagementApplication.java   ← アプリのエントリーポイント
├── src/main/resources/
│   └── application.properties          ← DB接続などの設定
└── pom.xml                             ← ライブラリ管理
```

**Spring Boot の仕組み**: `@SpringBootApplication` が付いたクラスを起動すると、
内蔵Tomcat（Webサーバー）が自動で立ち上がり、ブラウザからアクセスできるようになる。
サーバーを別途インストールする必要がない。

---

### 3. アプリを起動してブラウザでアクセスする

Eclipse で `DbManagementApplication.java` を右クリック →
**Run As → Spring Boot App**

起動後、ブラウザで `http://localhost:8080` にアクセス。
この時点では何も表示されない（404）が、**サーバーが動いている**ことが確認できる。

---

### 4. 最初の REST API エンドポイントを作る

`src/main/java/com/example/dbmanagement/` に新しいクラスを作る。

```java
// HelloController.java
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, DB Management!";
    }
}
```

アプリを再起動して `http://localhost:8080/hello` にアクセスすると
`Hello, DB Management!` が表示される。

**ポイント**:
- `@RestController` → このクラスがAPIの窓口であることを示す
- `@GetMapping("/hello")` → GETリクエストで `/hello` にアクセスしたときに呼ばれる
- 戻り値の文字列がそのままレスポンスになる

---

---

### 5. Spring Security を開発用に設定する

デフォルトでは Spring Security が自動でログイン画面を表示する。
開発中は邪魔なので、すべてのリクエストを許可する設定にする。

`src/main/java/com/example/db_management/SecurityConfig.java` を新規作成：

```java
package com.example.db_management;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable())
            );
        return http.build();
    }
}
```

**ポイント**：
- `@Configuration` → このクラスが設定クラスであることを示す
- `@EnableWebSecurity` → Spring Security の設定を有効化
- `anyRequest().permitAll()` → すべてのリクエストを認証なしで許可
- `csrf.disable()` → REST APIでは不要なCSRF保護を無効化
- `frameOptions.disable()` → H2コンソール（フレーム使用）を使えるようにする

アプリ再起動後、`http://localhost:8080/hello` がログインなしで表示されればOK。

---

### 6. H2データベースを接続する

H2 はインメモリDB（アプリ起動中のみ存在）。
ローカル開発に最適で、別途インストール不要。

#### pom.xml に依存関係を追加

`<dependencies>` の中に追加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

#### application.properties に設定を追記

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

**ポイント**：
- `mem:testdb` → アプリ起動中のみ存在するインメモリDB
- `ddl-auto=create-drop` → 起動時にテーブル自動生成、停止時に削除
- `h2-console.enabled=true` → ブラウザからDBを操作できる管理画面を有効化

#### 動作確認

アプリ再起動後、`http://localhost:8080/h2-console` にアクセス。
- JDBC URL: `jdbc:h2:mem:testdb`
- User Name: `sa`
- Password: （空欄）

「Connect」を押して管理画面が開けばOK。

---

*次のステップは phase1-learning.md に追記していく*
