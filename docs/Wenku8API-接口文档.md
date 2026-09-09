# Wenku8 轻小说文库 — 完整接口文档（新版 / api-stub 契约 + 应用侧证据）

> **整理日期**：2026-09-08
> **针对版本**：`studio-android`（新版）下的 `LightNovelLibrary`
> **接口契约来源**：`api-stub/src/main/java/org/mewx/wenku8/api/Wenku8API.java`（**新版接口签名全集**）
> **请求协议（wire）来源**：仓库内旧版公开实现 `eclipse-android-old/.../Wenku8Interface.java`（含 `action=…&do=…` 协议与 Base64 加密）、`STABILITY_PLAN.md`（印证新端点 `action=bookcase`、`action=bookcase&do=list`）
> **响应格式来源**：新版 `app/src/main/java/org/mewx/wenku8/global/api/*.java`（全部响应解析器）+ `app/src/main/java/org/mewx/wenku8/activity|fragment/*`（调用与应答处理）

> ⚠️ **重要说明**：新版真实的 `api/` 是**私有 GitHub 子模块** `git@github.com:MewX/wenku8-api-core.git`，本仓库未检出（目录为空），CI 因此默认用 `api-stub` 编译。因此：
> - 本文档中**接口签名（方法名/参数/返回）100% 来自新版** `Wenku8API.java`；
> - **请求 wire 格式**取自旧版公开实现 + `STABILITY_PLAN.md` 对实际端点的验证（协议家族一致，`action=bookcase` 等端点名已在新版计划文档中被逐字印证），个别参数名若真实子模块有调整需以子模块为准；
> - **响应格式**全部来自新版 app 的实际解析器，可靠性高。
>
> 想要逐字节确认 wire 格式，检出子模块即可：`git submodule update --init studio-android/LightNovelLibrary/api`（需要 GitHub 私钥权限）。

---

## 目录

