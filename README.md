# Flash-Sale-Voucher-Platform

A simplified flash-sale voucher demo built with Spring Boot, Redis, and MySQL.

## Features

- Phone login with verification code (demo code logged in browser console)
- Seckill voucher listing and purchase
- Redis Stream order processing
- One order per user per voucher

## Tech Stack

- Java 8+ / Spring Boot 2.7
- MySQL 5.7
- Redis 6+
- Vue + Element UI (static frontend)

## Quick Start

1. Start MySQL and Redis (Docker example):

```bash
docker start mysql57 redis
```

2. Import database:

```bash
mysql -h127.0.0.1 -uroot -proot -e "CREATE DATABASE IF NOT EXISTS hmdp;"
mysql -h127.0.0.1 -uroot -proot hmdp < hmdp.sql
mysql -h127.0.0.1 -uroot -proot hmdp < scripts/seed-english-data.sql
docker exec redis redis-cli -a root XGROUP CREATE stream.orders g1 $ MKSTREAM
```

3. Run the app:

```bash
mvn spring-boot:run
```

4. Open in browser:

- http://127.0.0.1:8081/index.html
- http://127.0.0.1:8081/login.html
- http://127.0.0.1:8081/seckill.html

## Test Accounts

| Phone | Nickname |
|-------|----------|
| 13686869696 | Alex Fisher |
| 13838411438 | Emma Lee |

After clicking **Send code**, open DevTools Console (`F12`) to see the verification code.

## License

Educational demo project.
