# Сервис новостей (news-app)

REST-сервис новостей на **Java 8 + RESTEasy + Undertow**. Это **один проект**, который
развивается от лабораторной работы к лабораторной: каждый коммит добавляет следующий
слой поверх предыдущего (а не создаёт новый проект с нуля).

## Последовательность работ

| Коммит | Работа | Что добавляется | Паттерны |
|--------|--------|-----------------|----------|
| 1 | **ЛР1** | Maven-проект, 3 пакета (`view`/`controller`/`model`), REST API (`ReaderAPI`, `CreatorAPI`, `AdminAPI` + `SingletonAPI`), веб-сервер Undertow. Методы возвращают текстовые заглушки. | Singleton |
| 2 | **ЛР2** | Пакет `view/api/dto`, DTO на каждый метод, провайдер Jackson. Методы возвращают DTO → JSON. | DTO |
| 3 | **ЛР3** | Бизнес-объекты (`NewsBO`, `CategoryBO`, иерархия `Person → User → Journalist/Admin`, `Role`, `Favorite`, `Tag`…), `FacadeController`, поиск по ключевым словам, статика + веб-интерфейс. | Facade, MVC |
| 4 | **ЛР4** | Интерфейс `NewsInterface`, контроллер `NewsController`, абстрактный `NewsDecorator` и `PositiveNewsDecorator` (фильтрация позитивных новостей); фасад выбирает декоратор по дате. | Decorator |
| 5 | **ЛР5** | Контроллер на каждый бизнес-объект (`CategoryController`, `TagController`, `RoleController`, `FavoriteController`, `UserController`, `CommentController`, `NewsController`), потокобезопасные кэши БО, бизнес-правила `NewsBO` (`changeTitle`, `publish`); преобразование БО в DTO осталось в фасаде. | Facade, MVC |
| 6 | **ЛР6** | Пакет `model/data` с DO-классами и пакет `controller/repository` с репозиторием на каждый контроллер; тестовые наборы данных лежат в репозиториях. `NewsBO` разделён на `NewsDO` (ядро + превью) и `NewsContentDO` (контент). | Repository |
| 7 | **ЛР7** | Пакет `model/dao` с интерфейсом `IDao` и реализациями `SqlDao` (SQLite) и `NoSqlDao` (MongoDB); репозитории переведены с карт в памяти на DAO. Зависимости `sqlite-jdbc` и `mongodb-driver-sync`. | DAO |

---

## Лабораторная работа №1 - REST API и Singleton

**Цель:** создать каркас REST-сервиса.

1. Создан Maven-проект с тремя модулями-пакетами: `view`, `model`, `controller`.
2. Подключены зависимости RESTEasy (`resteasy-jaxrs`, `resteasy-undertow`).
3. В `view/api` описаны:
   * `ReaderAPI` (`/reader`) - методы читателя;
   * `CreatorAPI` (`/creator`) - методы создателя;
   * `AdminAPI` (`/admin`) - методы администратора;
   * `SingletonAPI` - JAX-RS `Application`, отдающий **ровно по одному экземпляру**
     каждого API (паттерн «Одиночка»).
4. `view.WebAppServer` поднимает встроенный сервер Undertow и разворачивает `SingletonAPI`.
5. Все методы возвращают текстовые заглушки (бизнес-логики ещё нет).

**Проверка:** GET-методы открываются в браузере, POST/PUT/DELETE - через `requests.http` или curl.

---

## Лабораторная работа №2 - DTO и JSON

**Цель:** заменить текстовые ответы на структуры данных (JSON).

1. В `view/api` создан пакет `dto`.
2. Для каждого метода описаны DTO-классы с полями:
   * `NewsItemDTO`, `NewsTitleDTO`, `NewsListDTO` - новость, элемент списка, обёртка списка;
   * `CategoryDTO`, `CategoryListDTO` - категория и список;
   * `CommentDTO` - комментарий;
   * `ActionStatusDTO` - результат действия;
   * `UserDTO`, `UserListDTO`, `RoleDTO`, `RoleListDTO` - для администратора;
   * DTO запросов: `CreateNewsRequestDTO`, `UpdateNewsRequestDTO`,
     `CreateCommentRequestDTO`, `UpdateCommentRequestDTO`, `ReplyCommentRequestDTO`.
