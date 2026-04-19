# usdrub-rpc

Сервис для получения курса USD/RUB по gRPC.

Три модуля:
- `api` — proto-определение `CurrencyService` с методом `GetRate`
- `currency-rate-provider` — gRPC-сервер, отдаёт текущий курс (рандом вокруг базового значения)
- `rate-printer` — клиент, раз в 5 секунд запрашивает курс и печатает в лог

Стек: Java 17, Spring Boot 3.2, gRPC 1.61, Protobuf 3.25, Gradle.

---

## ДЗ-1: gRPC-сервис курса валют

Написан простой producer-consumer через gRPC.

Proto (`api/src/main/proto/currency.proto`):
```protobuf
service CurrencyService {
  rpc GetRate(GetRateRequest) returns (GetRateResponse);
}
```

Provider поднимает Netty gRPC-сервер, реализует `GetRate` — возвращает курс USD/RUB (базовый 95.0 ± случайное отклонение). Printer подключается к нему, шедулером дёргает ручку каждые 5 секунд и логирует результат.

### Локальный запуск (без Docker)

Нужен Gradle и JDK 17+.

```bash
# собрать всё
gradle build -x test

# запустить провайдер (порт 9090)
gradle :currency-rate-provider:bootRun

# в другом терминале — запустить потребитель
gradle :rate-printer:bootRun
```

---

## ДЗ-2: ZooKeeper как service registry

Подключил ZooKeeper для автоматической регистрации провайдеров и обнаружения их потребителем.

### Что сделано

**Provider** — при старте, после поднятия gRPC-сервера, регистрируется в ZooKeeper. Создаёт ephemeral sequential узел по пути `/services/currency-service/instance-XXXXXXXXXX`, в данные записывает свой `host:port`. Ephemeral-узлы автоматически удаляются, когда инстанс отваливается (сессия с ZK разрывается) — то есть дерегистрация происходит сама.

Хост определяется автоматически через `InetAddress.getLocalHost()` — в Docker это IP контейнера во внутренней сети, и остальные контейнеры могут до него достучаться.

**Consumer** — перед каждым запросом курса обращается к ZooKeeper, получает список детей `/services/currency-service/`, читает из каждого адрес инстанса. Выбирает следующий по round-robin. Каналы к инстансам кешируются, чтобы не пересоздавать на каждый запрос.

Если провайдеров нет — просто пишет warning и пропускает итерацию.

Для работы с ZK используется Apache Curator Framework (`curator-framework:5.6.0`).

### Как запустить

Нужен Docker.

```bash
docker compose up --build
```

Поднимется:
- ZooKeeper на порту 2181
- 3 инстанса провайдера (все на порту 9090, но в разных контейнерах)
- 1 потребитель

В логах потребителя видно, как он по очереди ходит к разным инстансам:

```
[Instance 172.20.0.4:9090] USDRUB rate: 95.99 (total instances: 3)
[Instance 172.20.0.3:9090] USDRUB rate: 94.25 (total instances: 3)
[Instance 172.20.0.5:9090] USDRUB rate: 94.18 (total instances: 3)
```

Посмотреть логи отдельно:
```bash
docker compose logs consumer -f
docker compose logs provider-1 -f
```

Остановить:
```bash
docker compose down
```

### Структура

```
├── api/                          — proto + сгенерированные стабы
├── currency-rate-provider/       — gRPC-сервер + регистрация в ZK
│   ├── Dockerfile
│   └── src/.../provider/
│       ├── GrpcServerRunner.java       — запуск сервера + регистрация
│       ├── ZookeeperConfig.java        — бин CuratorFramework
│       ├── CurrencyServiceImpl.java    — реализация GetRate
│       └── RateService.java            — генерация курса
├── rate-printer/                 — клиент + обнаружение через ZK
│   ├── Dockerfile
│   └── src/.../printer/
│       ├── RatePrinter.java            — шедулер, печатает курс
│       ├── ServiceDiscoveryClient.java — обнаружение инстансов + round-robin
│       └── ZookeeperConfig.java        — бин CuratorFramework
└── docker-compose.yml            — ZK + 3 провайдера + 1 потребитель
```


### Метрики

