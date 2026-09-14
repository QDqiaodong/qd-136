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

## 角色与档位授权（辅助设备绑定页）

绑定页（`/binding`，辅助设备与浪高档位绑定）按角色收窄可见与可操作范围，**越权拦截以后端为准**（请求头 `X-Role` 标识当前角色）：

| 角色 | X-Role | 可见范围 | 可操作范围 |
| --- | --- | --- | --- |
| 馆长 | `DIRECTOR` | 全部浪高档位、全部设备 | 全部绑定 / 调整 |
| 浪道教练 | `COACH` | 仅授权档位上的防滑扶手、缓冲挡垫 | 仅能查看并调整授权档位上的防滑扶手、缓冲挡垫 |

- 教练默认授权档位为低浪 `LOW`、中浪 `MEDIUM`（高浪 `HIGH` 未授权），可在
  `backend/src/main/resources/application.yml` 的 `surf.security.coach-authorized-wave-levels` 配置。
- 教练对**未授权档位**发起绑定或调整（含把未授权档位上的设备调整走）时，后端返回
  `403 没有权限：……`，前端弹错提示，数据不会被改动。
- 绑定状态、可选设备、调整记录均由后端按角色过滤；刷新页面后列表仍只显示该角色被授权的设备
  （角色保存在浏览器 localStorage）。
- 顶部可切换“馆长 / 浪道教练”视角用于演示与验收。

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