1. [传输协议总览](#1-传输协议总览)
2. [枚举定义](#2-枚举定义)
3. [常量](#3-常量)
4. [接口总表](#4-接口总表)
5. [接口详情](#5-接口详情)
   - 5.1 封面/头像 URL 构建
   - 5.2 连载状态与排序键转换
   - 5.3 小说信息
   - 5.4 搜索
   - 5.5 小说列表 / 文库
   - 5.6 用户（登录/信息/头像/签到/注销）
   - 5.7 投票
   - 5.8 书架
   - 5.9 敏感词
   - 5.10 评论
6. [错误码表（Wenku8Error）](#6-错误码表wenku8error)
7. [HTTP 层（LightNetwork）](#7-http-层lightnetwork)
8. [获取真实 api 子模块与验证清单](#8-获取真实-api-子模块与验证清单)

---

## 1. 传输协议总览

所有业务请求统一走 **HTTP POST**：

```
POST {BASE_URL}
Content-Type: application/x-www-form-urlencoded
Body: request = Base64( UTF-8( 协议字符串 ) )
```

- **BASE_URL（已实测确认 2026-09-08）**：`http://app.wenku8.cn/android.php`（**只走 HTTP 明文，HTTPS 返回空**）。⚠️ 旧地址 `app.wenku8.com/android.php` 已废弃，现在只回 42 字节的 `Welcome` 占位页；`www.wenku8.com` 已 301 到 `www.wenku8.net`。
- **协议字符串（已实测确认）**：`action=xxx&do=xxx&…` 形式的查询串，整体 Base64（UTF-8）放进 `request` 字段 POST 提交。实测 `request=Base64("action=book&do=info&aid=1305&t=0")` 返回与本文档完全一致的 XML。
- **响应** 有两类：
  - **二进制/文本**（正文、简介、目录、图片等）；
  - **整数状态码**（登录、签到、投票、书架增删、发帖等）——整数即服务器错误码，用 `Wenku8Error.getSystemDefinedErrorCode(int)` 映射为 `ErrorCode`（见第 6 节）；`action=book&do=vote` 例外，返回含 `PushCount` 的 XML。
- **语言参数**：请求中的 `t=` 取值 `SC=0`、`TC=1`（旧版 `getLANG()` 映射，新版 `AppLanguage` 同义）。
- **会话机制（实测确认 2026-09-08）**：登录后服务器下发 **PHP Session cookie `PHPSESSID`**，后续请求带此 cookie 即视为已登录。账号型接口（`bookcase*`、`userinfo`、`logout`、`book&do=vote`、`review*`）依赖它；`search`、`book&do=info/meta/list/text` 匿名可用。
- **HTTP 失败**：请求层 `LightHttpPostConnection` 失败返回 `null`（见第 7 节），调用方将其映射为 `NETWORK_ERROR` 等错误码。

### 1.1 已实测确认的端点清单（2026-09-08，用测试账号验证）

| 端点（action=…&do=…） | 认证要求 | 实测结果 |
|---|---|---|
| `login&username={u}&password={url编码密码}` | 匿名 | 返回整数：`1` 成功 / `2` 用户名错 / `3` 密码错 |
| `logout&t=0` | 会话 | 返回 `1` |
| `userinfo&t=0` | 会话 | XML 用户信息 |
| `bookcase&do=list&t=0` | 会话 | XML 书架列表（空书架返回 `<metadata></metadata>`） |
| `bookcase&do=add&aid={a}&t=0` | 会话 | 整数：`1` 成功 / `5` 已在书架 / `6` 书架满 |
| `bookcase&do=del&aid={a}&t=0` | 会话 | 整数：`1` 成功 / `4` 未登录 / `7` 不在书架 |
| `book&do=vote&aid={a}&t=0` | 会话 | XML：`<data name="PushCount" value="..."/>`（推荐后更新计数，日限约 5 次） |
| `review&do=list&aid={a}&page={p}&t=0` | 匿名 | XML 评论列表（根节点 `<metadata>`） |
| `search&searchtype=…&searchkey=…&t=0` | 匿名 | XML 富格式结果（含内嵌 info） |
| `book&do=info/meta/list/text` | 匿名 | XML（与文档示例一致） |
| `novellist&sort=…&page=…&t=0` | **会话+？** | ⚠️ **即便已登录也返回 `4`**（见 5.5 警示） |
| `articlelist&sort=…&page=…` | **会话+？** | ⚠️ 同 `novellist`，已登录也返回 `4` |

> 待确认项（私有 api-core 源码可一次性补齐）：列表端点新形态、`sign`（签到）action、头像 URL 规则、`review&do=post/reply`、邮箱登录。

---

## 2. 枚举定义

### 2.1 `Wenku8API.AppLanguage` — 语言

| 常量 | 含义 | 请求 `t=` 值 |
|---|---|---|
| `SC` | 简体中文 | `0` |
| `TC` | 繁体中文 | `1` |

### 2.2 `Wenku8API.NovelPublicationStatus` — 连载状态

| 常量 | 含义 |
|---|---|
| `FINISHED` | 已完结 |
| `NOT_FINISHED` | 连载中/未完结 |

转换接口：`getNovelPublicationStatusByInt(int)`、`getNovelPublicationStatusByString(String)`、`getStatusByNovelPublicationStatus(NovelPublicationStatus)`。

### 2.3 `Wenku8API.NovelSortedBy` — 排序键

**枚举常量名 == 服务端排序参数原值**（新版用 `valueOf` 直接双向映射，STABILITY_PLAN:1437 印证「real module maps `NovelSortedBy.allVote` to `"allvote"` and back」）。

| 常量 | 请求 `sort=` 值 | 含义 |
|---|---|---|
| `allVisit` | `allvisit` | 总访问榜 |
| `allVote` | `allvote` | 总推荐榜 |
| `monthVisit` | `monthvisit` | 月访问榜 |
| `monthVote` | `monthvote` | 月推荐榜 |
| `weekVisit` | `weekvisit` | 周访问榜 |
| `weekVote` | `weekvote` | 周推荐榜 |
| `dayVisit` | `dayvisit` | 日访问榜 |
| `dayVote` | `dayvote` | 日推荐榜 |
| `postDate` | `postdate` | 最新入库 |
| `lastUpdate` | `lastupdate` | 最近更新 |
| `goodNum` | `goodnum` | 总收藏榜 |
| `size` | `size` | 字数排行 |
| `fullFlag` | `fullflag` | 完结列表 |

---

## 3. 常量

| 常量 | 类型 | 说明 |
|---|---|---|
| `UNKNOWN` | `String` | 通用未知占位值 `"Unknown"` |
| `CurrentLang` | `AppLanguage` | 当前语言，默认 `SC`；`GlobalConfig.setCurrentLang` 会同步写回此字段 |
| `AppVer` | `String` | 应用版本号，默认 `UNKNOWN` |
| `NoticeString` | `String` | 公告字符串，默认 `UNKNOWN` |
| `REGISTER_URL` | `String` | 网页注册地址（登录页"注册"按钮跳转浏览器用），桩中 `UNKNOWN` |
| `BASE_URL` | `String` | 接口基础地址（见第 1 节），桩中 `UNKNOWN` |
| `MIN_REPLY_TEXT` | `int` | 回复/评论最小长度约束，`-1`（服务端规则，客户端无固定值） |

---

## 4. 接口总表

> 桩行为标记：
> - **【空 CV】** = 桩返回空 `ContentValues`（被调用路径要求先到离线空态，不抛异常）；
> - **【空串】** = 桩返回 `""`（封面 URL，空则不加载图，即真实状态）；
> - **【抛】** = 桩抛 `UnsupportedOperationException`（无诚实失败值，主动响亮失败）。

| # | 接口（签名） | 桩行为 | 调用方 |
|---|---|---|---|
| 1 | `String getCoverURL(int aid)` | 【空串】 | NovelInfoActivity（高清晰封面） |
| 2 | `String getAvatarURL(int uid)` | 【抛】 | UserInfoActivity / UserLoginActivity |
| 3 | `NovelPublicationStatus getNovelPublicationStatusByInt(int i)` | 【抛】 | 状态转换工具 |
| 4 | `NovelPublicationStatus getNovelPublicationStatusByString(String s)` | 【抛】 | 状态转换工具 |
| 5 | `String getStatusByNovelPublicationStatus(NovelPublicationStatus s)` | 【抛】 | 状态转换工具 |
| 6 | `NovelSortedBy getNovelSortedBy(String n)` | `valueOf` | RKListFragment |
| 7 | `String getNovelSortedBy(NovelSortedBy n)` | `n.name()` | RKListFragment / NovelItemListFragment |
| 8 | `ContentValues getNovelCover(int aid)` | 【抛】 | NovelInfoActivity（保存高清封面） |
| 9 | `ContentValues getNovelShortInfo(int aid, AppLanguage l)` | 【抛】 | （新 app 已改用 #10） |
| 10 | `ContentValues getNovelShortInfoUpdate_CV(int aid, AppLanguage l)` | 【抛】 | NovelItemAdapterUpdate.AsyncLoadNovelIntro |
| 11 | `ContentValues getNovelFullIntro(int aid, AppLanguage l)` | 【抛】 | NovelInfoActivity / FavFragment |
| 12 | `ContentValues getNovelFullMeta(int aid, AppLanguage l)` | 【抛】 | NovelInfoActivity / FavFragment |
| 13 | `ContentValues getNovelIndex(int aid, AppLanguage l)` | 【抛】 | NovelInfoActivity / FavFragment |
| 14 | `ContentValues getNovelContent(int aid, int cid, AppLanguage l)` | 【空 CV】 | VerticalReaderActivity / Wenku8ReaderActivityV1 / NovelInfoActivity |
| 15 | `ContentValues searchNovelByNovelName(String name, AppLanguage l)` | 【抛】 | NovelItemListFragment |
| 16 | `ContentValues searchNovelByAuthorName(String name, AppLanguage l)` | 【抛】 | NovelItemListFragment |
| 17 | `ContentValues getNovelList(NovelSortedBy n, int page)` | 【空 CV】 | 通用列表 |
| 18 | `ContentValues getMewxNovelList(NovelSortedBy n, int page, AppLanguage l)` | 【空 CV】 | 无调用方 |
| 19 | `ContentValues getNovelListWithInfo(NovelSortedBy n, int page, AppLanguage l)` | 【空 CV】 | NovelItemListFragment / LatestFragment（启动页） |
| 20 | `ContentValues getLibraryList()` | 【抛】 | 文库分类 |
| 21 | `ContentValues getNovelListByLibrary(int sortId, int page)` | 【抛】 | 按文库取列表 |
| 22 | `ContentValues getNovelListByLibraryWithInfo(int sortId, int page, AppLanguage l)` | 【抛】 | 按文库取列表+信息 |
| 23 | `ContentValues getUserLoginParams(String username, String password)` | 【抛】 | LightUserSession.doLoginFromGiven |
| 24 | `ContentValues getUserLoginEmailParams(String email, String password)` | 【抛】 | LightUserSession（邮箱登录） |
| 25 | `ContentValues getUserAvatar()` | 【抛】 | UserLoginActivity（登录后拉头像） |
| 26 | `ContentValues getUserLogoutParams()` | 【抛】 | UserInfoActivity.AsyncLogout |
| 27 | `ContentValues getUserInfoParams()` | 【空 CV】 | AccountInfoLoader → UserInfoActivity |
| 28 | `ContentValues getUserSignParams()` | 【抛】 | AccountInfoLoader → UserInfoActivity（签到） |
| 29 | `ContentValues getVoteNovelParams(int aid)` | 【抛】 | **无调用方**（旧版注释：`action=book&do=vote&aid=`，日限5次/需登录） |
| 30 | `ContentValues getBookshelfListAid(AppLanguage l)` | 【抛】 | FavFragment.fetchShelfAidsFromIdsOnlyEndpoint |
| 31 | `ContentValues getBookshelfListParams(AppLanguage l)` | 【抛】 | FavFragment.fetchShelfListing（**STABILITY_PLAN: 无调用方，为优化预留**） |
| 32 | `ContentValues getAddToBookshelfParams(int aid)` | 【抛】 | FavFragment（同步时推送本地→云端） |
| 33 | `ContentValues getDelFromBookshelfParams(int aid)` | 【抛】 | NovelInfoActivity / FavFragment（移除） |
| 34 | `@Nullable String searchBadWords(String source)` | 【抛】 | NovelReviewNewPostActivity / NovelReviewReplyListActivity |
| 35 | `ContentValues getCommentListParams(int aid, int page)` | 【抛】 | NovelReviewListActivity |
| 36 | `ContentValues getCommentContentParams(int rid, int page)` | 【抛】 | NovelReviewReplyListActivity |
| 37 | `ContentValues getCommentNewThreadParams(int aid, String title, String content)` | 【抛】 | NovelReviewNewPostActivity |
| 38 | `ContentValues getCommentReplyParams(int rid, String content)` | 【抛】 | NovelReviewReplyListActivity |

---

## 5. 接口详情

> 说明：请求列给出的是 **`action=…&do=…` 协议字符串**（旧版实现原文 + 新版计划文档印证），再按第 1 节 Base64 打包。响应列给出实际返回体与新版解析器。

### 5.1 封面 / 头像 URL 构建

#### ① `String getCoverURL(int aid)`
- **入参**：`aid` 小说 ID。
- **返回**：完整封面大图 URL（`String`）。桩返回 `""`（空则不加载，即离线真实态）。
- **用途**：NovelInfoActivity 在 `onCreateView` 直接交给 ImageLoader 加载；也用于详情页"查看高清封面"。
- **响应**：经由 `LightHttpDownload(URL)` / ImageLoader 获取 **JPEG 二进制**（保存为 `full_cover_{aid}.jpg`）。

#### ② `String getAvatarURL(int uid)`
- **入参**：`uid` 用户 ID。
- **返回**：用户头像 URL。
- **用途**：UserInfoActivity / UserLoginActivity → `LightHttpDownload` 取 **JPEG 二进制**，写缓存文件。

### 5.2 连载状态与排序键转换

#### ③④⑤ 连载状态
- `NovelPublicationStatus getNovelPublicationStatusByInt(int i)` — 服务器整型值 → 状态。
- `NovelPublicationStatus getNovelPublicationStatusByString(String s)` — 字符串 → 状态。
- `String getStatusByNovelPublicationStatus(NovelPublicationStatus s)` — 状态 → 展示字符串。
- 相关响应字段：完整元数据中的 `<data name="BookStatus" value="0"/>`（旧版示例中 `0`；完整元数据里则直接是文本如"已完成"）。

#### ⑥⑦ 排序键
- `NovelSortedBy getNovelSortedBy(String n)` — 服务端 key → 枚举；未识别 key 抛异常（响亮失败）。
- `String getNovelSortedBy(NovelSortedBy n)` — 枚举 → 服务端 key（即 `n.name()`）。
- 用途：RKListFragment 按 12 个排序 Tab 构建 `bundle.putString("type", getNovelSortedBy(...))`。

### 5.3 小说信息

#### ⑧ `getNovelCover(int aid)`
- **请求**：`action=book&do=cover&aid={aid}`
- **响应**：封面图 **JPEG 二进制**（`LightHttpPostConnection` 返回 bytes），保存为 `full_cover_{aid}.jpg` 后可查看大图。

#### ⑨ `getNovelShortInfo(int aid, AppLanguage l)`
- **请求**：`action=book&do=info&aid={aid}&t={0|1}`
- **响应**：XML 短信息
```xml
<?xml version="1.0" encoding="utf-8"?>
<metadata>
  <data name="Title" aid="1305"><![CDATA[绝对双刃absolute duo]]></data>
  <data name="Author" value="柊★巧"/>
  <data name="BookStatus" value="0"/>
  <data name="LastUpdate" value="2014-10-01"/>
  <data name="IntroPreview"><![CDATA[　　「焰牙」——那是藉由超化之后的精神力将自身灵...]]></data>
</metadata>
```
- **解析器**：`NovelItemInfoUpdate.parse()`（列表行渲染：title/author/status/update/intro_short）。

#### ⑩ `getNovelShortInfoUpdate_CV(int aid, AppLanguage l)` ★新版新增
- **请求**：与 ⑨ 同端点（`action=book&do=info&…`），语义为"行内增量刷新"。
- **响应**：同 ⑨ 的短信息 XML。
- **用途**：`NovelItemAdapterUpdate.AsyncLoadNovelIntro` 在列表行绑定时按需拉取，回填 title/author/status/update/intro_short。
- **注意**：若服务器应答成功，即使字段仍缺失也不再重试（注释：失败才允许下次重试）。

#### ⑪ `getNovelFullIntro(int aid, AppLanguage l)`
- **请求**：`action=book&do=intro&aid={aid}&t={0|1}`
- **响应**：完整简介（文本/CDATA，可能直接为纯文本段落）。NovelInfoActivity 与 FavFragment（书籍下载）使用。

#### ⑫ `getNovelFullMeta(int aid, AppLanguage l)`
- **请求**：`action=book&do=meta&aid={aid}&t={0|1}`
- **响应**：XML 完整元数据
```xml
<?xml version="1.0" encoding="utf-8"?>
<metadata>
  <data name="Title" aid="1306"><![CDATA[向森之魔物献上花束(向森林的魔兽少女献花)]]></data>
  <data name="Author" value="小木君人"/>
  <data name="DayHitsCount" value="26"/>
  <data name="TotalHitsCount" value="43984"/>
  <data name="PushCount" value="1735"/>
  <data name="FavCount" value="848"/>
  <data name="PressId" value="小学馆" sid="10"/>
  <data name="BookStatus" value="已完成"/>
  <data name="BookLength" value="105985"/>
  <data name="LastUpdate" value="2012-11-02"/>
  <data name="LatestSection" cid="41897"><![CDATA[第一卷 插图]]></data>
</metadata>
```
- **解析器**：`Wenku8Parser.parseNovelFullMeta()` → `NovelItemMeta{aid,title,author,dayHitsCount,totalHitsCount,pushCount,favCount,pressId,bookStatus,bookLength,lastUpdate,latestSectionCid,latestSectionName}`。**无 Title 的合法 XML 返回 null**（防"书名为1"的脏数据）。
- **用途**：详情页 + FavFragment 书籍下载（与 index、intro 并行拉取）。

#### ⑬ `getNovelIndex(int aid, AppLanguage l)`（目录）
- **请求**：`action=book&do=list&aid={aid}&t={0|1}`
- **响应**：XML 卷/章目录
```xml
<?xml version="1.0" encoding="utf-8"?>
<package>
  <volume vid="41748"><![CDATA[第一卷 告白于苍刻之夜]]>
    <chapter cid="41749"><![CDATA[序章]]></chapter>
    <chapter cid="41750"><![CDATA[第一章「去对我的『楯』说吧——」]]></chapter>
    ...
  </volume>
  ...
</package>
```
- **解析器**：`Wenku8Parser.getVolumeList()` → `List<VolumeList{volumeName,vid,chapterList:[ChapterInfo{cid,chapterName}]}>`（含对服务器 CDATA 换行格式错误的兼容处理）。

#### ⑭ `getNovelContent(int aid, int cid, AppLanguage l)`（章节正文）
- **请求**：`action=book&do=text&aid={aid}&cid={cid}&t={0|1}`
- **响应**：纯文本正文，每行一个段落/插图；插图行格式：
```
第一卷 告白于苍刻之夜 插图
......
<!--image-->http://pic.wenku8.cn/1/1305/41759/50471.jpg<!--image-->
<!--image-->http://pic.wenku8.cn/1/1305/41759/50472.jpg<!--image-->
```
- ⚠️ **图片 URL 格式（实测 2026-09-08）**：当前为 `http://pic.wenku8.cn/1/{aid}/{cid}/{seq}.jpg`（**无 `/pictures/` 前缀**，旧格式 `pic.wenku8.cn/pictures/1/…` 及 `pic.wenku8.com` 均无效）。
- ⚠️ **图片下载受限（实测）**：`pic.wenku8.cn` 在 **Cloudflare** 后面，从数据中心 IP 用 curl 请求**一律被 reset（HTTP 000）**，与 UA/Referer/登录 cookie 无关。真机（住宅 IP + OkHttp/WebView）能否加载**待真机验证**——这是重写中"插图"环节的唯一风险点；纯文本阅读不受影响。
- **解析器**：`OldNovelContentParser.parseNovelContent()` → `List<NovelContent{type:TEXT|IMAGE, content}>`（插图行可一行多图）。
- **读取器**：VerticalReaderActivity / Wenku8ReaderActivityV1 在 `onCreate` 即发起；因此桩对该方法返回空 CV 而非抛异常（避免无网络时 Activity 崩溃）。
- **图片下载**：正文插图 URL 再经 `LightHttpDownload` 单独拉取（NovelInfoActivity 下载模式）。

### 5.4 搜索

#### ⑮ `searchNovelByNovelName(String novelName, AppLanguage l)`
- **请求**：`action=search&searchtype=articlename&searchkey={URL编码的书名}&t={0|1}`
- **响应（实测确认 2026-09-08）**：XML 命中的列表。⚠️ **当前服务器返回的是带内嵌信息的富格式**（每个 item 内含 Title/Author/TotalHitsCount/PushCount/FavCount 等 data 节点，同 `novellist` 风格），而非仅 aid：
```xml
<?xml version="1.0" encoding="utf-8"?>
<result>
<item aid='3500'>
<data name='Title'><![CDATA[种马之剑 ...]]></data>
<data name='TotalHitsCount' value='104493'/>
<data name='PushCount' value='525'/>
<data name='FavCount' value='2186'/>
<data name="Author" value='...'/>
...
</item>
</result>
```
- 应用现用正则 `aid=\'(.*)\'` 只取 aid 再逐条补信息；**重构建议直接用 `NovelListWithInfoParser` 解析富格式**，省掉二次请求。

#### ⑯ `searchNovelByAuthorName(String authorName, AppLanguage l)`
- **请求**：`action=search&searchtype=author&searchkey={URL编码的作者名}&t={0|1}`
- **响应**：同上 `<result><item aid='…'/></result>`。

**搜索流程**（NovelItemListFragment）：先按书名查、再按作者查，两次结果用正则 `aid=\'(.*)\'` 提取 aid，**去重合并**（作者命中排前）。随后用 aid 调 ⑨/⑩ 拉短信息渲染。

### 5.5 小说列表 / 文库

> 🎯 **实测最终结论（2026-09-08）**：**Android API 的 `novellist` / `articlelist` 两个 action 在当前服务器上已废弃**——无论匿名、API 登录会话还是网页级登录会话（JIEQI cookie），一律返回 `4`。**当前唯一可用的列表来源是网页版 `articlelist.php`**（见下方"5.5A 网页列表接口"）。

#### 5.5A 网页列表接口（新版列表的事实来源）

| 项 | 内容 |
|---|---|
| URL | `https://www.wenku8.net/modules/article/articlelist.php` |
| 认证 | **必须登录**（JIEQI CMS 会话：`jieqiUserInfo` + `PHPSESSID` cookie；匿名访问 302 → `login.php?jumpurl=…`） |
| 编码 | **GBK**（HTML 页面） |
| 排序参数 | `sort=allvisit / allvote / monthvisit / monthvote / weekvisit / weekvote / dayvisit / dayvote / postdate / lastupdate / goodnum / size` —— **与 `NovelSortedBy` 枚举一一对应** |
| 分类参数 | `class={sortid}`（对应 `getLibraryList()` 的 1~14 文库分类） |
| 完结过滤 | `fullflag=1` |
| 字母索引 | `initial=A…Z / 1` |
| 分页 | `page={N}`（如 allvisit 共 215 页；每页约 20 条） |
| 繁简 | `charset=big5` 变体 |
| 条目 HTML | `<a href="/book/{aid}.htm" title="{书名}">` —— **仅提供 aid + 书名**，列表行其余字段需再用 API `book&do=info` 按 aid 补（该接口匿名可用） |

**登录方式（二选一，均为 JIEQI 会话）**：
- Android API：`action=login&username={u}&password={url编码}` → `1` 成功，下发 `PHPSESSID`（但**不足以**解锁网页列表，见下）；
- 网页登录：`POST https://www.wenku8.net/login.php?do=submit`，字段 `username`/`password`/`action=login`，成功下发 **`jieqiUserInfo` + `PHPSESSID`**（**此会话可解锁网页列表**）。

> ⚠️ 实测：API 登录下发的 `PHPSESSID` 只能过 Android 端账号接口（`bookcase*`/`userinfo`/`logout`/`vote`），**过不了网页列表**；网页登录下发的完整 JIEQI 会话（含 `jieqiUserInfo`）才能访问 `articlelist.php`。重写时若走网页列表，需在 App 内实现**网页登录**或复用网页会话。

### 5.5B Android API 列表端点（已废弃，仅存档）

#### ⑰ `getNovelList(NovelSortedBy n, int page)`（仅 aid）
- **请求**：`action=articlelist&sort={sort}&page={page}`（**当前返回 `4`，废弃**）
- **响应**：XML
```xml
<?xml version="1.0" encoding="utf-8"?>
<result>
  <page num='166'/>
  <item aid='1143'/>
  <item aid='1034'/>
  ...
</result>
```
- 返回的 aid 需再逐条拉 ⑨/⑩ 补信息。新版启动页已改走 ⑲。

#### ⑱ `getMewxNovelList(NovelSortedBy n, int page, AppLanguage l)`
- **请求**：`action=novellist&sort={sort}&page={page}&t={0|1}`（**当前返回 `4`，废弃**；与 ⑲ 同端点）
- **响应**：同 ⑲。
- **无调用方**（预留/历史接口）。

#### ⑲ `getNovelListWithInfo(NovelSortedBy n, int page, AppLanguage l)` ★旧版主用
- **请求**：`action=novellist&sort={sort}&page={page}&t={0|1}`（**当前返回 `4`，废弃**）
- **响应**：XML，每项自带列表行所需信息（避免逐条二次请求）：
```xml
<?xml version="1.0" encoding="utf-8"?>
<result>
  <page num='166'/>
  <item aid='1143'>
    <data name='Title'><![CDATA[约会大作战(DATE A LIVE)]]></data>
    <data name='TotalHitsCount' value='2200395'/>
    <data name='PushCount' value='164396'/>
    <data name='FavCount' value='15114'/>
    <data name='Author' value='作者名'/>
    <data name='BookStatus' value='...'/>
    <data name='LastUpdate' value='...'/>
    <data name='Tags' value='...'/>
    <data name='IntroPreview'><![CDATA[...]]></data>
  </item>
  ...
</result>
```
- **解析器**：`NovelListWithInfoParser.parse()` → `Result{pageNum, List<NovelItemInfoUpdate{aid,title,author,status,update,intro_short,tags}>}`。字段可 `value=` 属性或 CDATA 文本两种形态，两者兼容（`readValue` 优先取属性、回退取文本）。
- **缓存**：每页解析结果会写入 `NovelItemInfoUpdate` 全局 LruCache(500)，供排行页/最新页/搜索/书架共用。

#### ⑳ `getLibraryList()`（文库分类）
- **请求**：`action=xml&item=sort&t=0`（**当前状态待复测**）
- **响应**：XML
```xml
<?xml version="1.0" encoding="utf-8"?>
<metadata>
  <item sort="1">电击文库</item>
  <item sort="2">富士见文库</item>
  <item sort="3">角川文库</item>
  <item sort="4">MF文库J</item>
  <item sort="5">Fami通文库</item>
  <item sort="6">GA文库</item>
  <item sort="7">HJ文库</item>
  <item sort="8">一迅社</item>
  <item sort="9">集英社</item>
  <item sort="10">小学馆</item>
  <item sort="11">讲谈社</item>
  <item sort="12">少女文库</item>
  <item sort="13">其他文库</item>
  <item sort="14">游戏剧本</item>
</metadata>
```

#### ㉑ `getNovelListByLibrary(int sortId, int page)`
- **请求**：`action=articlelist&sort={sortId}&page={page}`（sortId 取自 ⑳；**当前返回 `4`，废弃**）
- **响应**：同 ⑰（`<result><page/><item aid/>`）。

#### ㉒ `getNovelListByLibraryWithInfo(int sortId, int page, AppLanguage l)`
- **请求**：`action=novellist&sort={sortId}&page={page}&t={0|1}`（**当前返回 `4`，废弃**）
- **响应**：同 ⑲（每项带信息）。

### 5.6 用户（登录 / 信息 / 头像 / 签到 / 注销）

> 登录会话封装在 `LightUserSession`（新版网络层）。凭证文件做加解密（`encUserFile/decAndSetUserFile`）。

#### ㉓ `getUserLoginParams(String username, String password)` — 用户名登录
- **请求（实测确认）**：`action=login&username={用户名}&password={URL编码后的密码}`（密码中的特殊字符如 `+` 必须先 URL 编码成 `%2B`，再整体 Base64 进 `request`）。
- **调用链**：`LightUserSession.doLoginFromGiven(username, password, saveCallback)` → `LightHttpPostConnection(BASE_URL, getUserLoginParams(...))`。
- **响应**：**整数状态码字符串**，映射为：
  - `1` = `SYSTEM_1_SUCCEEDED`（成功）
  - `2` = `SYSTEM_2_ERROR_USERNAME`（用户名错误）
  - `3` = `SYSTEM_3_ERROR_PASSWORD`（密码错误）
- 登录成功后服务器下发 `PHPSESSID` cookie（需客户端 cookie 持久化并随后续请求回传）；登录成功后再调 ㉕ 拉头像。

#### ㉔ `getUserLoginEmailParams(String email, String password)` — 邮箱登录
- 语义同 ㉓，账号字段为邮箱。返回整数状态码（同上）。

#### ㉕ `getUserAvatar()` — 获取当前用户头像
- **响应**：头像 **JPEG 二进制**（`LightHttpPostConnection` 返回 bytes）。登录后调用，写缓存文件（两套路径互备）。失败（null）则用本地默认头像兜底。

#### ㉖ `getUserLogoutParams()` — 注销
- **请求（实测确认）**：`action=logout&t=0`
- **响应**：**整数状态码字符串**，期望 `1`（成功）或 `4`（`SYSTEM_4_NOT_LOGGED_IN`）。二者皆视为已登出，随即删除本地会话/头像缓存文件。

#### ㉗ `getUserInfoParams()` — 用户信息
- **请求（实测确认）**：`action=userinfo&t=0`（需会话 cookie）
- **响应**（两种形态，按内容区分）：
  - **XML**（正常）：
```xml
<?xml version="1.0" encoding="utf-8"?>
<metadata>
  <item name="uname"><![CDATA[apptest]]></item>
  <item name="nickname"><![CDATA[apptest]]></item>
  <item name="uid">123</item>
  <item name="score">10</item>
  <item name="experience">10</item>
  <item name="rank"><![CDATA[新手上路]]></item>
</metadata>
```
  - **整数**（会话失效时返回状态码，如 `4` = `SYSTEM_4_NOT_LOGGED_IN`）。
- **解析器**：`UserInfo.parseUserInfo()` → `UserInfo{username,nickyname,uid,score,experience,rank}`；无 `uname` 的合法 XML 返回 null（防"空名已登录用户"）。
- **会话失效重试**：`AccountInfoLoader.load()` 发现整数响应为「未登录」时，自动用 `LightUserSession.doLoginFromFile` 重登录一次并重试；其他状态码直接返回失败。

#### ㉘ `getUserSignParams()` — 每日签到
- **响应**：**整数状态码字符串**。
  - `9` = `SYSTEM_9_SIGN_FAILED`（签到失败，界面报"签到失败"）；其他整数不阻断后续流程（如重复签到不视为失败）。
  - 成功通常为 `1`。非整数响应按 `STRING_CONVERSION_ERROR` 处理。
- 流程（UserInfoActivity 顶部菜单"签到"）：先签到 → 再拉 ㉗ 用户信息 → ㉡ 头像。

### 5.7 投票

#### ㉙ `getVoteNovelParams(int aid)`
- **请求（实测确认）**：`action=book&do=vote&aid={aid}&t=0`（旧版注释"每天限 5 次、需登录"；实测返回 XML，非整数码）。
- **响应（实测）**：
```xml
<?xml version="1.0" encoding="utf-8"?>
<metadata>
<data name="PushCount" value="5098"/>
</metadata>
```
- 即返回推荐后更新的小说推荐数。**当前无调用方**（新版 app 未接该功能，接口已定义）。

### 5.8 书架

#### ㉚ `getBookshelfListAid(AppLanguage l)` — 仅 aid 列表
- **请求**：`action=bookcase&do=list&t=0`（需会话；STABILITY_PLAN 逐字印证）。
- **响应（实测）**：XML，每行一个裸 aid：
```
<metadata>
<book aid="3988" />
<book aid="3990" />
...
</metadata>
```
- **解析器**：`BookshelfSync.parseCloudAidList()`（正则 `aid="([^"]*)"`，容错：非数字 aid 跳过）。
- **调用方**：FavFragment 在主列表读取失败时的兜底（ids-only 端点）。

#### ㉛ `getBookshelfListParams(AppLanguage l)` — 全量书架 ★新版优化端点
- **请求**：`action=bookcase`（**不带 do=list** 的兄弟端点；需会话）。
- **响应**：XML，每本书附 **date（=元数据 LastUpdate）** 与 **最新一章（cid 与 LatestSection 一致，章节名**不含卷前缀**）**：
```xml
<metadata>
  <book aid="3988" date="2026-08-23">
    <name>书名</name>
    <chapter cid="41897">章节名</chapter>
  </book>
  ...
</metadata>
```
- **解析器**：`BookshelfListParser.parse()` → `List<Entry{aid,date,name,latestChapterCid,latestChapterName}>`；**空书架返回空列表而非 null**（区分"没货"与"解析失败"，失败若被当空书架会把整个本地书架误传上云端）。
- **调用方**：FavFragment.fetchShelfListing（当前只用 aid）；STABILITY_PLAN 指出该端点用于判断"哪些书过期需整本重下"（比对 date 与 latestChapterCid）。**`getBookshelfListParams` 本身在 app 中暂无直接调用方**，是已实现待切换的优化路径。

#### ㉜ `getAddToBookshelfParams(int aid)` — 加入书架
- **请求（实测确认）**：`action=bookcase&do=add&aid={aid}&t=0`（需会话）。
- **响应**：**整数状态码字符串**：
  - `1` = `SYSTEM_1_SUCCEEDED`（成功）
  - `5` = `SYSTEM_5_ALREADY_IN_BOOKSHELF`（已在书架，视为成功）
  - `6` = `SYSTEM_6_BOOKSHELF_FULL`（书架已满——**中断整个同步**）
- **用途**：书架云同步时，把本地有而云端没有的书逐本上传（`FavFragment.AsyncLoadAllFromCloud`）。

#### ㉝ `getDelFromBookshelfParams(int aid)` — 移出书架
- **请求（实测确认）**：`action=bookcase&do=del&aid={aid}&t=0`（需会话）。
- **响应**：**整数状态码字符串**：
  - `1` = `SYSTEM_1_SUCCEEDED`（成功）
  - `4` = `SYSTEM_4_NOT_LOGGED_IN`（未登录，视为成功清理本地）
  - `7` = `SYSTEM_7_NOVEL_NOT_IN_BOOKSHELF`（本就不在书架，视为成功清理本地）
- **成功语义**：清本地卷缓存 + 删除本地 intro 文件 + `removeFromLocalBookshelf(aid)`；本地移除失败返回 `LOCAL_BOOK_REMOVE_FAILED`。

**书架同步总体流程**（FavFragment，登录态）：拉云端列表（㉛ 优先，㉚ 兜底）→ `BookshelfSync.plan(local, cloud, force)` 算出 `toDownload`（云端有本地没有）+ `localOnly`（本地有云端没有）→ 并发 `NovelDownloader` 整本下载（index+meta+intro+每章 text+插图）→ `localOnly` 逐本调 ㉜ 上传。**保证：本地有而云端没有的书永不丢失**（只会被上传）。

### 5.9 敏感词

#### ㉞ `@Nullable String searchBadWords(String source)`
- **入参**：待检测文本（评论标题/内容）。
- **返回**：`String` —— 检测结果；**`null` 表示无敏感词**。被替换/拦截后的文本形态由服务端返回。
- **用途**：`NovelReviewNewPostActivity` / `NovelReviewReplyListActivity` 发帖/回复前先检测，命中则提示用户修改。

### 5.10 评论（书评）

#### ㉟ `getCommentListParams(int aid, int page)` — 某书的评论列表
- **请求（实测确认）**：`action=review&do=list&aid={aid}&page={page}&t=0`（分页从 1 起；匿名可用）。
- **响应（实测）**：XML（**根节点为 `<metadata>`**，`page` 的 `num` 为总页数；`posttime`/`replytime` 为 14 位 `yyyyMMddHHmmss`；`replies` 为回复数；`user` 文本为用户名、`uid` 为用户 ID）：
```xml
<?xml version="1.0" encoding="utf-8"?>
<metadata>
<page num='21'/>
<item rid='288359' posttime='20241018111509' replies='3' replytime='20260527132606'>
<user uid='1414951'><![CDATA[3298118397]]></user>
<content>评论标题</content>
</item>
...
</metadata>
```
- **解析器**：`Wenku8Parser.parseReviewList()` → `ReviewList{totalPage,currentPage,list:[Review{rid,postTime,noReplies,lastReplyTime,userName,uid,title}]}`。
- **发帖/回复 action 待确认**：推测为 `action=review&do=post` / `action=review&do=reply`（含 `title`/`content`），需 api-core 源码确认。

#### ㊱ `getCommentContentParams(int rid, int page)` — 某评论帖的分页回复
- **请求**：`rid` 为评论帖 ID，`page` 分页。
- **响应**：XML
```xml
<result>
  <page num='2'/>
  <item timestamp="20150721150322">
    <user uid="123">用户名</user>
    <content>回复内容</content>
  </item>
  ...
</result>
```
- **解析器**：`Wenku8Parser.parseReviewReplyList()` → `ReviewReplyList{totalPage,currentPage,list:[ReviewReply{replyTime,userName,uid,content}]}`。

#### ㊲ `getCommentNewThreadParams(int aid, String title, String content)` — 发表新评论
- **入参**：`aid` 小说 ID、`title` 评论标题、`content` 评论正文。
- **响应**：**整数状态码**，`1` 成功；非 1 报网络错误。发帖前先用 ㉞ 敏感词检测。
- 响应非整数时 `Integer.valueOf` 会抛异常（调用方外层按失败处理）。

#### ㊳ `getCommentReplyParams(int rid, String content)` — 回复评论
- **入参**：`rid` 目标评论帖 ID、`content` 回复内容。
- **响应**：**整数状态码**，`1` 成功。

---

## 6. 错误码表（Wenku8Error）

`Wenku8Error.ErrorCode` 枚举，`getSystemDefinedErrorCode(int)` 将服务器返回整数映射为枚举。

### 6.1 服务器/系统错误码（整数 0~22）

| 整数 | 枚举 | 含义 | 触发现场 |
|---|---|---|---|
| 0 | `SYSTEM_0_REQUEST_ERROR` | 请求错误 | — |
| 1 | `SYSTEM_1_SUCCEEDED` | 成功 | 登录/签到/投票/书架/发帖通用 |
| 2 | `SYSTEM_2_ERROR_USERNAME` | 用户名错误 | 登录 |
| 3 | `SYSTEM_3_ERROR_PASSWORD` | 密码错误 | 登录 |
| 4 | `SYSTEM_4_NOT_LOGGED_IN` | 未登录 | 用户信息/注销/移除书架 |
| 5 | `SYSTEM_5_ALREADY_IN_BOOKSHELF` | 已在书架 | 加书架 |
| 6 | `SYSTEM_6_BOOKSHELF_FULL` | 书架已满 | 加书架（中断同步） |
| 7 | `SYSTEM_7_NOVEL_NOT_IN_BOOKSHELF` | 小说不在书架 | 移出书架 |
| 8 | `SYSTEM_8_TOPIC_NOT_EXIST` | 帖子不存在 | 评论 |
| 9 | `SYSTEM_9_SIGN_FAILED` | 签到失败 | 每日签到 |
| 10 | `SYSTEM_10_RECOMMEND_FAILED` | 推荐失败 | 投票 |
| 11 | `SYSTEM_11_POST_FAILED` | 发帖失败 | 发表评论 |
| 22 | `SYSTEM_22_REFER_PAGE_0` | 引用页为 0 | 分页 |

### 6.2 客户端/本地错误码

| 枚举 | 含义 |
|---|---|
| `ERROR_DEFAULT` | 默认错误 |
| `RETURNED_VALUE_EXCEPTION` | 返回值异常（如响应应为整数却是非整数） |
| `BYTE_TO_STRING_EXCEPTION` | 字节转字符串异常 |
| `USER_INFO_EMPTY` | 用户信息为空 |
| `NETWORK_ERROR` | 网络错误（请求返回 null） |
| `STORAGE_ERROR` | 存储错误 |
| `IMAGE_LOADING_ERROR` | 图片加载错误 |
| `STRING_CONVERSION_ERROR` | 字符串转换错误 |
| `XML_PARSE_FAILED` | XML 解析失败 |
| `USER_CANCELLED_TASK` | 用户取消任务 |
| `PARAM_COUNT_NOT_MATCHED` | 参数数量不匹配 |
| `LOCAL_BOOK_REMOVE_FAILED` | 本地书籍删除失败 |
| `SERVER_RETURN_NOTHING` | 服务器无返回 |

---

## 7. HTTP 层（LightNetwork）

`org.mewx.wenku8.network.LightNetwork`（新版）。

| 接口 | 返回 | 说明 |
|---|---|---|
| `String encodeToHttp(String str)` | `String` | URL 编码（UTF-8），异常时返回 `""` 防崩溃 |
| `String encodeToHttp(String str, String encoding)` | `String` | URL 编码（指定字符集） |
| `@Nullable byte[] LightHttpPostConnection(String u, ContentValues values)` | `byte[]`/`null` | **POST 主入口**。`u` = BASE_URL，`values` = 上述各 `*Params` 方法返回的 ContentValues。失败/非 200 返回 `null`。所有调用方以 null 判定网络失败 |
| `@Nullable byte[] LightHttpDownload(String url)` | `byte[]`/`null` | GET 下载（封面大图、头像、正文插图等小文件） |

---

## 8. 获取真实 api 子模块与验证清单

### 8.1 检出私有子模块
```
git submodule update --init studio-android/LightNovelLibrary/api
# 需要 GitHub 私钥有 MewX/wenku8-api-core 的读权限
```
检出后 `settings.gradle` 会自动切到真实实现（无 `-DforceApiStub` 时）。真实模块内含 `api/src/test/.../Wenku8APITest.java` 与 `BASE_URL`、各 `*Params` 的真实 `ContentValues` 构建——**用它可以逐字节核验 5.3~5.10 的请求参数**。

### 8.2 已实测验证项（2026-09-08，live + 测试账号）

| 项 | 结果 |
|---|---|
| 真实 BASE_URL | `http://app.wenku8.cn/android.php`（HTTP 明文；`.com` 已死、HTTPS 空） |
| 请求封装 | `request` = Base64(UTF-8 协议串)，POST form —— **与文档一致** |
| 登录(API) | `action=login&username={u}&password={url编码}` → `1` 成功；下发 `PHPSESSID` |
| 登录(网页) | `POST https://www.wenku8.net/login.php?do=submit` → 下发 **`jieqiUserInfo`+`PHPSESSID`**（JIEQI CMS） |
| 用户信息 | `action=userinfo&t=0` → XML 与文档一致 |
| 书架 | `action=bookcase&do=list/add/del` → `1`/`1`/`<metadata></metadata>`（空书架） |
| 投票 | `action=book&do=vote` → XML `PushCount`（推荐计数+1） |
| 评论列表 | `action=review&do=list` → XML 与文档一致 |
| 小说 info/meta/list/text | XML 响应与文档示例**逐字段一致** |
| 搜索 | `search` 匿名可用，富格式 |
| **列表（API）** | **`novellist`/`articlelist` 已废弃**：匿名/API会话/网页会话一律 `4` |
| **列表（网页）** | **`www.wenku8.net/modules/article/articlelist.php` 需 JIEQI 登录**；支持 `sort`/`class`/`fullflag`/`initial`/`page`；GBK HTML，条目仅 aid+书名 |

### 8.3 尚未验证 / 待办项
- 网页列表各 `sort`/`class` 组合的真实分页与条目数（重写时按需补测）
- 签到 `sign` 的 action（探测 `action=sign`、`user&do=sign`、`checkin` 均 `0`）
- 评论发帖/回复的 `action=review&do=post/reply` 精确参数
- 头像 URL 规则（`getAvatarURL(uid)`）
- 邮箱登录（推测 `action=login&email=…&password=…`）
- 书架全量端点 `action=bookcase`（不带 do）的登录态真实 XML

### 8.4 建议核验的差异点
| 项 | 现状（基于旧版+计划文档推断） | 应以子模块核验 |
|---|---|---|
| `BASE_URL` | 旧版 `http://app.wenku8.com/android.php` | 新版真实值 |
| `REGISTER_URL` | 桩 `UNKNOWN` | 真实注册页 |
| 请求封装 | `request` = Base64(协议串) | 新版 ContentValues 键名/加密方式 |
| 登录/签到/投票 | 整数状态码 | 请求参数名 |
| 评论各端点 | `action=…` 协议串 | 精确 action/do 值 |
| 头像 | POST 二进制 | 请求方式 |

### 8.3 已知的"无调用方/预留"接口
- `getVoteNovelParams`（投票，未接入 UI）
- `getMewxNovelList`（同端点预留）
- `getBookshelfListParams`（书架全量列表，已实现待切换）
- `getNovelShortInfo`（新版已由 `getNovelShortInfoUpdate_CV` 取代）