3. В каждом методе вместо текста возвращается экземпляр DTO с тестовыми данными.
4. Подключён `resteasy-jackson2-provider`; в `SingletonAPI` явно зарегистрирован
   `JacksonJsonProvider` - без него RESTEasy не найдёт `MessageBodyWriter` для DTO (ошибка 500).

**Проверка:** в браузере возвращается JSON-структура, а не текст.

---

## Лабораторная работа №3 - бизнес-объекты и Фасад

**Цель:** отделить бизнес-логику от представления через фасад.

1. В `controller` описаны бизнес-объекты (из UML-диаграммы классов):
   * интерфейсы `IAuthorizable`, `ICommentable`, `IPublishable`;
   * иерархия `PersonBO → GuestBO/UserBO → JournalistBO/AdminBO`;
   * сущности `NewsBO` (со статусами `DRAFT/PUBLISHED/EDITED/ARCHIVED/DELETED`),
     `CommentBO`, `CategoryBO`, `TagBO`, `FavoriteBO`, `RoleBO`;
   * `RegistrationData`, `NewsData` - входные данные методов.
2. Описан `FacadeController`:
   * скрывает BO и хранилище от API;
   * принимает DTO запросов и возвращает DTO ответов;
   * реализует **поиск новостей по ключевым словам** с объединением результатов из
     текста новостей и названий категорий;
   * выполняет CRUD новостей, комментариев, избранного и администрирование.
3. `ReaderAPI`, `CreatorAPI`, `AdminAPI` переведены на фасад (без прямого доступа к данным).
4. В `model` - хранилище `InMemoryStore<T>` и генератор `IdSequence`.
5. Сервер настроен на раздачу статики (`/static`), добавлен клиентский веб-интерфейс.

**Проверка:** клиентский интерфейс на `http://localhost:8080/`, все запросы - в `requests.http` или через тестер API (`/static/tester.html`).

---

## Лабораторная работа №4 - паттерн «Декоратор»

**Цель:** дополнить поведение контроллера новостей, не меняя его код.

1. Создан интерфейс `NewsInterface` - фиксирует набор методов контроллера новостей.
2. `NewsController` реализует `NewsInterface` - вся логика работы с новостями,
   категориями и тегами вынесена из фасада в этот контроллер.
3. Создан абстрактный декоратор `NewsDecorator` - реализует `NewsInterface`, хранит
   внутри декорируемый контроллер и делегирует ему все вызовы.
4. Создан `PositiveNewsDecorator` - наследник декоратора, который дополняет методы
   выдачи списка новостей фильтрацией: новости с «негативными» словами в заголовке
   (катастрофа, кризис, авария, пожар, война, трагедия) исключаются из результата.
5. В `FacadeController`:
   * добавлен метод `isEndOfYearNear()` - проверяет, осталось ли до конца года меньше двух месяцев;
   * поле контроллера новостей заменено на интерфейс `NewsInterface`;
   * при создании контроллера, если до конца года меньше двух месяцев, создаётся
     `PositiveNewsDecorator`, иначе - обычный `NewsController`.

**Проверка:** когда декоратор активен, негативная новость «Кризис на рынке
недвижимости» не попадает в ленту (`GET /reader/news/all`).

---

## Лабораторная работа №5 - разделение бизнес-объекта и контроллера

**Цель:** убрать из бизнес-объектов управление их существованием и распределить
ответственность между отдельными контроллерами.

1. Для каждого бизнес-объекта создан собственный контроллер:
   * `NewsController` - новости, публикация, категории и теги новости;
   * `CategoryController`, `TagController`, `RoleController`;
   * `UserController` - регистрация, вход, блокировка и назначение ролей;
   * `CommentController` - комментарии и модерация;
   * `FavoriteController` - избранное пользователя.
