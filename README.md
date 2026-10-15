# RAG 期末复习 Q&A 助手

> 一个基于 **Spring Boot 3 + Spring AI + MongoDB** 的期末复习刷题小站：
> 上传 Word 讲义（或直接粘贴文本），调用大模型自动生成单项选择题，支持答题、错题本、收藏、学习统计和知识点思维导图。

---

## ✨ 功能一览

- **注册 / 登录**（用户名密码持久化在 MongoDB）
- **文档上传出题**：上传 `.docx` 讲义 → Apache POI 抽取文本 → LLM 生成题目
- **粘贴文本题**：直接在页面输入文本，一键生成题目
- **可控题目数量**：严格 1~50 道，模型返回不足会自动重试补齐，超出则截断
- **答题 & 判分**：即时反馈正确 / 错误 + 解析
- **错题本**：答错自动入库，答对自动移除，可手动清理
- **收藏**：任意题目一键收藏，随时回看
- **学习统计**：总题数、答题数、正确数、正确率、错题数、收藏数
- **知识点导图**：按源文档聚类展示题目分布
- **中英文界面**：内置 i18n 切换

---

## 🧱 技术栈

| 层级 | 选型 |
| --- | --- |
| 后端框架 | Spring Boot 3.2.4 |
| AI 框架 | Spring AI 1.0.0-M2（`spring-ai-openai-spring-boot-starter`） |
| 大模型 | 通过 OpenAI 兼容接口接入 [SiliconFlow](https://siliconflow.cn) 的 `deepseek-ai/DeepSeek-V3.2`（可自由替换） |
| 数据库 | MongoDB 6+（Spring Data MongoDB） |
| 文档解析 | Apache POI 5.2.5（`poi-ooxml`） |
| JSON | Jackson |
| 前端 | 原生 HTML + CSS + JavaScript 单页应用（`src/main/resources/static/index.html`） |
| 构建 | Maven 3.9+ |
| JDK | Java 17+ |

---

## 📦 项目结构

```
SpringAI/
├── pom.xml
├── README.md
├── .gitignore
├── application-local.properties.example     # 本地覆盖配置模板（拷自一份自己填）
├── application-local.properties             # ★ 你自己创建，已 gitignore，不会入库
└── src/main/
    ├── java/org/example/quizapp/
    │   ├── QuizApplication.java              # Spring Boot 启动类
    │   ├── controller/
    │   │   ├── QuizController.java           # 出题、答题、错题、收藏、统计、知识地图
    │   │   └── UserController.java           # 注册、登录
    │   ├── service/
    │   │   ├── RagService.java               # 调用 LLM 生成题目 + JSON 解析
    │   │   ├── QuizService.java              # 题目、答题、错题、收藏业务
    │   │   └── UserService.java              # 用户业务
    │   ├── repository/                        # MongoDB Spring Data 仓库
    │   ├── model/                             # MongoDB 文档实体
    │   ├── dto/                               # 请求 / 响应封装
    │   └── util/WordDocumentReader.java       # .docx 文本抽取
    └── resources/
        ├── application.properties            # 主配置（无真实 Key）
        └── static/index.html                 # 前端页面
```

---

## 🚀 快速开始

### 1. 环境准备

确保本机已安装：

- **JDK 17 及以上**（推荐 17 或 21）
- **Maven 3.9+**
- **MongoDB 6+**，默认连接 `mongodb://localhost:27017`
- 一个 **SiliconFlow**（或其他 OpenAI 兼容服务）账号，拿到 API Key

> MongoDB 若还没安装，可用 Docker 一键拉起：
>
> ```bash
> docker run -d --name mongo -p 27017:27017 mongo:7
> ```

### 2. 克隆仓库

```bash
git clone https://github.com/<你的用户名>/SpringAI.git
cd SpringAI
```

### 3. 配置 API Key

仓库里 `application.properties` **不再存放真实密钥**，主配置如下（Key 使用占位符 + 环境变量默认值）：

```properties
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=quiz_rag_db

spring.ai.openai.base-url=https://api.siliconflow.cn
spring.ai.openai.api-key=${SILICONFLOW_API_KEY:sk-请填入你的-API-Key}
spring.ai.openai.chat.options.model=deepseek-ai/DeepSeek-V3.2

spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

spring.config.import=optional:file:./application-local.properties
```

选择以下任一方式提供你的 Key 即可（**都不需要改主配置**）：

**方式 A：本地覆盖文件（推荐）**

```bash
# Windows PowerShell
copy application-local.properties.example application-local.properties
# Linux / macOS
cp application-local.properties.example application-local.properties
```

然后编辑 `application-local.properties`，写入：

```
spring.ai.openai.api-key=sk-你-的-key
```

该文件已在 `.gitignore` 中忽略，可以放心放在项目根目录。

**方式 B：环境变量**

```powershell
# PowerShell
$env:SILICONFLOW_API_KEY = "sk-你-的-key"
mvn spring-boot:run
```

```bash
# Linux / macOS
export SILICONFLOW_API_KEY=sk-你-的-key
mvn spring-boot:run
```

> 🔐 请到 [SiliconFlow 控制台](https://cloud.siliconflow.cn/account/ak) 生成自己的 Key；
> **千万不要把真实 Key 提交到 Git**，公开仓库里的 Key 会在几小时内被扫走并盗用额度。

### 4. 启动项目

```bash
# Windows PowerShell
mvn spring-boot:run

# 或先打包再运行
mvn -DskipTests clean package
java -jar target/SpringAI-1.0-SNAPSHOT.jar
```

看到 `Started QuizApplication` 即代表启动成功。

### 5. 打开浏览器

访问：<http://localhost:8080/>

首次进入先注册一个账号 → 登录 → 上传 `.docx` 讲义或粘贴文本 → 设定题目数量（1~50）→ 开始刷题。

---

## 🔌 API 一览

后端为纯 REST 接口，前端 `index.html` 是唯一使用者；你也可以用 Postman / curl 调试。

### 用户

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| POST | `/user/register` | `username`, `password` | 注册 |
| POST | `/user/login`    | `username`, `password` | 登录，成功返回 `success:{userId}` |

### 题库

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| POST | `/quiz/generate-from-document` | `file` (multipart), `chatId`, `numQuestions` | 上传 Word 生成 N 道题 |
| POST | `/quiz/generate-from-text` | body `{text, sourceDoc}`, query `chatId`, `numQuestions` | 由文本生成 N 道题 |
| GET | `/quiz/list` | `chatId` | 列出该 chatId 的所有题目 |
| POST | `/quiz/submit-answer` | body `{userId, questionId, selectedAnswer}` | 提交答案，返回是否正确 + 解析 |
| POST | `/quiz/toggle-favorite` | body `{userId, questionId}` | 收藏 / 取消收藏 |
| GET | `/quiz/favorites` | `userId` | 收藏列表 |
| GET | `/quiz/wrong-answers` | `userId` | 错题列表 |
| DELETE | `/quiz/remove-wrong-answer` | `userId`, `questionId` | 移除某条错题 |
| DELETE | `/quiz/delete-question` | `userId`, `questionId` | 删除单题（连带进度、错题） |
| POST | `/quiz/delete-document` | body `{userId, documentName}` | 删除该文档下的所有题目 |
| GET | `/quiz/statistics` | `userId` | 学习统计 |
| GET | `/quiz/knowledge-map` | `userId` | 按源文档聚类的知识地图 |

响应体统一为 JSON，包含 `success`、`message`，业务数据放在同级字段（如 `questions`、`statistics`）。

---

## 🧪 生成题目的关键逻辑（FAQ 前置）

题目生成位于 [`RagService`](src/main/java/org/example/quizapp/service/RagService.java)：

1. 按 `numQuestions` 构造严格数量约束的 prompt 送给大模型；
2. 剥离模型可能带上的 ```` ```json ```` Markdown 代码围栏，截取 JSON 数组片段；
3. Jackson 反序列化 → 逐题校验题干 / 选项数 / 正确下标；
4. 若首次返回的有效题目 < 目标数量，会自动重试（最多 3 次）并把已生成题干作为"禁止重复"清单一起下发；
5. 按题干去重后，若仍超出目标数量则截断，最终严格返回 ≤ `numQuestions` 条。

因此只要文本足够、模型可用，实际生成数量与请求数量高度一致；如果文本很短，后端返回的 `message` 会明确提示 `成功生成 X/Y 道题目（可用知识点不足）`。

---

## 🛠 常见问题

**Q1. 启动报 `Non-resolvable parent POM` 或找不到 `spring-ai-bom`？**
A. Spring AI 里程碑版本只发布在 Spring Milestones 仓库，不在 Maven Central。请确认 `pom.xml` 中的 `<repositories>` 存在 `https://repo.spring.io/milestone`；若使用了阿里云镜像，需要在 `settings.xml` 中给这个仓库放行（不要把 `mirrorOf` 设为 `*`）。

**Q2. 生成失败，日志出现 `401 Unauthorized` / `invalid api key` / `sk-请填入你的-API-Key`？**
A. 你还没提供真实 Key。按上面 “3. 配置 API Key” 中的方式 A 或方式 B 填上即可（推荐方式 A，拷一份 `application-local.properties`）。

**Q3. 想用别的大模型（OpenAI / DeepSeek 官方 / 通义 / Moonshot 等）？**
A. 只需要改 3 行：`base-url`、`api-key`、`chat.options.model`，其他代码不用动。任何兼容 OpenAI `/v1/chat/completions` 协议的服务都可以。

**Q4. 上传 `.doc`（老版 Word）报错？**
A. 当前用的是 `XWPFDocument`，仅支持 `.docx`。前端会拦截，若需要 `.doc` 请自行加上 `poi-scratchpad` 依赖并调用 `HWPFDocument`。

**Q5. MongoDB 有权限 / 端口不是 27017？**
A. 修改 `application.properties` 中 `spring.data.mongodb.*`；若需要账号密码，可改为 `spring.data.mongodb.uri=mongodb://user:pass@host:port/db`。

**Q6. 生成的题目前端显示不对？**
A. 打开浏览器 DevTools 看 `/quiz/generate-*` 响应，`questions` 字段即为入库后回读的数据；若为空，多半是模型返回不是标准 JSON，此时可在后端日志里看 `RagService` 的输出以定位。

---

## 🔒 已知安全 & 局限（README 里说清楚）

本项目定位是**个人 / 课程作业级别的学习工具**，出于教学清晰度刻意保持简单，因此：

- 用户密码目前 **明文存储**，登录 **无 Session / JWT**：仅适合本地或内网 demo，公网部署请自行加上 `spring-boot-starter-security`；
- 出题接口没有频率限制，会真实消耗 API 额度；
- API Key 一律使用环境变量或 `application-local.properties`（已 gitignore）提供，切勿提交到仓库。历史 commit 里如果曾经过不小心推过真 Key，请去供应商后台立刻 **作废并重生**。

---

## 📄 License

MIT — 随意使用、修改、分享。若觉得有用，欢迎给个 ⭐。
