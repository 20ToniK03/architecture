package controller.favorite;

import java.util.Date;

/**
 * Бизнес-объект «Избранное» (UML: {@code Favorite} - {@code id}, {@code addedAt}).
 *
 * <p>Связывает пользователя с сохранённой новостью. Ссылки хранятся
 * идентификаторами: сам пользователь и сама новость живут в своих
 * контроллерах, здесь только факт связи.</p>
 */
public class FavoriteBO {

    private Long id;
    private Date addedAt;
    private Long userId;
    private Long newsId;

    public FavoriteBO() {
    }

    public FavoriteBO(Long id, Date addedAt, Long userId, Long newsId) {
        this.id = id;
        this.addedAt = addedAt;
        this.userId = userId;
        this.newsId = newsId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Date addedAt) {
        this.addedAt = addedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getNewsId() {
        return newsId;
    }

    public void setNewsId(Long newsId) {
        this.newsId = newsId;
    }
}