2. Бизнес-объекты больше не содержат методов `getAll`, `getById`, `create`, `save`,
   `update`, `delete` и не управляют своим существованием:
   * `JournalistBO` не создаёт, не редактирует и не удаляет новости;
   * `AdminBO` не блокирует пользователей и не модерирует комментарии;
   * `GuestBO` не регистрирует пользователей;
   * `UserBO` не хранит список избранного.
3. Каждый контроллер держит собственный потокобезопасный кэш бизнес-объектов
   (`InMemoryStore` на `ConcurrentSkipListMap`).
4. В `NewsBO` добавлены бизнес-правила:
   * `changeTitle` - заголовок можно менять только у неопубликованной новости,
     пустой заголовок запрещён;
   * `publish` - нельзя опубликовать новость без заголовка.
5. Нарушение правил API возвращает как `409 Conflict` (состояние новости) или
   `400 Bad Request` (некорректное значение поля).
6. Сеттеры, которые позволяли обойти правила, удалены: статус меняется только
   через `publish`, `unpublish`, `archive` и `markEdited`, коллекции - через
   `addCategory`, `addTag`, `addMedia`, модерация - через `approve` и `reject`.
7. `FacadeController` связывает контроллеры между собой и преобразует БО в DTO;
   декоратор `PositiveNewsDecorator` по-прежнему оборачивает `NewsController`.

**Проверка:** созданная новость сразу публикуется. Если попробовать изменить её
заголовок через `PUT /creator/news/{id}`, сервер вернёт `409` с сообщением
«Нельзя менять заголовок опубликованной новости». После `POST /creator/news/{id}/unpublish`
заголовок снова можно менять.

---

## Лабораторная работа №6 - паттерн «Репозиторий»

**Цель:** вынести хранение данных из контроллеров в отдельный слой доступа к
данным (репозитории) и описать модель данных через DO-классы.

1. Из бизнес-объектов выделены модели данных - DO-классы в пакете `model/data`:
   * `NewsDO` - «ядро + превью» новости: заголовок, анонс, регион, рейтинг,
     статус и ссылки на автора/категории/теги (хранятся идентификаторами);
   * `NewsContentDO` - полный текст новости, нужен только при открытии статьи
     (другая модель данных, имитирует документное хранилище);
   * `CategoryDO`, `TagDO`, `RoleDO`, `UserDO`, `CommentDO`, `FavoriteDO` -
     по одному DO-классу на остальные бизнес-объекты.
2. Для каждого контроллера создан класс-репозиторий в пакете `controller/repository`
   (общий интерфейс `IRepository<T>`): `NewsRepository`, `CategoryRepository`,
   `TagRepository`, `RoleRepository`, `UserRepository`, `CommentRepository`,
   `FavoriteRepository`. Репозиторий преобразует BO в DO и обратно и выдаёт
   идентификаторы.
3. В репозитории добавлены тестовые наборы данных (роли, пользователи,
   категории, теги, новости и комментарий) - внешние СУБД не подключаются,
   данные хранятся в потокобезопасных in-memory-картах. `NewsRepository` держит
   два хранилища: `sqlStore` (`NewsDO`) и `nosqlStore` (`NewsContentDO`).
4. Контроллеры загружают кэш из своего репозитория при старте, а каждую
   изменяющую операцию сразу сохраняют в репозиторий. Класс `SeedData` удалён:
   начальные данные теперь живут в репозиториях.
5. В `NewsBO` добавлен конструктор, восстанавливающий статус из хранилища;
   статус по-прежнему меняется только бизнес-методами (`publish`, `unpublish`,
   `archive`, `markEdited`).

**Проверка:** после запуска читателю доступны 6 опубликованных новостей
(негативная «Кризис на рынке недвижимости» скрыта декоратором), а создание,
публикация и модерация по-прежнему работают через API.

---

## Лабораторная работа №7 - доступ к данным через DAO

