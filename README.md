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
6. `FacadeController` связывает контроллеры между собой и преобразует БО в DTO;
   декоратор `PositiveNewsDecorator` по-прежнему оборачивает `NewsController`.

**Проверка:** созданная новость сразу публикуется. Если попробовать изменить её
заголовок через `PUT /creator/news/{id}`, сервер вернёт `409` с сообщением
«Нельзя менять заголовок опубликованной новости». После `POST /creator/news/{id}/unpublish`
заголовок снова можно менять.

---

## Полный API (по итогам ЛР1-5)

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

## Структура проекта

```
news-app/
├── pom.xml
├── requests.http
└── src/main/
    ├── java/
    │   ├── model/                # InMemoryStore (кэш БО), IdSequence
    │   ├── controller/           # FacadeController, SeedData, контроллеры БО
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
