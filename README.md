# usdrub-rpc

Сервис для получения курса USD/RUB по **gRPC** с регистрацией инстансов в **ZooKeeper**, наблюдаемостью (**Prometheus** / **Grafana**) и контрактными тестами (**Pact**).

**Модули**

| Модуль | Назначение |
|--------|------------|
| `api` | Proto `CurrencyService` / `GetRate` + сгенерированный gRPC-код |
| `currency-rate-provider` | gRPC-сервер, регистрация в ZK |
| `rate-printer` | Клиент, discovery + round-robin, запрос раз в 5 с |

**Стек:** Java 17, Spring Boot 3.2, gRPC 1.61, Protobuf 3.25, Gradle 8.5 (wrapper).

Подробный разбор доработок по **12-factor** (build/release/run, graceful shutdown, профили, логи): [docs/TWELVE_FACTOR.md](docs/TWELVE_FACTOR.md).

---

## 12-factor: кратко

- **Конфигурация** — через `application.properties` и переменные окружения (в Docker задаются в `docker-compose.yml`).
- **Build / Release / Run** — сборка через `./gradlew`; релиз как версионированный Docker-образ (`Dockerfile.release` или `docker compose build` с полем `image:`); запуск без пересборки: `docker compose up --no-build` при уже собранных образах.
- **Graceful shutdown** — Spring `server.shutdown=graceful`, ожидание gRPC и gRPC-клиентских каналов; в compose задан `stop_grace_period`.
- **Dev / prod** — профили Spring `dev` (по умолчанию локально) и `prod` (в контейнерах через `SPRING_PROFILES_ACTIVE`).
- **Логи** — поток в stdout/stderr; шумные payload-логи на уровне DEBUG.

---

## Локальный запуск (без Docker)

Нужны **JDK 17+** и Git.

```bash
./gradlew build -x test

./gradlew :currency-rate-provider:bootRun
# другой терминал:
./gradlew :rate-printer:bootRun
```

По умолчанию активен профиль **`dev`**. Для prod-поведения локально:

```bash
set SPRING_PROFILES_ACTIVE=prod
./gradlew :currency-rate-provider:bootRun
```

(В PowerShell: `$env:SPRING_PROFILES_ACTIVE="prod"`.)

Переменные: `ZOOKEEPER_ADDRESS`, `GRPC_SERVER_PORT`, `SPRING_APPLICATION_NAME` — см. `application.properties` модулей.

---

## Docker Compose (весь стек)

```bash
docker compose up --build
```

Поднимется ZooKeeper, три провайдера, consumer, Prometheus, Grafana, Pact Broker + Postgres. Образы приложений тегируются как `usdrub-rpc/currency-rate-provider:${IMAGE_TAG:-latest}` и `usdrub-rpc/rate-printer:${IMAGE_TAG:-latest}`.

Пример переменных: [.env.example](.env.example). Скопируйте в `.env` при необходимости (файл `.env` не коммитится).

**Логи**

```bash
docker compose logs consumer -f
docker compose logs provider-1 -f
```

**Остановка**

```bash
docker compose down
```

Контейнерам приложений задан `stop_grace_period`, чтобы уложиться в graceful shutdown JVM/gRPC.

---

## Сборка «релизного» образа только из JAR

См. [currency-rate-provider/Dockerfile.release](currency-rate-provider/Dockerfile.release) и [rate-printer/Dockerfile.release](rate-printer/Dockerfile.release): сначала соберите JAR, затем `docker build -f …/Dockerfile.release`.

---

## Версия артефакта

Версия Gradle-проекта задаётся переменными `CI_VERSION` или `PROJECT_VERSION`, либо `-PprojectVersion=…` (см. корневой `build.gradle`). Удобно для CI/CD.

---

## Метрики и дашборды

- **Prometheus:** порт хоста `9091` → контейнер `9090`, конфиг [monitoring/prometheus/prometheus.yml](monitoring/prometheus/prometheus.yml).
- **Grafana:** `http://localhost:3000` (логин/пароль по умолчанию в `docker-compose.yml` — только для демо).
- Приложения экспортируют Actuator `/actuator/prometheus` (порты `18081`–`18084` на хосте сопоставлены с `8080` в контейнерах).

---

## Pact

В compose подняты `pact-postgres` и `pact-broker`. Провайдерские тесты брокера включаются переменной `PACT_BROKER_ENABLED=true` (см. исходники тестов в `currency-rate-provider`).

---

## Структура репозитория (упрощённо)

```
├── api/
├── currency-rate-provider/
│   ├── Dockerfile
│   └── Dockerfile.release
├── rate-printer/
│   ├── Dockerfile
│   └── Dockerfile.release
├── docker-compose.yml
├── docs/
│   └── TWELVE_FACTOR.md
├── monitoring/
└── gradlew / gradlew.bat / gradle/wrapper/
```

---

## ДЗ-1 и ДЗ-2 (история)

**ДЗ-1:** gRPC `GetRate` — провайдер отдаёт курс около 95 ± 1, consumer опрашивает по расписанию.

**ДЗ-2:** ZooKeeper — провайдер регистрируется в `/services/currency-service/instance-…` (ephemeral sequential), consumer читает детей и ходит round-robin, кешируя каналы.