**Цель:** заменить in-memory-хранилища в репозиториях на доступ к настоящим
СУБД через слой DAO (паттерн Data Access Object).

1. Создан пакет `model/dao` с DAO-классами:
   * `IDao<T, ID>` - базовый интерфейс доступа к данным (`save`, `findById`,
     `findAll`, `deleteById`); дженерики позволяют использовать один класс для
     разных DO-классов;
   * `SqlDao<T, ID>` - DAO для реляционного хранилища **SQLite**: таблица
     создаётся автоматически по полям DO-класса, списки (`List<Long>`,
     `List<String>`) хранятся в `TEXT` как JSON;
   * `NoSqlDao<T, ID>` - DAO для документного хранилища **MongoDB**: каждое
     поле DO-класса становится ключом BSON-документа;
   * `Database` - настройки подключения: открывает соединение SQLite и MongoDB.
2. Добавлены настройки подключения к СУБД:
   * в `pom.xml` - зависимости `sqlite-jdbc` (3.45.1.0) и `mongodb-driver-sync` (5.1.1);
   * `Database.SQLITE_URL = jdbc:sqlite:news.db` - файл базы создаётся рядом с приложением;
   * `Database.MONGO_URI = mongodb://localhost:27017`, имя базы `newsdb`.
3. Репозитории переведены с карт в памяти на DAO: `NewsRepository` держит
   `sqlDao` (ядро `NewsDO`) и `nosqlDao` (контент `NewsContentDO`), остальные
   репозитории - по одному `sqlDao`. Тестовые наборы записываются в БД при
   первом запуске (если БД пуста), а данные, созданные через HTML-интерфейс,
   сразу сохраняются в БД.
4. Если сервер MongoDB не запущен, `NoSqlDao` прозрачно переключается на
   временное хранилище в памяти: приложение остаётся работоспособным, а SQLite
   продолжает сохранять «ядро» новостей и остальные сущности.

**Проверка:** после запуска в рабочем каталоге появляется файл `news.db`
(SQLite) с таблицами `roles`, `users`, `categories`, `tags`, `news`, `comments`,
`favorites`, заполненными тестовыми данными. При запущенном MongoDB контент
новостей сохраняется в коллекцию `news_contents` базы `newsdb`.

---

## Итоговый показ программы - краткий монолог

Это REST-сервис новостей на Java 8 (RESTEasy + Undertow) - один Maven-проект,
который развивался от ЛР1 к ЛР7: каждый коммит добавлял следующий слой поверх
предыдущего.

**Архитектура.** Три слоя: `view` (REST-интерфейсы `ReaderAPI`, `CreatorAPI`,
`AdminAPI`, DTO и веб-сервер), `controller` (бизнес-объекты, их контроллеры,
фасад, декоратор и репозитории) и `model` (DO-классы, DAO и подключение к СУБД).
По работам применялись паттерны: Singleton, DTO, Facade/MVC, Decorator,
Repository и DAO. Новость разделена на две модели данных: «ядро + превью»
хранится в SQLite, полный текст - в MongoDB, а доступ к ним идёт через
`SqlDao` и `NoSqlDao`, реализующие дженерик-интерфейс `IDao<T, ID>`.

**Что было трудно.** Главная сложность - вынести хранение наружу, не нарушив
бизнес-правила. Раньше сеттеры позволяли обходить правила (например, напрямую
выставить статус `PUBLISHED`), поэтому их удалили: статус меняется только через
`publish`, `unpublish`, `archive` и `markEdited`, а восстановление из хранилища
выполняется через конструктор.

**Два вопроса, отнявших больше всего времени.**
1. Как сделать DAO универсальным для разных DO-классов. Решение - дженерики
   `IDao<T, ID>` и рефлексия: таблица строится по полям класса, списки
   сериализуются в JSON.
2. Как подключить СУБД, не нарушая чистой архитектуры, и сохранить
   работоспособность без запущенного MongoDB. Решение - вынести настройки в
   класс `Database`, а `NoSqlDao` при недоступности MongoDB прозрачно
   переключается на хранилище в памяти.

