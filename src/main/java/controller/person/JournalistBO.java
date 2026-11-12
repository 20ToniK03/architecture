package controller.person;

/**
 * Бизнес-объект «Журналист» (UML: {@code Journalist}) - создатель контента.
 *
 * <p>Связан с новостями ({@code Journalist 1 -- 0..* News}). Существованием
 * новостей управляет {@code NewsController}: сам бизнес-объект не создаёт
 * и не удаляет их, а хранит только свои поля, включая рейтинг автора.</p>
 */
public class JournalistBO extends UserBO {

    private double rating;

    public JournalistBO(Long id, String name, String email) {
        super(id, name, email);
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }
}
