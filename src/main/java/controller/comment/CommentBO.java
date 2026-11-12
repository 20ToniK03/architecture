package controller.comment;

import controller.person.UserBO;

import java.util.Date;

/**
 * Бизнес-объект «Комментарий» (UML: {@code Comment}).
 *
 * <p>Хранит ссылку на новость ({@code newsId}) и автора ({@code User 1 -- 0..* Comment}).</p>
 */
public class CommentBO {

    private Long id;
    private Long newsId;
    private String text;
    private Date createdAt;
    private UserBO author;
    private boolean approved;

    public CommentBO(Long id, Long newsId, String text, Date createdAt, UserBO author) {
        this.id = id;
        this.newsId = newsId;
        this.text = text;
        this.createdAt = createdAt;
        this.author = author;
        this.approved = true;
    }

    /** Редактировать текст комментария (метод {@code edit} из UML). */
    public void edit(String text) {
        this.text = text;
    }

    /** Одобрить комментарий при модерации. */
    public void approve() {
        this.approved = true;
    }

    /** Отклонить комментарий при модерации. */
    public void reject() {
        this.approved = false;
    }

    public Long getId() {
        return id;
    }

    public Long getNewsId() {
        return newsId;
    }

    public String getText() {
        return text;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public UserBO getAuthor() {
        return author;
    }

    public boolean isApproved() {
        return approved;
    }
}