**Демонстрация.** Работоспособность проверяется через веб-интерфейс
(`http://localhost:8080/`) и тестер API (`/static/tester.html`). История
коммитов сохранена в git (по коммиту на каждую работу), задачи велись в redmine.

---

## Полный API (по итогам ЛР1-7)

### ReaderAPI - `/reader`

| Метод | Путь | Ответ | Описание |
|-------|------|-------|----------|
| GET | `/news/all` | `NewsListDTO` | все новости |
| GET | `/news/{id}` | `NewsItemDTO` | новость по id |
| GET | `/news/last_week` | `NewsListDTO` | за 7 дней |
| GET | `/news/search?keywords=` | `NewsListDTO` | поиск (текст + категория) |
| GET | `/news/region?region=` | `NewsListDTO` | по региону |
| GET | `/categories` | `CategoryListDTO` | категории |
| POST | `/auth/register?name=&email=&password=` | `UserDTO` | регистрация |
| POST | `/auth/login?login=&password=` | `ActionStatusDTO` | вход |
| POST | `/news/{id}/bookmark?userId=` | `ActionStatusDTO` | в избранное |
| DELETE | `/news/{id}/bookmark?userId=` | `ActionStatusDTO` | убрать из избранного |
| GET | `/favorites?userId=` | `NewsListDTO` | избранное |
| POST | `/comments` | `CommentDTO` | комментарий |
| PUT | `/comments/{id}` | `CommentDTO` | изменить комментарий |
| DELETE | `/comments/{id}` | `ActionStatusDTO` | удалить комментарий |

### CreatorAPI - `/creator`

| Метод | Путь | Ответ | Описание |
|-------|------|-------|----------|
| POST | `/news` | `NewsItemDTO` | создать новость |
| PUT | `/news/{id}` | `NewsItemDTO` | изменить новость |
| DELETE | `/news/{id}` | `ActionStatusDTO` | удалить новость |
| POST | `/news/{id}/publish` | `NewsItemDTO` | опубликовать |
| POST | `/news/{id}/unpublish` | `NewsItemDTO` | снять с публикации |
| POST | `/news/{id}/archive` | `NewsItemDTO` | в архив |
| POST | `/news/{id}/tags?tag=` | `ActionStatusDTO` | добавить тег |
| POST | `/news/{id}/media?url=` | `ActionStatusDTO` | прикрепить медиа |

### AdminAPI - `/admin`

| Метод | Путь | Ответ | Описание |
|-------|------|-------|----------|
| GET | `/users` | `UserListDTO` | пользователи |
| GET | `/roles` | `RoleListDTO` | роли |
| POST | `/users/{id}/block` | `ActionStatusDTO` | заблокировать |
| PUT | `/users/{id}/role?role=` | `ActionStatusDTO` | назначить роль |
| POST | `/comments/{id}/moderate?action=` | `ActionStatusDTO` | модерировать комментарий |

---

## Архитектура и путь запроса

### Три слоя

Проект разделён на три слоя, зависимости направлены сверху вниз
(`view → controller → model`): верхний слой знает только о слое под ним.

| Слой | Пакет | Отвечает за |
|------|-------|-------------|
| представление | `view` | REST-интерфейсы, DTO, веб-сервер Undertow |
| бизнес-логика | `controller` | бизнес-объекты, контроллеры, фасад, декоратор, репозитории |
| данные | `model` | DO-классы, DAO, подключение к SQLite и MongoDB |

### Паттерны

| Паттерн | Где | Роль |
|---------|-----|------|
| Singleton | `SingletonAPI`, `FacadeController`, `Database` | один экземпляр на приложение |
| DTO | `view/api/dto/*` | плоские структуры для обмена с клиентом |
| Facade | `FacadeController` | единый вход в бизнес-логику |
| MVC | три слоя | Model = BO, View = API + DTO, Controller = контроллеры |
| Decorator | `NewsDecorator`, `PositiveNewsDecorator` | оборачивает `NewsController`, фильтрует негативные новости |
| Repository | `controller/repository/*` | слой доступа к данным поверх BO |
| DAO | `model/dao/*` | доступ к конкретной СУБД через DO-классы |

