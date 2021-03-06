package com.epam.jwd.validator;

import com.epam.jwd.entity.Faculty;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Validator class that checks right of {@link Faculty} faculty.
 *
 * @author Maxim Semenko
 * @version 0.0.1
 */

public class FacultyValidator implements Validator<Faculty> {

    private static FacultyValidator instance;
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final AtomicBoolean INSTANCE_CREATED = new AtomicBoolean(false);


    public static FacultyValidator getInstance() {
        if (!INSTANCE_CREATED.get()) {
            LOCK.lock();
            try {
                if (instance == null) {
                    instance = new FacultyValidator();
                    INSTANCE_CREATED.set(true);
                }
            } finally {
                LOCK.unlock();
            }
        }
        return instance;
    }

    /**
     * Method checks, if {@link Faculty} has valid count of places.
     *
     * @param faculty {@link Faculty} faculty object
     * @return {@link Boolean} true/false
     */
    @Override
    public Boolean validate(final Faculty faculty) {
        return CountPlacesValidator.getInstance().validate(faculty.getCountPlaces());
    }
}
