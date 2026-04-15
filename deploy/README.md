# 教师评价考核平台 - Docker Compose 部署

## 前提条件

- Docker Engine >= 20.10
- Docker Compose >= 2.0
- 至少 4GB 可用内存
- 端口 80, 3306, 6379, 8080, 9000, 9001 可用

## 快速开始

### 1. 克隆项目并进入部署目录

```bash
cd C:\TeacherEvaluation\deploy
```

### 2. 配置镜像仓库访问

如果使用 VKE 私有镜像，需要先登录：

```bash
docker login teacher-eval-cn-beijing.cr.volces.com -u <用户名> -p <密码>
```

或者修改 `docker-compose.yml` 中的镜像地址为公开仓库。

### 3. 启动所有服务

```bash
docker-compose up -d
```

### 4. 查看服务状态

```bash
docker-compose ps
```

或

```bash
docker-compose logs -f
```

### 5. 访问应用

- 前端页面: http://localhost
- 后端 API: http://localhost:8080
- MinIO 控制台: http://localhost:9001 (用户名: minioadmin / 密码: minioadmin)

## 服务说明

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| MinIO | 9000 (API), 9001 (控制台) | 对象存储 |
| Backend | 8080 | Spring Boot API |
| Frontend | 80 | Nginx 前端 |

## 数据持久化

数据存储在 `data/` 目录：
- `data/mysql/` - MySQL 数据
- `data/minio/` - MinIO 文件存储

## 常用命令

```bash
# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose down

# 查看日志
docker-compose logs -f [服务名]

# 重启单个服务
docker-compose restart [服务名]

# 进入容器
docker-compose exec [服务名] /bin/sh

# 重新构建镜像
docker-compose build --no-cache
```

## 故障排查

### 服务无法启动

```bash
# 查看详细日志
docker-compose logs [服务名]

# 检查端口占用
netstat -ano | findstr "3306 6379 8080 9000"
```

### 数据库连接失败

```bash
# 检查 MySQL 是否就绪
docker-compose exec mysql mysqladmin ping -uroot -proot123

# 查看 MySQL 日志
docker-compose logs mysql
```

### 镜像拉取失败

确认已登录私有仓库：
```bash
docker login teacher-eval-cn-beijing.cr.volces.com
```

## 生产环境建议

1. 修改默认密码
2. 配置 SSL/HTTPS
3. 定期备份数据目录
4. 配置日志轮转
5. 使用 Docker Swarm 或 Kubernetes 进行生产部署