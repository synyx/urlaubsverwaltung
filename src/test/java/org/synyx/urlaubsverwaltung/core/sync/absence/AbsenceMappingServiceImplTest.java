package org.synyx.urlaubsverwaltung.core.sync.absence;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;


/**
 * Daniel Hammann - <hammann@synyx.de>.
 */
public class AbsenceMappingServiceImplTest {

    private AbsenceMappingService sut;
    private AbsenceMappingDAO absenceMappingDAO;

    @Before
    public void setUp() throws Exception {

        absenceMappingDAO = Mockito.mock(AbsenceMappingDAO.class);
        sut = new AbsenceMappingServiceImpl(absenceMappingDAO);
    }


    @Test
    public void shouldCreateAbsenceMappingForVacation() throws Exception {

        String eventId = "eventId";

        AbsenceMapping result = sut.create(42, AbsenceType.VACATION, eventId);

        assertThat(result.getAbsenceId(), is(42));
        assertThat(result.getAbsenceType(), is(AbsenceType.VACATION));
        assertThat(result.getEventId(), is(eventId));
        verify(absenceMappingDAO).save(result);
    }


    @Test
    public void shouldCreateAbsenceMappingForSickDay() throws Exception {

        String eventId = "eventId";

        AbsenceMapping result = sut.create(21, AbsenceType.SICKNOTE, eventId);

        assertThat(result.getAbsenceId(), is(21));
        assertThat(result.getAbsenceType(), is(AbsenceType.SICKNOTE));
        assertThat(result.getEventId(), is(eventId));
        verify(absenceMappingDAO).save(result);
    }


    @Test
    public void shouldCallAbsenceMappingDaoDelete() {

        AbsenceMapping absenceMapping = new AbsenceMapping(42, AbsenceType.VACATION, "dummyEvent");
        sut.delete(absenceMapping);

        verify(absenceMappingDAO).delete(absenceMapping);
    }


    @Test
    public void shouldCallAbsenceMappingDaoFind() {

        sut.getAbsenceByIdAndType(21, AbsenceType.SICKNOTE);

        verify(absenceMappingDAO).findAbsenceMappingByAbsenceIdAndAbsenceType(21, AbsenceType.SICKNOTE);
    }
}