Цепочка хранения: `NewsBO → NewsRepository → NewsDO (ядро) → SqlDao → SQLite`
и `NewsBO → NewsRepository → NewsContentDO (контент) → NoSqlDao → MongoDB`.

### Путь запроса на чтение (GET /reader/news/all)

1. Undertow принимает запрос, JAX-RS по `@Path` находит метод `ReaderAPI.getAllNews()`.
2. `ReaderAPI` вызывает `FacadeController.getInstance().getAllNews()`.
3. Фасад вызывает `newsController.findVisible()` (за интерфейсом `NewsInterface` может
   стоять `PositiveNewsDecorator`, отфильтровывающий негативные новости).
4. `NewsController` читает свой кэш `InMemoryStore<NewsBO>`, заполненный при старте
   из `NewsRepository`, и отбирает только опубликованные.
5. Фасад преобразует `List<NewsBO>` в `List<NewsTitleDTO>` и сортирует по дате.
6. `ReaderAPI` заворачивает список в `NewsListDTO`, Jackson сериализует в JSON → `200`.

### Путь запроса на запись (POST /creator/news)

1. JAX-RS находит `CreatorAPI.createNews()`, Jackson десериализует тело в
   `CreateNewsRequestDTO`.
2. Фасад находит категорию, собирает `NewsData` и вызывает `newsController.create(data)`.
3. `NewsController` создаёт `NewsBO`, связывает категории и теги, сохраняет через
   `NewsRepository` и кладёт в кэш.
4. Фасад публикует новость: `NewsBO.publish()` проверяет заголовок и ставит `PUBLISHED`.
5. `NewsRepository.save()` делит новость на два DO: `NewsDO` уходит в `SqlDao`
   (SQLite, `INSERT OR REPLACE`), `NewsContentDO` - в `NoSqlDao` (MongoDB).
6. Фасад преобразует `NewsBO` в `NewsItemDTO`, `CreatorAPI` отвечает `201` с JSON.

### Как слои общаются

* API ↔ Фасад - только DTO; фасад ↔ контроллер - BO и входные данные;
* контроллер ↔ репозиторий - BO (кэш и бизнес-правила в контроллере, хранение в репозитории);
* репозиторий ↔ DAO - DO (репозиторий делает маппинг `BO ↔ DO` и восстанавливает
  связанные объекты по идентификаторам);
* DAO ↔ СУБД - SQL через JDBC и документы через драйвер MongoDB (таблицы и
  документы строятся рефлексией по полям DO-класса).

---

## Структура проекта

```
news-app/
├── pom.xml
├── requests.http
└── src/main/
    ├── java/
    │   ├── model/                # IdSequence, InMemoryStore (кэш БО)
    │   │   ├── data/             # DO-классы: NewsDO, NewsContentDO, CategoryDO, TagDO, RoleDO, UserDO, CommentDO, FavoriteDO
    │   │   └── dao/              # IDao, SqlDao (SQLite), NoSqlDao (MongoDB), Database (подключение)
    │   ├── controller/           # FacadeController, контроллеры БО
    │   │   ├── repository/       # IRepository, NewsRepository, CategoryRepository, TagRepository, RoleRepository, UserRepository, CommentRepository, FavoriteRepository
    │   │   ├── news/             # NewsBO, NewsController, NewsInterface, NewsData, NewsStatus
    │   │   │   └── decor/        # NewsDecorator, PositiveNewsDecorator
    │   │   ├── person/           # PersonBO, GuestBO, UserBO, JournalistBO, AdminBO, UserController
    │   │   ├── category/         # CategoryBO, CategoryController
    │   │   ├── tag/              # TagBO, TagController
    │   │   ├── comment/          # CommentBO, CommentController
    │   │   ├── favorite/         # FavoriteBO, FavoriteController
    │   │   ├── role/             # RoleBO, RoleController
    │   │   └── interfaces/       # IAuthorizable, ICommentable, IPublishable
    │   └── view/
    │       ├── WebAppServer.java # запуск Undertow (localhost:8080)
    │       └── api/              # ReaderAPI, CreatorAPI, AdminAPI, StaticPageAPI, SingletonAPI, dto/
    └── resources/static/         # index.html (UI), tester.html (тестер API)
```

