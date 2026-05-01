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

*次のステップは phase1-learning.md に追記していく*
