# qd-136 室内冲浪馆浪道辅助设备浪高档位适配绑定统计系统

## 项目简介

室内冲浪馆浪道辅助设备、浪高档位与绑定统计系统。项目包含 Vue/Vite 前端、Spring Boot 后端、MySQL 与 Redis，已按依赖层缓存和固定端口交付链路规范整理。

## 访问地址

- 前端地址: [http://localhost:8156](http://localhost:8156)
- 127.0.0.1 地址: [http://127.0.0.1:8156](http://127.0.0.1:8156)
- 后端 API: http://localhost:8146/api

## 端口

- 前端: 8156
- 后端: 8146
- MySQL: 3362
- Redis: 6435

## 编译与启动

```bash
cd backend
mvn compile -q

cd ../frontend
npm ci
npm run build

cd ..
docker compose up -d --build
```

Docker Compose 端口均绑定到 `127.0.0.1`，镜像基础地址通过 `.env` 中的 `DOCKER_REGISTRY` 统一控制。
