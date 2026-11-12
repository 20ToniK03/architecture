package controller.person;

/**
 * Бизнес-объект «Гость» (UML: {@code Guest}).
 *
 * <p>Неавторизованный посетитель: может просматривать и искать новости.
 * Регистрацией нового пользователя управляет {@code UserController},
 * поэтому бизнес-объект гостя не создаёт других бизнес-объектов.</p>
 */
public class GuestBO extends PersonBO {

    public GuestBO() {
        super(null, "Гость", null);
    }
}
