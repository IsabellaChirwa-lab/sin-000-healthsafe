package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertLevelStoreTest {

    @Test
    void startsAtZero() {
        assertEquals(0, new AlertLevelStore().get());
    }

    @Test
    void acceptsBoundaryLevels() {
        AlertLevelStore store = new AlertLevelStore();
        store.set(8);
        assertEquals(8, store.get());
        store.set(0);
        assertEquals(0, store.get());
    }

    @Test
    void rejectsOutOfRangeAndKeepsPreviousLevel() {
        AlertLevelStore store = new AlertLevelStore();
        store.set(3);
        assertThrows(IllegalArgumentException.class, () -> store.set(9));
        assertThrows(IllegalArgumentException.class, () -> store.set(-1));
        assertEquals(3, store.get());
    }

    @Test
    void rejectsMissingLevel() {
        assertThrows(IllegalArgumentException.class, () -> new AlertLevelStore().set(null));
    }
}