## Запуск

```bash
mvn clean package
java -jar target/news-app.jar
```

* Клиентский интерфейс: **http://localhost:8080/**
* Тестер API: **http://localhost:8080/static/tester.html**

### Хранилища данных (ЛР7)

* **SQLite** - файл `news.db` создаётся автоматически в рабочем каталоге при
  первом запуске и хранит роли, пользователей, категории, теги, новости
  (ядро), комментарии и избранное. Дополнительная настройка не нужна.
* **MongoDB** - используется для контента новостей (коллекция `news_contents`
  базы `newsdb`). Перед запуском нужно поднять сервер MongoDB на
  `localhost:27017`. Если сервер недоступен, контент временно хранится в
  памяти, и приложение продолжает работать.

## Тестовые учётные записи

| Роль | Логин | Пароль |
|------|-------|--------|
| Журналист | maria@example.com | pass |
| Пользователь | ivan@example.com | pass |
| Администратор | admin@example.com | admin |

## Тестирование через curl (Windows)

Сервер должен быть запущен. Используй `curl.exe` (в PowerShell голый `curl` - алиас
на `Invoke-WebRequest`). Кириллицу подставляй в URL-кодировке (метро = `%D0%BC%D0%B5%D1%82%D1%80%D0%BE`).

```powershell
# GET - можно открыть в браузере
curl.exe "http://localhost:8080/reader/news/all"
curl.exe "http://localhost:8080/reader/news/1"
curl.exe "http://localhost:8080/reader/news/last_week"
curl.exe "http://localhost:8080/reader/news/search?keywords=%D0%BC%D0%B5%D1%82%D1%80%D0%BE"
curl.exe "http://localhost:8080/reader/categories"
curl.exe "http://localhost:8080/admin/users"

# Регистрация и вход
curl.exe -X POST "http://localhost:8080/reader/auth/register?name=Petr&email=petr@example.com&password=pass"
curl.exe -X POST "http://localhost:8080/reader/auth/login?login=maria@example.com&password=pass"

# Комментарии
curl.exe -X POST "http://localhost:8080/reader/comments" -H "Content-Type: application/json" -d '{"newsId":1,"author":"Ivan","text":"Nice!"}'
curl.exe -X PUT  "http://localhost:8080/reader/comments/1" -H "Content-Type: application/json" -d '{"text":"Fixed"}'
curl.exe -X DELETE "http://localhost:8080/reader/comments/1"

# Новости (создатель)
curl.exe -X POST   "http://localhost:8080/creator/news" -H "Content-Type: application/json" -d '{"title":"Test","text":"Body","categoryId":1}'
curl.exe -X POST   "http://localhost:8080/creator/news/1/unpublish"
curl.exe -X PUT    "http://localhost:8080/creator/news/1" -H "Content-Type: application/json" -d '{"title":"New title"}'
curl.exe -X DELETE "http://localhost:8080/creator/news/1"

# ЛР5: заголовок опубликованной новости изменить нельзя (ответ 409)
curl.exe -X PUT "http://localhost:8080/creator/news/2" -H "Content-Type: application/json" -d '{"title":"Another title"}'

# Администратор
curl.exe -X POST "http://localhost:8080/admin/users/2/block"
curl.exe -X PUT  "http://localhost:8080/admin/users/2/role?role=ADMIN"
```

> В **cmd** одинарные кавычки для JSON не работают - используй двойные с
> экранированием: `curl.exe -X POST ... -d "{\"title\":\"Test\",\"categoryId\":1}"`.
