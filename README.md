# CS2Trade

一个面向 CS2 饰品交易与社区内容运营的全栈项目，包含公开市场、饰品详情、价格分析、资讯投稿、玩家秀、用户中心与后台管理能力。

## 项目简介

CS2Trade 以 CS2 饰品交易场景为核心，提供从饰品浏览、交易撮合到内容互动的一体化体验。项目采用前后端分离架构，前端负责市场与社区交互，后端负责订单、库存、消息、推荐、定价建议与 Steam 数据同步。

## 功能亮点

- 市场浏览：支持在售列表、最新上架、热点饰品浏览与详情页查看。
- 价格能力：提供市场面板、成交记录、定价建议、相似饰品推荐等分析能力。
- 社区内容：支持资讯投稿、资讯详情、玩家秀发布、玩家秀详情与互动。
- 用户中心：提供库存、出售记录、求购记录、订单、收藏、投稿管理等页面。
- 后台管理：覆盖饰品、订单、消息、内容运营、玩家秀与同步维护。
- Steam 集成：支持库存同步、市场数据补全、饰品图标修复与参考价同步。

## 技术栈

- 前端：Vue 3、Vue Router、Pinia、Element Plus、Axios、ECharts、Vite
- 后端：Spring Boot 3、MyBatis、MySQL、Redis、WebSocket
- 运行环境：JDK 21、Node.js 22、Maven 3.9

## 项目结构

```text
.
├─qd/   前端项目（Vue 3 + Vite）
├─hd/   后端项目（Spring Boot + MyBatis）
├─docs/
│  └─screenshots/  README 演示截图
└─开发文档.md
```

## 快速启动

### 前端

```bash
cd qd
npm install
npm run dev
```

默认访问地址：`http://localhost:3000`

### 后端

```bash
cd hd
mvn spring-boot:run
```

默认接口地址：`http://localhost:8080/api`

## 页面预览

### 首页

![首页预览](docs/screenshots/home.png)

### 饰品详情

![饰品详情](docs/screenshots/item-detail.png)

### 资讯中心

![资讯中心](docs/screenshots/news.png)

### 玩家秀

![玩家秀](docs/screenshots/player-shows.png)

## 项目说明

- 项目当前以 CS2 场景为主，同时保留了扩展到更多游戏品类的结构能力。
- 仓库内包含近期对推荐、定价建议、资讯投稿、玩家秀上传与 Steam 图片补全的完整实现。
- 如果你希望用于答辩或作品展示，建议结合 `开发文档.md` 与本 README 一起阅读。
