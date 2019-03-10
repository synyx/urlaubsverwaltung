package org.synyx.urlaubsverwaltung.core.application.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.synyx.urlaubsverwaltung.core.application.dao.ApplicationCommentDAO;
import org.synyx.urlaubsverwaltung.core.application.domain.Application;
import org.synyx.urlaubsverwaltung.core.application.domain.ApplicationAction;
import org.synyx.urlaubsverwaltung.core.application.domain.ApplicationComment;
import org.synyx.urlaubsverwaltung.core.application.domain.VacationCategory;
import org.synyx.urlaubsverwaltung.core.person.Person;
import org.synyx.urlaubsverwaltung.test.TestDataCreator;

import java.util.Optional;


/**
 * Unit test for {@link org.synyx.urlaubsverwaltung.core.application.service.ApplicationCommentServiceImpl}.
 *
 * @author  Aljona Murygina - murygina@synyx.de
 */
public class ApplicationCommentServiceImplTest {

    private ApplicationCommentService commentService;

    private ApplicationCommentDAO commentDAO;

    @Before
    public void setUp() {

        commentDAO = Mockito.mock(ApplicationCommentDAO.class);
        commentService = new ApplicationCommentServiceImpl(commentDAO);
    }


    @Test
    public void ensureCreatesACommentAndPersistsIt() {

        Person author = TestDataCreator.createPerson("author");
        Application application = TestDataCreator.createApplication(TestDataCreator.createPerson("person"),
                TestDataCreator.createVacationType(VacationCategory.HOLIDAY));

        ApplicationComment comment = commentService.create(application, ApplicationAction.ALLOWED,
                Optional.<String>empty(), author);

        Assert.assertNotNull("Should not be null", comment);

        Assert.assertNotNull("Date should be set", comment.getDate());
        Assert.assertNotNull("Action should be set", comment.getAction());
        Assert.assertNotNull("Author should be set", comment.getPerson());
        Assert.assertNotNull("Application for leave should be set", comment.getApplication());

        Assert.assertEquals("Wrong action", ApplicationAction.ALLOWED, comment.getAction());
        Assert.assertEquals("Wrong author", author, comment.getPerson());

        Assert.assertNull("Text should not be set", comment.getText());

        Mockito.verify(commentDAO).save(Mockito.eq(comment));
    }


    @Test
    public void ensureCreationOfCommentWithTextWorks() {

        Person author = TestDataCreator.createPerson("author");
        Application application = TestDataCreator.createApplication(TestDataCreator.createPerson("person"),
                TestDataCreator.createVacationType(VacationCategory.HOLIDAY));

        ApplicationComment comment = commentService.create(application, ApplicationAction.REJECTED, Optional.of("Foo"),
                author);

        Assert.assertNotNull("Should not be null", comment);

        Assert.assertNotNull("Date should be set", comment.getDate());
        Assert.assertNotNull("Action should be set", comment.getAction());
        Assert.assertNotNull("Author should be set", comment.getPerson());
        Assert.assertNotNull("Text should be set", comment.getText());

        Assert.assertEquals("Wrong action", ApplicationAction.REJECTED, comment.getAction());
        Assert.assertEquals("Wrong author", author, comment.getPerson());
        Assert.assertEquals("Wrong text", "Foo", comment.getText());

        Mockito.verify(commentDAO).save(Mockito.eq(comment));
    }
}
