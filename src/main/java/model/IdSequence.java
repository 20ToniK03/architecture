package model;

/**
 * Генератор идентификаторов. Относится к модулю модели и не зависит
 * от бизнес-логики.
 */
public class IdSequence {

    private long next;

    public IdSequence() {
        this(1L);
    }

    public IdSequence(long start) {
        this.next = start;
    }

    /** Возвращает следующий свободный идентификатор. */
    public synchronized long next() {
        return next++;
    }

    /** Возвращает значение, которое будет выдано следующим (без расхода). */
    public synchronized long peek() {
        return next;
    }

    /**
     * Поднять счётчик до указанного значения, если оно больше текущего.
     * Используется после загрузки тестовых данных из репозитория: новые
     * идентификаторы не должны совпадать с уже сохранёнными.
     */
    public synchronized void ensureAtLeast(long value) {
        if (value > next) {
            next = value;
        }
    }
}
